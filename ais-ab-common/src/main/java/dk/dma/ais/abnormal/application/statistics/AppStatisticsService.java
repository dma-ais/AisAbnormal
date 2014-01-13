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

package dk.dma.ais.abnormal.application.statistics;

import net.jcip.annotations.ThreadSafe;

/**
 * A statistics service class which can be called with updates for certain events, such as reception of
 * a packet or a message. Via the dumpStatistics() method the service can dump its collected statistics
 * to the system logger.
 *
 * The methods of this service are reentrant / MT-safe.
 */

@ThreadSafe
public interface AppStatisticsService {

    /**
     * Start automatic periodic dumping of statistics to log.
     */
    void start();

    /**
     * Stop automatic periodic dumping of statistics to log.
     */
    void stop();

    /**
     * Increment no. of AIS messages received (any type).
     */
    void incMessageCount();

    /**
     * Increment no. of position messages received.
     */
    void incPosMsgCount();

    /**
     * Increment no. of statitic and voyage related messages received.
     */
    void incStatMsgCount();

    /**
     * Increment no. of packets received after any pre-filtering has been performed.
     */
    void incFilteredPacketCount();

    /**
     * Increment no. of packets received prior to any filtering taking place.
     */
    void incUnfilteredPacketCount();

    /**
     * Get the no. of filtered packets received so far.
     * @return the no. of filtered packets
     */
    long getFilteredPacketCount();

    /**
     * Get the no. of messages received so far.
     * @return the no. of messsages received.
     */
    long getMessageCount();

    long getPosMsgCount();

    long getStatMsgCount();

    /**
     * Increment no. of messages received out of sequence.
     */
    void incOutOfSequenceMessages();

    void setTrackCount(int trackCount);

    /**
     * Dump the current state of application statistics to the system log service.
     */
    void dumpStatistics();
}
