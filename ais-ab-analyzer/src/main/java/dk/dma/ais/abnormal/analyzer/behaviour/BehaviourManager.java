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

import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.tracker.eventEmittingTracker.Track;

/**
 * The behaviour manager keeps on an eye on the number of consecutive normal and abnormal behaviours of a track.
 * If the behaviour is to that it can be considered abnormal, then a request to raise an event is posted on
 * the event bus. If a track is already considered behaving abnormally but starts to behave normally, then a
 * request to lower an event is posted on the event bus.
 *
 * @author Thomas Borg Salling <tbsalling@tbsalling.dk>
 */
public interface BehaviourManager {
    /**
     * Mark that a normal behaviour for a track has been detected. This information is used for vote for
     * lowering of any similar events, that may be raised for this vessel.
     */
    void abnormalBehaviourDetected(Class<? extends Event> eventClass, Track track);

    /**
     * Mark that an abnormal behaviour for a track has been detected. This information is used for vote for
     * raising an 'abnormal event' for this vessel.
     */
    void normalBehaviourDetected(Class<? extends Event> eventClass, Track track);

    /**
     * Mark that a track has been detected as stale
     * @param track
     */
    void trackStaleDetected(Class<? extends Event> eventClass, Track track);

    /**
     * Register an event bus subscriber
     * @param subscriber
     */
    void registerSubscriber(Object subscriber);

    /**
     * Get the event certainty computed for the most recent position report.
     * @param eventClass
     * @return
     */
    EventCertainty getEventCertaintyAtCurrentPosition(Class<? extends Event> eventClass, Track track);
}
