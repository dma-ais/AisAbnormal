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
import dk.dma.ais.abnormal.analyzer.helpers.SafetyZone;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingReport;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;
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

    Track track, oldNearbyTrack, newNearbyTrack, distantNearbyTrack;
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
        track.updatePosition(TrackingReport.create(timestamp, position, 90.0f, 10.0f, false));

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
    public void testComputeSafetyZone1() {
        track.updatePosition(TrackingReport.create(1000L, Position.create(56, 12), 90.0f, 10.0f, false));
        track.setProperty(Track.VESSEL_LENGTH, 100);
        track.setProperty(Track.VESSEL_BEAM, 15);
        track.setProperty(Track.VESSEL_DIM_STERN, 30);
        track.setProperty(Track.VESSEL_DIM_STARBOARD, 7);

        SafetyZone safetyZone = analysis.computeSafetyZone(track, track);

        assertEquals(120.0, safetyZone.getX(), 1e-6);
        assertEquals(-0.5, safetyZone.getY(), 1e-6);
        assertEquals(37.5, safetyZone.getBeta(), 1e-6);
        assertEquals(200.0, safetyZone.getAlpha(), 1e-6);
        assertEquals(0.0, safetyZone.getThetaDeg(), 1e-6);
    }


    @Test
    public void testAnalyseCloseEncounter() throws Exception {
        analysis.clearTrackPairsAnalyzed();
//        analysis.analyseCloseEncounter(track, distantNearbyTrack);

    }
}
