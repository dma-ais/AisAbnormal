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
import dk.dma.ais.abnormal.tracker.events.CellIdChangedEvent;
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
import java.util.Queue;

import static org.junit.Assert.assertEquals;

public class TrackingServiceTest {

    final Grid grid = Grid.createSize(100);

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
        AisMessage3 firstPositionMessage = getAisMessage3(aisStartingPosition);
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
        final TrackingService tracker = new TrackingServiceImpl(grid);

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

        assertEquals(expectedNumberOfCellChangeEvents, testSubscriber.getNumberOfEventsReceived());
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
        AisMessage3 firstPositionMessage = getAisMessage3(aisStartingPosition);
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
        final TrackingService tracker = new TrackingServiceImpl(grid);

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

        assertEquals(1, testSubscriber.getNumberOfEventsReceived());
        assertEquals(Integer.valueOf(1), tracker.getNumberOfTracks());
    }

    private static AisMessage3 cloneAisMessage3(AisMessage3 msgToClone) {
        AisMessage3 message = new AisMessage3();
        message.setCog(msgToClone.getCog());
        message.setNavStatus(msgToClone.getNavStatus());
        message.setRot(msgToClone.getRot());
        message.setSog(msgToClone.getSog());
        return message;
    }

    private static AisMessage3 getAisMessage3(AisPosition aisStartingPosition) {
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

    @Test
    public void testGridChangeEventsNotEmittedForMovementsInsideCell() {
    }

    public class TestSubscriber {
        private long currentCellId;
        private int numberOfEventsReceived;

        @Subscribe
        public void onCellIdChanged(CellIdChangedEvent event) {
            numberOfEventsReceived++;
            currentCellId = (long) event.getTrack().getProperty(Track.CELL_ID);
            System.out.println("We are now in cell: " + currentCellId);
        }

        public long getCurrentCellId() {
            return currentCellId;
        }

        public int getNumberOfEventsReceived() {
            return numberOfEventsReceived;
        }
    }

}
