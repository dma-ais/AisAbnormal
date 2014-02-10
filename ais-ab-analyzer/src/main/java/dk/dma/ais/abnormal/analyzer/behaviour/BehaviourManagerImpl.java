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

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import dk.dma.ais.abnormal.analyzer.behaviour.events.AbnormalEventLower;
import dk.dma.ais.abnormal.analyzer.behaviour.events.AbnormalEventMaintain;
import dk.dma.ais.abnormal.analyzer.behaviour.events.AbnormalEventRaise;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The behaviour manager keeps on an eye on the number of consecutive normal and abnormal behaviours of a track.
 * If more than the threshold of abnormal behaviours are observed, then a request to raise an event is posted on
 * the event bus (if no such event was already raised). If more than the the threshold of normal behaviours are
 * observed, then a request to lower an event is posted on the event bus (if an event was requested raised earlier).
 */
public class BehaviourManagerImpl implements BehaviourManager {

    static final Logger LOG = LoggerFactory.getLogger(BehaviourManagerImpl.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    TrackingService trackingService;
    EventBus eventBus = new EventBus();

    /**
     * Number of consecutive abnormal behaviours of track before an event is raised.
     */
    static final int RAISE_EVENT_SCORE_THRESHOLD = 2;

    /**
     * Number of consecutive normal behaviours of track before an event is lowered.
     */
    static final int LOWER_EVENT_SCORE_THRESHOLD = 3;

    private static final String SCORE_KEY_PREFIX = "event-score-";
    private static final String EVENT_STATE_KEY_PREFIX = "event-state-";

    @Inject
    public BehaviourManagerImpl(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @Override
    public void abnormalBehaviourDetected(Class<? extends Event> eventClass, Track track) {
        String scoreKey = getScoreKey(eventClass);

        if (!hasOngoingEvent(eventClass, track)) {
            Integer score = getEventScore(eventClass, track);

            if (score < RAISE_EVENT_SCORE_THRESHOLD) {
                /*
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Increasing severeness of " + eventClass.getSimpleName() + " for " + track.getMmsi() + " from level " + score);
                }
                */
                score++;
                if (score == RAISE_EVENT_SCORE_THRESHOLD) {
                    fireRaiseEvent(eventClass, track);
                    track.removeProperty(scoreKey);
                } else {
                    track.setProperty(scoreKey, score);
                }
            }
        } else {
            track.setProperty(scoreKey, Integer.valueOf(0));
            LOG.debug("fireMaintainEvent - " + track.getMmsi() + " - " + eventClass);
            fireMaintainEvent(eventClass, track);
        }
    }

    @Override
    public void normalBehaviourDetected(Class<? extends Event> eventClass, Track track) {
        String scoreKey = getScoreKey(eventClass);

        if (hasOngoingEvent(eventClass, track)) {
            Integer score = getEventScore(eventClass, track);

            if (score < LOWER_EVENT_SCORE_THRESHOLD) {
                score++;
                if (score == LOWER_EVENT_SCORE_THRESHOLD) {
                    fireLowerEvent(eventClass, track);
                    track.removeProperty(scoreKey);
                } else {
                    track.setProperty(scoreKey, score);
                    LOG.debug("fireMaintainEvent (normal) - " + track.getMmsi() + " - " + eventClass);
                    fireMaintainEvent(eventClass, track);
                }
            }
        } else {
            /*
            if (LOG.isDebugEnabled()) {
                Object score = (Integer) track.getProperty(scoreKey);
                if (score instanceof Integer && (Integer)score >= 1) {
                    LOG.debug(track.getMmsi() + " is back to normal. " + eventClass.getSimpleName() + ".");
                }
            }
            */
            track.setProperty(scoreKey, Integer.valueOf(0));
        }
    }

    @Override
    public void trackStaleDetected(Class<? extends Event> eventClass, Track track) {
        track.removeProperty(getEventStateKey(eventClass));
        track.removeProperty(getScoreKey(eventClass));
    }

    @Override
    public void registerSubscriber(Object subscriber) {
        eventBus.register(subscriber);
    }

    private void fireMaintainEvent(Class<? extends Event> eventClass, Track track) {
        track.setProperty(getEventStateKey(eventClass), Boolean.TRUE);
        eventBus.post(new AbnormalEventMaintain(eventClass, track));
    }

    private void fireRaiseEvent(Class<? extends Event> eventClass, Track track) {
        LOG.debug("fireRaiseEvent - " + track.getMmsi() + " - " + eventClass);
        track.setProperty(getEventStateKey(eventClass), Boolean.TRUE);
        eventBus.post(new AbnormalEventRaise(eventClass, track));
    }

    private void fireLowerEvent(Class<? extends Event> eventClass, Track track) {
        LOG.debug("fireLowerEvent - " + track.getMmsi() + " - " + eventClass);
        track.removeProperty(getEventStateKey(eventClass));
        eventBus.post(new AbnormalEventLower(eventClass, track));
    }

    private static boolean hasOngoingEvent(Class<? extends Event> eventClass, Track track) {
        Object eventState = track.getProperty(getEventStateKey(eventClass));
        return eventState != null && eventState instanceof Boolean && (Boolean) eventState == true;
    }

    private static String getEventStateKey(Class<? extends Event> eventClass) {
        return EVENT_STATE_KEY_PREFIX + eventClass.getSimpleName();
    }

    private static String getScoreKey(Class<? extends Event> eventClass) {
        return SCORE_KEY_PREFIX + eventClass.getSimpleName();
    }

    private static Integer getEventScore(Class<? extends Event> eventClass, Track track) {
        String scoreKey = getScoreKey(eventClass);

        Integer score = (Integer) track.getProperty(scoreKey);
        if (score == null) {
            score = Integer.valueOf(0);
            track.setProperty(scoreKey, score);
        }

        return score;
    }
}
