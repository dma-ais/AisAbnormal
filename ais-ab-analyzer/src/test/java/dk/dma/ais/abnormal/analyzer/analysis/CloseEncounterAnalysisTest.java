/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dma.ais.abnormal.analyzer.analysis;

import dk.dma.ais.abnormal.analyzer.AppStatisticsService;
import dk.dma.ais.abnormal.analyzer.behaviour.EventCertainty;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.domain.Behaviour;
import dk.dma.ais.abnormal.event.db.domain.CloseEncounterEvent;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.test.helpers.ArgumentCaptor;
import dk.dma.ais.tracker.Tracker;
import dk.dma.ais.tracker.eventEmittingTracker.Track;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CloseEncounterAnalysisTest {

    JUnit4Mockery context;
    CloseEncounterAnalysis analysis;
    Tracker trackingService;
    AppStatisticsService statisticsService;
    EventRepository eventRepository;

    Track track, closeTrack, distantTrack, oldNearbyTrack, newNearbyTrack, distantNearbyTrack;
    Set<Track> tracks;

    int maxTimestampDeviationMillis = 60*1000;
    int maxDistanceDeviationMeters = 1852;

    // GatehouseSourceTag [baseMmsi=2190067, country=DK, region=, timestamp=Thu Apr 10 15:30:29 CEST 2014]
    // [msgId=5, repeat=0, userId=219000606, callsign=OWNM@@@, dest=BOEJDEN-FYNSHAV@@@@@, dimBow=12, dimPort=8, dimStarboard=4, dimStern=58, draught=30, dte=0, eta=67584, imo=8222824, name=FRIGG SYDFYEN@@@@@@@, posType=1, shipType=61, spare=0, version=0]
    final AisPacket vessel1StaticPacket = AisPacket.from(
        "$PGHP,1,2014,4,10,13,30,29,165,219,,2190067,1,28*22\r\n" +
        "!BSVDM,2,1,1,A,53@ng7P1uN6PuLpl000I8TLN1=T@ITDp0000000u1Pr844@P07PSiBQ1,0*7B\r\n" +
        "!BSVDM,2,2,1,A,CcAVCTj0EP00000,2*53");

    // GatehouseSourceTag [baseMmsi=2190074, country=DK, region=, timestamp=Tue Nov 12 13:04:58 CET 2013]
    // [msgId=5, repeat=0, userId=219002827, callsign=5QRJ   , dest=SKAGEN              , dimBow=2, dimPort=2, dimStarboard=3, dimStern=13, draught=28, dte=0, eta=576864, imo=0, name=MIE MALENE          , posType=1, shipType=30, spare=0, version=0]
    final AisPacket vessel2StaticPacket = AisPacket.from(
        "$PGHP,1,2013,11,12,12,4,58,279,219,,2190074,1,54*24\r\n" +
        "!BSVDM,2,1,0,B,53@nojh00003E58b220lTF0l4hDpF2222222220N0@=236<mP74jhAiC,0*32\r\n" +
        "!BSVDM,2,2,0,B,`88888888888880,2*66");

    @Before
    public void setUp() throws Exception {
        context = new JUnit4Mockery();
        trackingService = context.mock(Tracker.class);
        statisticsService = context.mock(AppStatisticsService.class);
        eventRepository = context.mock(EventRepository.class);
        analysis = new CloseEncounterAnalysis(new PropertiesConfiguration(), statisticsService, trackingService, eventRepository);

        long timestamp = System.currentTimeMillis();
        long tooOld = timestamp - maxTimestampDeviationMillis - 1;
        long old = timestamp - maxTimestampDeviationMillis + 1;
        long tooNew = timestamp + maxTimestampDeviationMillis + 1;
        long nyw = timestamp + maxTimestampDeviationMillis - 1;

        Position position = Position.create(56, 12);
        Position tooFarAway = Position.create(56.1, 12.1);
        assertTrue(position.distanceTo(tooFarAway, CoordinateSystem.CARTESIAN) > maxDistanceDeviationMeters);
        Position farAway = Position.create(56.014, 12.014);
        assertTrue(position.distanceTo(farAway, CoordinateSystem.CARTESIAN) < maxDistanceDeviationMeters);

        track = new Track(219000606);
        track.update(vessel1StaticPacket);
        track.update(timestamp, position, 90.0f, 10.0f, 90.0f);

        closeTrack = new Track(219002827);
        closeTrack.update(vessel2StaticPacket);
        closeTrack.update(timestamp - 40000, Position.create(56.1000, 12.0010), 180.0f, 10.0f, 180.0f);
        closeTrack.update(timestamp - 30000, Position.create(56.0800, 12.0010), 180.0f, 10.0f, 180.0f);
        closeTrack.update(timestamp - 20000, Position.create(56.0600, 12.0010), 180.0f, 10.0f, 180.0f);
        closeTrack.update(timestamp - 10000, Position.create(56.0400, 12.0010), 180.0f, 10.0f, 180.0f);
        closeTrack.update(timestamp,         Position.create(56.00001, 12.0000), 180.0f, 10.0f, 180.0f);
        closeTrack.getTrackingReports().forEach( tr -> { tr.setProperty("event-certainty-CloseEncounterEvent", EventCertainty.LOWERED);});
        assertTrue(closeTrack.getPosition().equals(Position.create(56.00001, 12.0000)));
        assertTrue(track.getPosition().distanceTo(closeTrack.getPosition(), CoordinateSystem.CARTESIAN) < 200);

        distantTrack = new Track(219000606);
        distantTrack.update(vessel1StaticPacket);
        distantTrack.update(timestamp, Position.create(57, 13), 90.0f, 10.0f, 90.0f);

        Track track1 = new Track(1);
        track1.update(tooOld, position, 90.0f, 10.0f, 90.0f);

        oldNearbyTrack = new Track(2);
        oldNearbyTrack.update(old, position, 90.0f, 10.0f, 90.0f);

        Track track3 = new Track(3);
        track3.update(tooNew, position, 90.0f, 10.0f, 90.0f);

        newNearbyTrack = new Track(3);
        newNearbyTrack.update(nyw, position, 90.0f, 10.0f, 90.0f);

        Track track4 = new Track(4);
        track4.update(timestamp, tooFarAway, 90.0f, 10.0f, 90.0f);

        distantNearbyTrack = new Track(5);
        distantNearbyTrack.update(timestamp, farAway, 90.0f, 10.0f, 90.0f);

        tracks = new HashSet<>();
        tracks.add(track);
        tracks.add(track1);
        tracks.add(oldNearbyTrack);
        tracks.add(track3);
        tracks.add(track4);
        tracks.add(distantNearbyTrack);
        tracks.add(newNearbyTrack);
    }

    @Test
    public void testFindNearByTracks() throws Exception {
        Set<Track> nearByTracks = analysis.findNearByTracks(tracks, track, maxTimestampDeviationMillis, maxDistanceDeviationMeters);

        assertEquals(3, nearByTracks.size());
        assertTrue(nearByTracks.contains(oldNearbyTrack));
        assertTrue(nearByTracks.contains(newNearbyTrack));
        assertTrue(nearByTracks.contains(distantNearbyTrack));
    }

    @Test
    public void testIsTrackPairAnalyzed() throws Exception {
        analysis.clearTrackPairsAnalyzed();
        assertFalse(analysis.isTrackPairAnalyzed(track, track));
        assertFalse(analysis.isTrackPairAnalyzed(track, distantNearbyTrack));
        assertFalse(analysis.isTrackPairAnalyzed(newNearbyTrack, distantNearbyTrack));
        assertFalse(analysis.isTrackPairAnalyzed(track, oldNearbyTrack));
        assertFalse(analysis.isTrackPairAnalyzed(track, newNearbyTrack));

        analysis.markTrackPairAnalyzed(track, distantNearbyTrack);
        assertFalse(analysis.isTrackPairAnalyzed(track, track));
        assertTrue(analysis.isTrackPairAnalyzed(track, distantNearbyTrack));
        assertFalse(analysis.isTrackPairAnalyzed(newNearbyTrack, distantNearbyTrack));
        assertFalse(analysis.isTrackPairAnalyzed(track, oldNearbyTrack));
        assertFalse(analysis.isTrackPairAnalyzed(track, newNearbyTrack));

        analysis.markTrackPairAnalyzed(newNearbyTrack, distantNearbyTrack);
        assertFalse(analysis.isTrackPairAnalyzed(track, track));
        assertTrue(analysis.isTrackPairAnalyzed(track, distantNearbyTrack));
        assertTrue(analysis.isTrackPairAnalyzed(newNearbyTrack, distantNearbyTrack));
        assertFalse(analysis.isTrackPairAnalyzed(track, oldNearbyTrack));
        assertFalse(analysis.isTrackPairAnalyzed(track, newNearbyTrack));
    }

    @Test
    public void closeEncounterCausesEventRaised() throws Exception {
        analysis.clearTrackPairsAnalyzed();
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(CloseEncounterAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(CloseEncounterAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            oneOf(eventRepository).findOngoingEventByVessel(track.getMmsi(), CloseEncounterEvent.class);
            oneOf(eventRepository).save(with(any(Event.class)));
        }});
        analysis.analyseCloseEncounter(track, closeTrack);
    }

    @Test
    public void noCloseEncounterCausesNoEventRaised() throws Exception {
        analysis.clearTrackPairsAnalyzed();
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(CloseEncounterAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(CloseEncounterAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            oneOf(eventRepository).findOngoingEventByVessel(track.getMmsi(), CloseEncounterEvent.class);
            never(eventRepository).save(with(any(Event.class)));
        }});
        analysis.analyseCloseEncounter(track, distantTrack);
    }

    @Test
    public void closeEncounterEventContainsTwoVesselBehaviours() throws Exception {
        analysis.clearTrackPairsAnalyzed();

        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(CloseEncounterAnalysis.class.getSimpleName()), with(any(String.class)));
            ignoring(statisticsService).setAnalysisStatistics(with(CloseEncounterAnalysis.class.getSimpleName()), with(any(String.class)), with(any(Long.class)));
            oneOf(eventRepository).findOngoingEventByVessel(track.getMmsi(), CloseEncounterEvent.class);
            oneOf(eventRepository).save(with(eventCaptor.getMatcher()));
        }});

        analysis.analyseCloseEncounter(track, closeTrack);

        Event event = eventCaptor.getCapturedObject();
        Set<Behaviour> behaviours = event.getBehaviours();
        assertEquals(2, behaviours.size());
        assertEquals(1, behaviours.stream().filter(t -> t.getVessel().getMmsi() == track.getMmsi()).count());
        assertEquals(1, behaviours.stream().filter(t -> t.getVessel().getMmsi() == closeTrack.getMmsi()).count());

        behaviours.forEach(b -> {
            if (b.getVessel().getMmsi() == track.getMmsi()) {
                assertEquals(1, b.getTrackingPoints().size()); // No. of previous tracking points in event
            } else if (b.getVessel().getMmsi() == closeTrack.getMmsi()) {
                assertEquals(5, b.getTrackingPoints().size()); // No. of previous tracking points in event
            }
        });
    }

}
