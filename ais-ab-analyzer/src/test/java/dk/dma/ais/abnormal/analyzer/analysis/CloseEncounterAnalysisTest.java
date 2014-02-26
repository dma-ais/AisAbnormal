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
import dk.dma.ais.abnormal.analyzer.helpers.SafetyZone;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.domain.Behaviour;
import dk.dma.ais.abnormal.event.db.domain.CloseEncounterEvent;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingReport;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.test.helpers.ArgumentCaptor;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;
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
    TrackingService trackingService;
    AppStatisticsService statisticsService;
    EventRepository eventRepository;

    Track track, closeTrack, distantTrack, oldNearbyTrack, newNearbyTrack, distantNearbyTrack;
    Set<Track> tracks;

    int maxTimestampDeviationMillis = 60*1000;
    int maxDistanceDeviationMeters = 1852;

    @Before
    public void setUp() throws Exception {
        context = new JUnit4Mockery();
        trackingService = context.mock(TrackingService.class);
        statisticsService = context.mock(AppStatisticsService.class);
        eventRepository = context.mock(EventRepository.class);
        analysis = new CloseEncounterAnalysis( statisticsService, trackingService, eventRepository);

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

        track = new Track(0);
        track.setProperty(Track.VESSEL_LENGTH, 100);
        track.setProperty(Track.VESSEL_BEAM, 15);
        track.setProperty(Track.VESSEL_DIM_STERN, 30);
        track.setProperty(Track.VESSEL_DIM_STARBOARD, 7);
        track.updatePosition(TrackingReport.create(timestamp, position, 90.0f, 10.0f, false));

        closeTrack = new Track(100);
        closeTrack.setProperty(Track.VESSEL_LENGTH, 125);
        closeTrack.setProperty(Track.VESSEL_BEAM, 17);
        closeTrack.setProperty(Track.VESSEL_DIM_STERN, 18);
        closeTrack.setProperty(Track.VESSEL_DIM_STARBOARD, 9);
        closeTrack.updatePosition(TrackingReport.create(timestamp - 40000, Position.create(56.100, 12.001), 180.0f, 10.0f, false));
        closeTrack.updatePosition(TrackingReport.create(timestamp - 30000, Position.create(56.080, 12.001), 180.0f, 10.0f, false));
        closeTrack.updatePosition(TrackingReport.create(timestamp - 20000, Position.create(56.060, 12.001), 180.0f, 10.0f, false));
        closeTrack.updatePosition(TrackingReport.create(timestamp - 10000, Position.create(56.040, 12.001), 180.0f, 10.0f, false));
        closeTrack.updatePosition(TrackingReport.create(timestamp,         Position.create(56.001, 12.001), 180.0f, 10.0f, false));
        closeTrack.getTrackingReports().forEach( tr -> { tr.setProperty("event-certainty-CloseEncounterEvent", EventCertainty.LOWERED);});
        assertTrue(closeTrack.getPosition().equals(Position.create(56.001, 12.001)));
        assertTrue(track.getPosition().distanceTo(closeTrack.getPosition(), CoordinateSystem.CARTESIAN) < 200);

        distantTrack = new Track(101);
        distantTrack.setProperty(Track.VESSEL_LENGTH, 175);
        distantTrack.setProperty(Track.VESSEL_BEAM, 23);
        distantTrack.setProperty(Track.VESSEL_DIM_STERN, 54);
        distantTrack.setProperty(Track.VESSEL_DIM_STARBOARD, 4);
        distantTrack.updatePosition(TrackingReport.create(timestamp, Position.create(57, 13), 90.0f, 10.0f, false));

        Track track1 = new Track(1);
        track1.updatePosition(TrackingReport.create(tooOld, position, 90.0f, 10.0f, false));

        oldNearbyTrack = new Track(2);
        oldNearbyTrack.updatePosition(TrackingReport.create(old, position, 90.0f, 10.0f, false));

        Track track3 = new Track(3);
        track3.updatePosition(TrackingReport.create(tooNew, position, 90.0f, 10.0f, false));

        newNearbyTrack = new Track(3);
        newNearbyTrack.updatePosition(TrackingReport.create(nyw, position, 90.0f, 10.0f, false));

        Track track4 = new Track(4);
        track4.updatePosition(TrackingReport.create(timestamp, tooFarAway, 90.0f, 10.0f, false));

        distantNearbyTrack = new Track(5);
        distantNearbyTrack.updatePosition(TrackingReport.create(timestamp, farAway, 90.0f, 10.0f, false));

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
    public void safetyZoneXYAreZeroInCenterPoint() {
        track.setProperty(Track.VESSEL_LENGTH, 0);
        track.setProperty(Track.VESSEL_BEAM, 0);
        track.setProperty(Track.VESSEL_DIM_STERN, 0);
        track.setProperty(Track.VESSEL_DIM_STARBOARD, 0);
        track.updatePosition(TrackingReport.create(1000L, Position.create(56, 12), 90.0f, 0.0f, false));

        SafetyZone safetyZone = analysis.computeSafetyZone(track.getPosition(), track, track);

        assertEquals(0.0, safetyZone.getX(), 1e-6);
        assertEquals(0.0, safetyZone.getY(), 1e-6);
    }

    @Test
    public void safetyZoneXYAreTranslatedForwardX() {
        Track track = new Track(0);
        track.setProperty(Track.VESSEL_LENGTH, 100);
        track.setProperty(Track.VESSEL_BEAM, 20);
        track.setProperty(Track.VESSEL_DIM_STERN, 50);
        track.setProperty(Track.VESSEL_DIM_STARBOARD, 10);
        track.updatePosition(TrackingReport.create(1000L, Position.create(56, 12), 90.0f, 0.0f, false));

        SafetyZone safetyZone = analysis.computeSafetyZone(track.getPosition(), track, track);

        assertEquals(100.0, safetyZone.getX(), 1e-6);
        assertEquals(0.0, safetyZone.getY(), 1e-6);
    }

    @Test
    public void safetyZoneXYAreTranslatedForwardY() {
        Track track = new Track(0);
        track.setProperty(Track.VESSEL_LENGTH, 100);
        track.setProperty(Track.VESSEL_BEAM, 20);
        track.setProperty(Track.VESSEL_DIM_STERN, 50);
        track.setProperty(Track.VESSEL_DIM_STARBOARD, 10);
        track.updatePosition(TrackingReport.create(1000L, Position.create(56, 12), 0.0f, 0.0f, false));

        SafetyZone safetyZone = analysis.computeSafetyZone(track.getPosition(), track, track);

        assertEquals(0.0, safetyZone.getX(), 1e-6);
        assertEquals(100.0, safetyZone.getY(), 1e-6);
    }

    @Test
    public void safetyZoneAtSpeedZeroIsSameSizeAsShip() {
        Track track = new Track(0);
        track.setProperty(Track.VESSEL_LENGTH, 100);
        track.setProperty(Track.VESSEL_BEAM, 20);
        track.setProperty(Track.VESSEL_DIM_STERN, 50);
        track.setProperty(Track.VESSEL_DIM_STARBOARD, 10);
        track.updatePosition(TrackingReport.create(1000L, Position.create(56, 12), 90.0f, 0.0f, false));

        SafetyZone safetyZone = analysis.computeSafetyZone(track.getPosition(), track, track);

        /* Checking in cartesian coordinates */
        assertEquals(100.0, safetyZone.getX(),        1e-6);
        assertEquals(  0.0, safetyZone.getY(),        1e-6);
        assertEquals(200.0, safetyZone.getAlpha(),    1e-6);
        assertEquals( 50.0, safetyZone.getBeta(),     1e-6);
        assertEquals(  0.0, safetyZone.getThetaDeg(), 1e-6);
    }

    @Test
    public void safetyZoneXYAreBigForDistantPositions() {
        Track track = new Track(0);
        track.setProperty(Track.VESSEL_LENGTH, 100);
        track.setProperty(Track.VESSEL_BEAM, 20);
        track.setProperty(Track.VESSEL_DIM_STERN, 50);
        track.setProperty(Track.VESSEL_DIM_STARBOARD, 10);
        track.updatePosition(TrackingReport.create(1000L, Position.create(56, 12), 0.0f, 0.0f, false));

        SafetyZone safetyZone1 = analysis.computeSafetyZone(Position.create(57, 12), track, track);
        assertEquals(0.0, safetyZone1.getX(), 1e-6);
        assertEquals(-110849.07380140232, safetyZone1.getY(), 1e-6);

        SafetyZone safetyZone2 = analysis.computeSafetyZone(Position.create(56, 13), track, track);
        assertEquals(-62038.68737595969, safetyZone2.getX(), 1e-6);
        assertEquals(548.8437792803036, safetyZone2.getY(), 1e-6);
    }

    @Test
    public void testComputeSafetyZone1() {
        track.updatePosition(TrackingReport.create(1000L, Position.create(56, 12), 90.0f, 10.0f, false));

        SafetyZone safetyZone = analysis.computeSafetyZone(track.getPosition(), track, track);

        assertEquals(120.0, safetyZone.getX(), 1e-6);
        assertEquals(-0.5, safetyZone.getY(), 1e-6);
        assertEquals(37.5, safetyZone.getBeta(), 1e-6);
        assertEquals(200.0, safetyZone.getAlpha(), 1e-6);
        assertEquals(0.0, safetyZone.getThetaDeg(), 1e-6);
    }

    @Test
    public void testComputeSafetyZone2() {
        track.updatePosition(TrackingReport.create(1000L, Position.create(56, 12), 90.0f, 10.0f, false));
        track.setProperty(Track.VESSEL_LENGTH, 100);
        track.setProperty(Track.VESSEL_BEAM, 15);
        track.setProperty(Track.VESSEL_DIM_STERN, 30);
        track.setProperty(Track.VESSEL_DIM_STARBOARD, 7);

        Track otherTrack = track.clone();
        otherTrack.updatePosition(TrackingReport.create(1000L, Position.create(56, 12), 90.0f, 20.0f, false));
        otherTrack.setProperty(Track.VESSEL_LENGTH, 100);
        otherTrack.setProperty(Track.VESSEL_BEAM, 15);
        otherTrack.setProperty(Track.VESSEL_DIM_STERN, 30);
        otherTrack.setProperty(Track.VESSEL_DIM_STARBOARD, 7);

        SafetyZone safetyZone = analysis.computeSafetyZone(track.getPosition(), track, otherTrack);

        assertEquals(120.0, safetyZone.getX(), 1e-6);
        assertEquals(-0.5, safetyZone.getY(), 1e-6);
        assertEquals(37.5, safetyZone.getBeta(), 1e-6);
        assertEquals(200.0, safetyZone.getAlpha(), 1e-6);
        assertEquals(0.0, safetyZone.getThetaDeg(), 1e-6);
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
