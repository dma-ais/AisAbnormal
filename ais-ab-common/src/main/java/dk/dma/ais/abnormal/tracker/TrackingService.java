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

import dk.dma.ais.message.AisMessage;

import java.util.Date;
import java.util.Set;

/**
 * A tracking service receives AisMessages and based on these it maintains a collection of all known tracks,
 * including their position, speed, course, etc.
 *
 * If a certain track has not received any updates for a while
 * it enteres status 'stale' and will receive no further updates. Instead a new track is created if more
 * AisMessages are received from the same vessel later on.
 *
 * The AisMesssages are assumed to arrive in timely order; i.e. by ever-increasing values of timestamp.
 */
public interface TrackingService {

    /**
     * Update the tracker with a new aisMessage.
     * @param timestamp time when the aisMessage was received.
     * @param aisMessage the AIS message.
     */
    void update(Date timestamp, AisMessage aisMessage);

    /**
     * Count and return the number of tracks current ACTIVE or STALE in the tracker.
     * @return the no. of tracks.
     */
    Integer getNumberOfTracks();

    /**
     *  Register a subscriber to receive Events from the tracker.
     */
    void registerSubscriber(Object subscriber);

    /**
     * Get a set of cloned tracks from the tracker. The fact that the list is cloned makes is possible
     * to work with its contents in isolation without worrying about multi-theaded behaviour in the tracker.
     *
     * @return A list of Tracks cloned from the tracker.
     */
    Set<Track> cloneTracks();
}
