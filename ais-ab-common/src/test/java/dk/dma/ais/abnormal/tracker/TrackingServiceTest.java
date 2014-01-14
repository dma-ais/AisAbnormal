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

package dk.dma.ais.abnormal.tracker;

import com.google.common.eventbus.Subscribe;
import dk.dma.ais.abnormal.application.statistics.AppStatisticsService;
import dk.dma.ais.abnormal.application.statistics.AppStatisticsServiceImpl;
import dk.dma.ais.abnormal.tracker.events.CellIdChangedEvent;
import dk.dma.ais.abnormal.tracker.events.PositionChangedEvent;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage3;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.AisPosition;
import dk.dma.ais.message.IPositionMessage;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.model.geometry.grid.Cell;
import dk.dma.enav.model.geometry.grid.Grid;
import org.junit.Test;

import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TrackingServiceTest {

    final Grid grid = Grid.createSize(100);
    final AppStatisticsService statisticsService = new AppStatisticsServiceImpl();

    /**
     * Test that grid cell change events are emitted by the tracker when a simulated track is moving
     * north under the Great Belt bridge.
     *
     * Assumes grid size 100m.
     * Track starts in cell id 24686212289 (55°20'13.7"N,11°02'21.8"E) - (55°20'10.5"N,11°02'25.1"E)
     */
    @Test
    public void testGridChangeEventsEmitted() {
        // Starting position in the center of cell 24686212289
        Position startingPosition = Position.create((55.33714285714286 + 55.33624454148472) / 2, (11.039401122894573 + 11.040299438552713) / 2);
        System.out.println("Starting position: " + startingPosition);
        Cell startingCell = grid.getCell(startingPosition);
        assertEquals(24686212289L, startingCell.getCellId());
        dk.dma.ais.message.AisPosition aisStartingPosition = new AisPosition(startingPosition);

        // Create initial static and voyage data message
        Queue<AisMessage> messageQueue = new LinkedList<>();
        AisMessage5 message5 = createAisMessage5();
        messageQueue.add(message5);

        // Create series of position reports for passing under the bridge (north-going)
        AisMessage3 firstPositionMessage = createAisMessage3(aisStartingPosition);
        messageQueue.add(firstPositionMessage);

        Position prevGeoLocation = firstPositionMessage.getPos().getGeoLocation();
        final double step = grid.getResolution();
        for (int i = 0; i < 10; i++) {
            AisMessage3 positionMessage = cloneAisMessage3(firstPositionMessage);
            Position nextGeoLocation = Position.create(prevGeoLocation.getLatitude() + step, prevGeoLocation.getLongitude());
            AisPosition nextPosition = new AisPosition(nextGeoLocation);
            positionMessage.setPos(nextPosition);
            System.out.println("Next position: " + nextGeoLocation);
            messageQueue.add(positionMessage);
            prevGeoLocation = positionMessage.getPos().getGeoLocation();
        }
        final int expectedNumberOfCellChangeEvents = messageQueue.size() - 1 /* minus the static msg */;

        // Create object under test
        final TrackingService tracker = new TrackingServiceImpl(grid, statisticsService);

        // Wire up test subscriber
        // (discussion: https://code.google.com/p/guava-libraries/issues/detail?id=875)
        TestSubscriber testSubscriber = new TestSubscriber();
        tracker.registerSubscriber(testSubscriber);

        // Set up our expectations
        final long[] expectedSequenceOfCells =
        {
                24686212289L,
                24686613039L,
                24687013789L,
                24687414539L,
                24687815289L,
                24688216039L,
                24688616789L,
                24689017539L,
                24689418289L,
                24689819039L,
                24690219789L,
                24690620539L
        };
        int nextExpectedCellId = 0;

        // Play scenario through tracker
        long firstTimestamp = System.currentTimeMillis();
        int timeStep = 0;

        // Run test scenario and assert results
        assertEquals(Integer.valueOf(0), tracker.getNumberOfTracks());

        while (!messageQueue.isEmpty()) {
            AisMessage message = messageQueue.remove();
            Date messageTimestamp = new Date(firstTimestamp + (timeStep++ * 10000)); // 10 secs between msgs
            System.out.println(messageTimestamp + ": " + message);
            tracker.update(messageTimestamp, message);
            if (message instanceof IPositionMessage) {
                assertEquals(expectedSequenceOfCells[nextExpectedCellId++], testSubscriber.getCurrentCellId());
            }
        }

        assertEquals(expectedNumberOfCellChangeEvents, testSubscriber.getNumberOfCellIdChangedEventsReceived());
        assertEquals(Integer.valueOf(1), tracker.getNumberOfTracks());
    }

    /**
     * Test that grid cell change events are not emitted by the tracker when a simulated track is moving
     * inside the same cell.
     *
     * Assumes grid size 100m.
     * Track starts in cell id 24686212289 (55°20'13.7"N,11°02'21.8"E) - (55°20'10.5"N,11°02'25.1"E)
     */
    @Test
    public void testGridChangeEventsNotEmittedForMovementsInsideSameCell() {
        // Starting position in the center of cell 24686212289
        Position startingPosition = Position.create((55.33714285714286 + 55.33624454148472) / 2, (11.039401122894573 + 11.040299438552713) / 2);
        System.out.println("Starting position: " + startingPosition);
        Cell startingCell = grid.getCell(startingPosition);
        assertEquals(24686212289L, startingCell.getCellId());
        dk.dma.ais.message.AisPosition aisStartingPosition = new AisPosition(startingPosition);

        // Create initial static and voyage data message
        Queue<AisMessage> messageQueue = new LinkedList<>();
        AisMessage5 message5 = createAisMessage5();
        messageQueue.add(message5);

        // Create series of position reports for passing under the bridge (north-going)
        AisMessage3 firstPositionMessage = createAisMessage3(aisStartingPosition);
        messageQueue.add(firstPositionMessage);

        Position prevGeoLocation = firstPositionMessage.getPos().getGeoLocation();
        final double step = grid.getResolution() / 25;
        for (int i = 0; i < 10; i++) {
            AisMessage3 positionMessage = cloneAisMessage3(firstPositionMessage);
            Position nextGeoLocation = Position.create(prevGeoLocation.getLatitude() + step, prevGeoLocation.getLongitude());
            AisPosition nextPosition = new AisPosition(nextGeoLocation);
            positionMessage.setPos(nextPosition);
            System.out.println("Next position: " + nextGeoLocation);
            messageQueue.add(positionMessage);
            prevGeoLocation = positionMessage.getPos().getGeoLocation();
        }

        // Create object under test
        final TrackingService tracker = new TrackingServiceImpl(grid, statisticsService);

        // Wire up test subscriber
        // (discussion: https://code.google.com/p/guava-libraries/issues/detail?id=875)
        TestSubscriber testSubscriber = new TestSubscriber();
        tracker.registerSubscriber(testSubscriber);

        // Play scenario through tracker
        long firstTimestamp = System.currentTimeMillis();
        int timeStep = 0;

        // Run test scenario and assert results
        assertEquals(Integer.valueOf(0), tracker.getNumberOfTracks());

        while (!messageQueue.isEmpty()) {
            AisMessage message = messageQueue.remove();
            Date messageTimestamp = new Date(firstTimestamp + (timeStep++ * 10000)); // 10 secs between msgs
            System.out.println(messageTimestamp + ": " + message);
            tracker.update(messageTimestamp, message);
            if (message instanceof IPositionMessage) {
                assertEquals(startingCell.getCellId(), testSubscriber.getCurrentCellId());
            }
        }

        assertEquals(1, testSubscriber.getNumberOfCellIdChangedEventsReceived());
        assertEquals(Integer.valueOf(1), tracker.getNumberOfTracks());
    }

    /**
     *  Test that Track.setProperty(Track.TIMESTAMP_ANY_UPDATE, ...) is called on every update
     */
    @Test
    public void testTrackTimestampIsUpdatedOnUpdates() {
        // Create object under test
        final TrackingServiceImpl tracker = new TrackingServiceImpl(grid, statisticsService);

        // Prepare test data
        Position startingPosition = Position.create((55.33714285714286 + 55.33624454148472) / 2, (11.039401122894573 + 11.040299438552713) / 2);
        dk.dma.ais.message.AisPosition aisStartingPosition = new AisPosition(startingPosition);
        AisMessage3 firstPositionMessage = createAisMessage3(aisStartingPosition);

        Date firstTimestamp = new Date(System.currentTimeMillis());

        // Execute test
        tracker.update(firstTimestamp, firstPositionMessage);

        // Assert results
        Set<Integer> keys = tracker.tracks.keySet();
        assertNotNull(keys);
        assertEquals(1, keys.size());
        Integer key = keys.iterator().next();
        assertNotNull(key);
        Object trackTimestamp = tracker.tracks.get(key).getProperty(Track.TIMESTAMP_ANY_UPDATE);
        assertTrue(trackTimestamp instanceof Long);
        assertEquals(trackTimestamp, firstTimestamp.getTime());

        // Update track with newer timestamp - then test again
        Date secondTimestamp = new Date(firstTimestamp.getTime() + 600);
        tracker.update(secondTimestamp, firstPositionMessage);

        // Assert results
        keys = tracker.tracks.keySet();
        assertNotNull(keys);
        assertEquals(1, keys.size());
        key = keys.iterator().next();
        assertNotNull(key);
        trackTimestamp = tracker.tracks.get(key).getProperty(Track.TIMESTAMP_ANY_UPDATE);
        assertTrue(trackTimestamp instanceof Long);
        assertEquals(trackTimestamp, secondTimestamp.getTime());
    }

    @Test
    public void testLinearInterpolation() {
       assertEquals(1, TrackingServiceImpl.linearInterpolation(1, 1, 3, 3, 1), 1e-16);
       assertEquals(3, TrackingServiceImpl.linearInterpolation(1, 1, 3, 3, 3), 1e-16);

       assertEquals(2, TrackingServiceImpl.linearInterpolation(1, 1, 3, 3, 2), 1e-16);
       assertEquals(10, TrackingServiceImpl.linearInterpolation(1, 1, 3, 3, 10), 1e-16);

       assertEquals(2.5, TrackingServiceImpl.linearInterpolation(0, 0, 5, 10, 5), 1e-16);
       assertEquals(4.5, TrackingServiceImpl.linearInterpolation(0, 0, 5, 10, 9), 1e-16);
    }

    @Test
    public void testCalculateInterpolatedPositions() {
        Date now = new Date(System.currentTimeMillis());

        Position p1 = Position.create(55, 10);
        long t1 = now.getTime();

        Position p2 = Position.create(60, 15);
        long t2 = t1 + 50*1000;

        Map<Long,Position> interpolatedPositions = TrackingServiceImpl.calculateInterpolatedPositions(p1, t1, p2, t2);

        assertEquals(5, interpolatedPositions.size());
        Set<Long> interpolationTimestamps = interpolatedPositions.keySet();

        // Assert timestamps
        assertEquals((Long) (t1 + (long) TrackingServiceImpl.INTERPOLATION_TIME_STEP_MILLIS), (Long) interpolationTimestamps.toArray()[0]);
        assertEquals((Long) (t1 + (long) TrackingServiceImpl.INTERPOLATION_TIME_STEP_MILLIS*2), (Long) interpolationTimestamps.toArray()[1]);
        assertEquals((Long) (t1 + (long) TrackingServiceImpl.INTERPOLATION_TIME_STEP_MILLIS*3), (Long) interpolationTimestamps.toArray()[2]);
        assertEquals((Long) (t1 + (long) TrackingServiceImpl.INTERPOLATION_TIME_STEP_MILLIS*4), (Long) interpolationTimestamps.toArray()[3]);
        assertEquals((Long) (t1 + (long) TrackingServiceImpl.INTERPOLATION_TIME_STEP_MILLIS * 5), (Long) interpolationTimestamps.toArray()[4]);

        // Assert positions
        assertEquals(Position.create(56, 11), interpolatedPositions.get(interpolationTimestamps.toArray()[0]));
        assertEquals(Position.create(57, 12), interpolatedPositions.get(interpolationTimestamps.toArray()[1]));
        assertEquals(Position.create(58, 13), interpolatedPositions.get(interpolationTimestamps.toArray()[2]));
        assertEquals(Position.create(59, 14), interpolatedPositions.get(interpolationTimestamps.toArray()[3]));
        assertEquals(p2, interpolatedPositions.get(interpolationTimestamps.toArray()[4]));
    }

    /**
     *  Test that interpolation is not used if less than 10 secs between messages
     */
    @Test
    public void testTrackIsNotInterpolated() {
        testInterpolation(TrackingServiceImpl.TRACK_INTERPOLATION_REQUIRED_SECS - 1, 2 /* initial + second */ );
    }

    /**
     *  Test that interpolation is used if more than 10 secs between messages
     */
    @Test
    public void testTrackIsInterpolated() {
        testInterpolation(TrackingServiceImpl.TRACK_INTERPOLATION_REQUIRED_SECS + 1, 5 /* initial + second + 3 interpolated */);
    }

    /**
     *  Test that interpolation is used between 2 position messages 50 seconds apart - even though there
     *  is a static message half way between
     */
    @Test
    public void testTrackIsInterpolatedEvenThoughStaticMessageIsBetweenToPositionUpdates() {
        // Create object under test
        final TrackingServiceImpl tracker = new TrackingServiceImpl(grid, statisticsService);

        // Wire up test subscriber
        // (discussion: https://code.google.com/p/guava-libraries/issues/detail?id=875)
        TestSubscriber testSubscriber = new TestSubscriber();
        tracker.registerSubscriber(testSubscriber);

        // Scenario
        AisPosition p1 = new AisPosition(Position.create(55, 10));
        AisPosition p3 = new AisPosition(Position.create(56, 11));

        long currentTimeMillis = System.currentTimeMillis();
        Date t1 = new Date(currentTimeMillis);
        Date t2 = new Date(currentTimeMillis + (TrackingServiceImpl.TRACK_INTERPOLATION_REQUIRED_SECS-1)*1*1000);
        Date t3 = new Date(currentTimeMillis + (TrackingServiceImpl.TRACK_INTERPOLATION_REQUIRED_SECS-1)*2*1000);

        AisMessage messageSeq1 = createAisMessage3(p1);
        AisMessage messageSeq2 = createAisMessage5();
        AisMessage messageSeq3 = createAisMessage3(p3);

        tracker.update(t1, messageSeq1);
        tracker.update(t2, messageSeq2);
        tracker.update(t3, messageSeq3);

        // Assert result
        int expectedNumberOfPositionChangeEvents = 2 /* p1, p3 */+ /* interpolated */(int) ((t3.getTime() - t1.getTime())/ TrackingServiceImpl.INTERPOLATION_TIME_STEP_MILLIS);
        assertEquals(expectedNumberOfPositionChangeEvents, testSubscriber.getNumberOfCellIdChangedEventsReceived());
        assertEquals(Position.create(56, 11), testSubscriber.getCurrentPosition());
    }

    @Test
    public void testTrackIsStale() {
        assertFalse(TrackingServiceImpl.isTrackStale(0, 0, TrackingServiceImpl.TRACK_STALE_SECS*1000 - 1));
        assertFalse(TrackingServiceImpl.isTrackStale(0, 0, TrackingServiceImpl.TRACK_STALE_SECS*1000 + 1));

        long now = new Date(System.currentTimeMillis()).getTime();

        assertFalse(TrackingServiceImpl.isTrackStale(now, now, now + TrackingServiceImpl.TRACK_STALE_SECS*1000 - 1));
        assertTrue(TrackingServiceImpl.isTrackStale(now, now, now + TrackingServiceImpl.TRACK_STALE_SECS*1000 + 1));

        assertFalse(TrackingServiceImpl.isTrackStale(1, now, now + TrackingServiceImpl.TRACK_STALE_SECS*1000 - 1));
        assertTrue(TrackingServiceImpl.isTrackStale(1, now, now + TrackingServiceImpl.TRACK_STALE_SECS*1000 + 1));

        assertFalse(TrackingServiceImpl.isTrackStale(now, 1, now + TrackingServiceImpl.TRACK_STALE_SECS*1000 - 1));
        assertTrue(TrackingServiceImpl.isTrackStale(now, 1, now + TrackingServiceImpl.TRACK_STALE_SECS*1000 + 1));
    }

    @Test
    public void testCanProcessStaleTracks() {
        // Create object under test
        final TrackingServiceImpl tracker = new TrackingServiceImpl(grid, statisticsService);

        // Prepare test data
        Position startingPosition = Position.create((55.33714285714286 + 55.33624454148472) / 2, (11.039401122894573 + 11.040299438552713) / 2);
        dk.dma.ais.message.AisPosition aisPosition = new AisPosition(startingPosition);
        AisMessage3 positionMessage = createAisMessage3(aisPosition);

        Date t1 = new Date(System.currentTimeMillis());
        Date t2 = new Date(t1.getTime() + TrackingServiceImpl.TRACK_STALE_SECS*1000 + 60000);

        // Execute test where track goes stale
        tracker.update(t1, positionMessage);
        tracker.update(t2, positionMessage);

        // No exceptions are expected
    }

    private void testInterpolation(int secsBetweenMessages, int expectedNumberOfPositionChangeEvents) {
        // Create object under test
        final TrackingServiceImpl tracker = new TrackingServiceImpl(grid,statisticsService);

        // Prepare test data
        Position startingPosition = Position.create((55.33714285714286 + 55.33624454148472) / 2, (11.039401122894573 + 11.040299438552713) / 2);
        dk.dma.ais.message.AisPosition aisStartingPosition = new AisPosition(startingPosition);
        AisMessage3 firstPositionMessage = createAisMessage3(aisStartingPosition);

        Position secondPosition = Position.create((55.33714285714286 + 55.35624454148472) / 2, (11.038401122894573 + 10.890299438552713) / 2);
        dk.dma.ais.message.AisPosition aisSecondPosition = new AisPosition(secondPosition);
        AisMessage3 secondPositionMessage = createAisMessage3(aisSecondPosition);

        Date firstTimestamp = new Date(System.currentTimeMillis());
        Date secondTimestamp = new Date(System.currentTimeMillis() + secsBetweenMessages*1000);

        System.out.println("Starting at " + firstTimestamp.getTime() + " in " + startingPosition);
        System.out.println("Ending at " + secondTimestamp.getTime() + " in " + secondPosition);

        // Wire up test subscriber
        // (discussion: https://code.google.com/p/guava-libraries/issues/detail?id=875)
        TestSubscriber testSubscriber = new TestSubscriber();
        tracker.registerSubscriber(testSubscriber);

        // Execute test
        tracker.update(firstTimestamp, firstPositionMessage);
        tracker.update(secondTimestamp, secondPositionMessage);

        // Assert result
        assertEquals(expectedNumberOfPositionChangeEvents, testSubscriber.getNumberOfCellIdChangedEventsReceived());

        assertEquals(secondPosition.getLatitude(), testSubscriber.getCurrentPosition().getLatitude(), 1e-6);
        assertEquals(secondPosition.getLongitude(), testSubscriber.getCurrentPosition().getLongitude(), 1e-6);
    }

    private static AisMessage3 cloneAisMessage3(AisMessage3 msgToClone) {
        AisMessage3 message = new AisMessage3();
        message.setCog(msgToClone.getCog());
        message.setNavStatus(msgToClone.getNavStatus());
        message.setRot(msgToClone.getRot());
        message.setSog(msgToClone.getSog());
        return message;
    }

    private static AisMessage3 createAisMessage3(AisPosition aisStartingPosition) {
        AisMessage3 firstPositionMessage = new AisMessage3();
        firstPositionMessage.setPos(aisStartingPosition);
        firstPositionMessage.setCog(1);
        firstPositionMessage.setNavStatus(0);
        firstPositionMessage.setRot(0);
        firstPositionMessage.setSog(10);
        return firstPositionMessage;
    }

    private static AisMessage5 createAisMessage5() {
        AisMessage5 message5 = new AisMessage5();
        message5.setDest("SKAGEN");
        message5.setCallsign("OY1234");
        message5.setImo(1234567);
        return message5;
    }

    public class TestSubscriber {

        private long currentCellId;
        private Position currentPosition;

        private int numberOfCellIdChangedEventsReceived;
        private int numberOfPositionChangedEventsReceived;

        @Subscribe
        public void onCellIdChanged(CellIdChangedEvent event) {
            numberOfCellIdChangedEventsReceived++;
            currentCellId = (long) event.getTrack().getProperty(Track.CELL_ID);
            System.out.println("We are now in cell: " + currentCellId);
            assertTrackTimestamps(event.getTrack());
        }

        @Subscribe
        public void onPositionChanged(PositionChangedEvent event) {
            numberOfPositionChangedEventsReceived++;
            currentPosition = (Position) event.getTrack().getProperty(Track.POSITION);
            System.out.println("We are now in position: " + currentPosition);
            assertTrackTimestamps(event.getTrack());
        }

        private void assertTrackTimestamps(Track track) {
            // TIMESTAMP_ANY_UPDATE is never allowed to fall behind TIMESTAMP_POSITION_UPDATE
            Long anyUpdate = getTimestampFromTrack(track, Track.TIMESTAMP_ANY_UPDATE);
            Long posUpdate = getTimestampFromTrack(track, Track.TIMESTAMP_POSITION_UPDATE);
            assertTrue(anyUpdate >= posUpdate);
        }

        private long getTimestampFromTrack(Track track, String timestampType) {
            Long timestamp = (Long) track.getProperty(timestampType);
            if (timestamp == null) {
                timestamp = 0L;
            }
            return timestamp;
        }

        public long getCurrentCellId() {
            return currentCellId;
        }

        public Position getCurrentPosition() {
            return currentPosition;
        }

        public int getNumberOfCellIdChangedEventsReceived() {
            return numberOfCellIdChangedEventsReceived;
        }

        public int getNumberOfPositionChangedEventsReceived() {
            return numberOfPositionChangedEventsReceived;
        }

    }

}
