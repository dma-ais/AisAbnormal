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

package dk.dma.ais.abnormal.analyzer.behaviour;

import com.google.common.eventbus.Subscribe;
import dk.dma.ais.abnormal.analyzer.behaviour.events.AbnormalEventLower;
import dk.dma.ais.abnormal.analyzer.behaviour.events.AbnormalEventMaintain;
import dk.dma.ais.abnormal.analyzer.behaviour.events.AbnormalEventRaise;
import dk.dma.ais.abnormal.application.statistics.AppStatisticsService;
import dk.dma.ais.abnormal.event.db.domain.CourseOverGroundEvent;
import dk.dma.ais.abnormal.tracker.EventEmittingTracker;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.Tracker;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.model.geometry.grid.Grid;
import org.apache.commons.configuration.BaseConfiguration;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BehaviourManagerImplTest {

    private JUnit4Mockery context;
    private Tracker trackingService;
    private BehaviourManagerImpl behaviourManager;
    private Track track;
    private EventBusSubscriber testSubscriber;

    @Before
    public void setUp() {
        context = new JUnit4Mockery();
        AppStatisticsService statisticsService = context.mock(AppStatisticsService.class);
        trackingService = new EventEmittingTracker(new BaseConfiguration(), Grid.createSize(200), statisticsService);
        behaviourManager = new BehaviourManagerImpl(trackingService);
        track = new Track(12345678);
        track.update(1234567890L, Position.create(56, 12), 45.0f, 10.1f, 45.0f);

        testSubscriber = new EventBusSubscriber();
        behaviourManager.registerSubscriber(testSubscriber);
    }

    @Test
    public void noCogEventRaisedForPurelyNormalBehaviour() {
        for (int i=0; i<1000; i++) {
            behaviourManager.normalBehaviourDetected(CourseOverGroundEvent.class, track);
            assertEquals(0, testSubscriber.numAbnormalEventRaise);
            assertEquals(0, testSubscriber.numAbnormalEventMaintain);
            assertEquals(0, testSubscriber.numAbnormalEventLower);
        }
    }

    @Test
    public void cogEventRaisedWhenThresholdReached() {
        assertEquals(0, testSubscriber.numAbnormalEventRaise);
        assertEquals(0, testSubscriber.numAbnormalEventMaintain);
        assertEquals(0, testSubscriber.numAbnormalEventLower);

        for (int i=0; i<BehaviourManagerImpl.RAISE_EVENT_SCORE_THRESHOLD - 1; i++) {
            behaviourManager.abnormalBehaviourDetected(CourseOverGroundEvent.class, track);
            assertEquals(0, testSubscriber.numAbnormalEventRaise);
            assertEquals(0, testSubscriber.numAbnormalEventMaintain);
            assertEquals(0, testSubscriber.numAbnormalEventLower);
        }

        behaviourManager.abnormalBehaviourDetected(CourseOverGroundEvent.class, track);
        assertEquals(1, testSubscriber.numAbnormalEventRaise);
        assertEquals(0, testSubscriber.numAbnormalEventMaintain);
        assertEquals(0, testSubscriber.numAbnormalEventLower);
    }

    @Test
    public void cogEventIsRaisedAndMaintained() {
        assertEquals(0, testSubscriber.numAbnormalEventRaise);
        assertEquals(0, testSubscriber.numAbnormalEventMaintain);
        assertEquals(0, testSubscriber.numAbnormalEventLower);

        for (int i=0; i<BehaviourManagerImpl.RAISE_EVENT_SCORE_THRESHOLD - 1; i++) {
            behaviourManager.abnormalBehaviourDetected(CourseOverGroundEvent.class, track);
            assertEquals(0, testSubscriber.numAbnormalEventRaise);
            assertEquals(0, testSubscriber.numAbnormalEventMaintain);
            assertEquals(0, testSubscriber.numAbnormalEventLower);
        }

        behaviourManager.abnormalBehaviourDetected(CourseOverGroundEvent.class, track);
        assertEquals(1, testSubscriber.numAbnormalEventRaise);
        assertEquals(0, testSubscriber.numAbnormalEventMaintain);
        assertEquals(0, testSubscriber.numAbnormalEventLower);

        for (int i=1; i<100; i++) {
            behaviourManager.abnormalBehaviourDetected(CourseOverGroundEvent.class, track);
            assertEquals(1, testSubscriber.numAbnormalEventRaise);
            assertEquals(i, testSubscriber.numAbnormalEventMaintain);
            assertEquals(0, testSubscriber.numAbnormalEventLower);
        }
    }

    @Test
    public void cogEventLoweredAfterNConsecutiveNormalBehaviours() {
        assertEquals(0, testSubscriber.numAbnormalEventRaise);
        assertEquals(0, testSubscriber.numAbnormalEventMaintain);
        assertEquals(0, testSubscriber.numAbnormalEventLower);

        for (int i=0; i<BehaviourManagerImpl.RAISE_EVENT_SCORE_THRESHOLD; i++) {
            behaviourManager.abnormalBehaviourDetected(CourseOverGroundEvent.class, track);
            assertEquals(i == BehaviourManagerImpl.RAISE_EVENT_SCORE_THRESHOLD - 1 ? 1:0, testSubscriber.numAbnormalEventRaise);
            assertEquals(0, testSubscriber.numAbnormalEventMaintain);
            assertEquals(0, testSubscriber.numAbnormalEventLower);
        }

        for (int i=1; i<100; i++) {
            behaviourManager.abnormalBehaviourDetected(CourseOverGroundEvent.class, track);
            assertEquals(1, testSubscriber.numAbnormalEventRaise);
            assertEquals(i, testSubscriber.numAbnormalEventMaintain);
            assertEquals(0, testSubscriber.numAbnormalEventLower);
        }

        int i;
        for (i=0; i<BehaviourManagerImpl.LOWER_EVENT_SCORE_THRESHOLD-1; i++) {
            behaviourManager.normalBehaviourDetected(CourseOverGroundEvent.class, track);
            assertEquals(1, testSubscriber.numAbnormalEventRaise);
            assertEquals(100+i, testSubscriber.numAbnormalEventMaintain);
            assertEquals(0, testSubscriber.numAbnormalEventLower);
        }

        behaviourManager.normalBehaviourDetected(CourseOverGroundEvent.class, track);
        assertEquals(1, testSubscriber.numAbnormalEventRaise);
        assertEquals(100+i-1, testSubscriber.numAbnormalEventMaintain);
        assertEquals(1, testSubscriber.numAbnormalEventLower);
    }

    @Test
    public void scoreIsResetWhenAbnormalEventReceived() {
        assertEquals(0, testSubscriber.numAbnormalEventRaise);
        assertEquals(0, testSubscriber.numAbnormalEventMaintain);
        assertEquals(0, testSubscriber.numAbnormalEventLower);

        // Raise event
        for (int i=0; i<BehaviourManagerImpl.RAISE_EVENT_SCORE_THRESHOLD; i++) {
            behaviourManager.abnormalBehaviourDetected(CourseOverGroundEvent.class, track);
            assertEquals(i == BehaviourManagerImpl.RAISE_EVENT_SCORE_THRESHOLD-1 ? 1:0, testSubscriber.numAbnormalEventRaise);
            assertEquals(0, testSubscriber.numAbnormalEventMaintain);
            assertEquals(0, testSubscriber.numAbnormalEventLower);
        }

        // A few normal behaviours observed (but not enough to lower event)
        behaviourManager.normalBehaviourDetected(CourseOverGroundEvent.class, track);
        assertEquals(1, testSubscriber.numAbnormalEventRaise);
        assertEquals(1, testSubscriber.numAbnormalEventMaintain);
        assertEquals(0, testSubscriber.numAbnormalEventLower);

        behaviourManager.normalBehaviourDetected(CourseOverGroundEvent.class, track);
        assertEquals(1, testSubscriber.numAbnormalEventRaise);
        assertEquals(2, testSubscriber.numAbnormalEventMaintain);
        assertEquals(0, testSubscriber.numAbnormalEventLower);

        // Then a new abnormal event is received
        behaviourManager.abnormalBehaviourDetected(CourseOverGroundEvent.class, track);
        assertEquals(1, testSubscriber.numAbnormalEventRaise);
        assertEquals(3, testSubscriber.numAbnormalEventMaintain);
        assertEquals(0, testSubscriber.numAbnormalEventLower);

        // Now we require the full no. of consective normal behaviours to lower event
        for (int i=0; i<BehaviourManagerImpl.LOWER_EVENT_SCORE_THRESHOLD-1; i++) {
            System.out.println("i:" + i);
            behaviourManager.normalBehaviourDetected(CourseOverGroundEvent.class, track);
            assertEquals(1, testSubscriber.numAbnormalEventRaise);
            assertEquals(4+i, testSubscriber.numAbnormalEventMaintain);
            assertEquals(0, testSubscriber.numAbnormalEventLower);
        }

        behaviourManager.normalBehaviourDetected(CourseOverGroundEvent.class, track);
        assertEquals(1, testSubscriber.numAbnormalEventRaise);
        assertEquals(4 + BehaviourManagerImpl.LOWER_EVENT_SCORE_THRESHOLD - 1 - 1, testSubscriber.numAbnormalEventMaintain);
        assertEquals(1, testSubscriber.numAbnormalEventLower);
    }

    @Test
    public void testEventCertainty() {
        EventCertainty eventCertainty;

        assertEquals(0, behaviourManager.trackingService.getNumberOfTracks());

        //
        behaviourManager.normalBehaviourDetected(CourseOverGroundEvent.class, track);
        eventCertainty = (EventCertainty) track.getNewestTrackingReport().getProperty(BehaviourManagerImpl.getEventCertaintyKey(CourseOverGroundEvent.class));
        System.out.println(eventCertainty);
        assertEquals(EventCertainty.LOWERED, eventCertainty);

        // Raise event
        for (int i=0; i<BehaviourManagerImpl.RAISE_EVENT_SCORE_THRESHOLD - 1; i++) {
            behaviourManager.abnormalBehaviourDetected(CourseOverGroundEvent.class, track);
            eventCertainty = (EventCertainty) track.getNewestTrackingReport().getProperty(BehaviourManagerImpl.getEventCertaintyKey(CourseOverGroundEvent.class));
            System.out.println(eventCertainty);
            assertEquals(EventCertainty.UNCERTAIN, eventCertainty);
        }

        behaviourManager.abnormalBehaviourDetected(CourseOverGroundEvent.class, track);
        eventCertainty = (EventCertainty) track.getNewestTrackingReport().getProperty(BehaviourManagerImpl.getEventCertaintyKey(CourseOverGroundEvent.class));
        System.out.println(eventCertainty);
        assertEquals(EventCertainty.RAISED, eventCertainty);

        // Raise again
        behaviourManager.abnormalBehaviourDetected(CourseOverGroundEvent.class, track);
        eventCertainty = (EventCertainty) track.getNewestTrackingReport().getProperty(BehaviourManagerImpl.getEventCertaintyKey(CourseOverGroundEvent.class));
        System.out.println(eventCertainty);
        assertEquals(EventCertainty.RAISED, eventCertainty);

        // Make us a bit uncertain
        behaviourManager.normalBehaviourDetected(CourseOverGroundEvent.class, track);
        eventCertainty = (EventCertainty) track.getNewestTrackingReport().getProperty(BehaviourManagerImpl.getEventCertaintyKey(CourseOverGroundEvent.class));
        System.out.println(eventCertainty);
        assertEquals(EventCertainty.UNCERTAIN, eventCertainty);

        // Reassure us
        behaviourManager.abnormalBehaviourDetected(CourseOverGroundEvent.class, track);
        eventCertainty = (EventCertainty) track.getNewestTrackingReport().getProperty(BehaviourManagerImpl.getEventCertaintyKey(CourseOverGroundEvent.class));
        System.out.println(eventCertainty);
        assertEquals(EventCertainty.RAISED, eventCertainty);

        // Now we require the full no. of consective normal behaviours to lower event
        for (int i=0; i<BehaviourManagerImpl.LOWER_EVENT_SCORE_THRESHOLD-1; i++) {
            behaviourManager.normalBehaviourDetected(CourseOverGroundEvent.class, track);
            eventCertainty = (EventCertainty) track.getNewestTrackingReport().getProperty(BehaviourManagerImpl.getEventCertaintyKey(CourseOverGroundEvent.class));
            System.out.println(eventCertainty);
            assertEquals(EventCertainty.UNCERTAIN, eventCertainty);
        }

        behaviourManager.normalBehaviourDetected(CourseOverGroundEvent.class, track);
        eventCertainty = (EventCertainty) track.getNewestTrackingReport().getProperty(BehaviourManagerImpl.getEventCertaintyKey(CourseOverGroundEvent.class));
        System.out.println(eventCertainty);
        assertEquals(EventCertainty.LOWERED, eventCertainty);
    }

    public final class EventBusSubscriber {
        int numAbnormalEventRaise = 0;
        int numAbnormalEventMaintain = 0;
        int numAbnormalEventLower = 0;
        EventCertainty eventCertainty;

        @Subscribe
        public void onAbnormalEventRaise(AbnormalEventRaise event) {
            numAbnormalEventRaise++;
            eventCertainty = event.getEventCertainty();
        }

        @Subscribe
        public void onAbnormalEventMaintain(AbnormalEventMaintain event) {
            numAbnormalEventMaintain++;
            eventCertainty = event.getEventCertainty();
        }

        @Subscribe
        public void onAbnormalEventLower(AbnormalEventLower event) {
            numAbnormalEventLower++;
            eventCertainty = event.getEventCertainty();
        }
    }

}
