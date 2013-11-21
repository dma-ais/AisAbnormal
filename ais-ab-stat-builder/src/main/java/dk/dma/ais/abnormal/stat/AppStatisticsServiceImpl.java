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
package dk.dma.ais.abnormal.stat;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for holding information on the file processing process
 */
@Singleton
public final class AppStatisticsServiceImpl implements AppStatisticsService {

    private static final Logger LOG = LoggerFactory.getLogger(AppStatisticsServiceImpl.class);

    private final long DEFAULT_LOG_INTERVAL = 60 * 1000; // 1 minute

    private final long logInterval;
    private final long startTime = System.currentTimeMillis();

    private long packetCount;
    private long messageCount;
    private long posMsgCount;
    private long statMsgCount;
    private long cellCount;
    private int trackCount;

    private long lastLog;

    public AppStatisticsServiceImpl() {
        this.logInterval = DEFAULT_LOG_INTERVAL;
    }

    /*
    public AppStatisticsServiceImpl(long interval, TimeUnit unit) {
        this.logInterval = unit.toMillis(interval);
    }
    */

    @Override
    public void incPacketCount() {
        packetCount++;
    }

    @Override
    public void incMessageCount() {
        messageCount++;
    }

    @Override
    public void incPosMsgCount() {
        posMsgCount++;
    }

    @Override
    public void incStatMsgCount() {
        statMsgCount++;
    }
    
    @Override
    public long getPacketCount() {
        return packetCount;
    }

    @Override
    public long getMessageCount() {
        return messageCount;
    }

    @Override
    public long getPosMsgCount() {
        return posMsgCount;
    }

    @Override
    public long getStatMsgCount() {
        return statMsgCount;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }
    
    @Override
    public long getLastLog() {
        return lastLog;
    }
    
    @Override
    public long getCellCount() {
        return cellCount;
    }
    
    @Override
    public void setCellCount(long cellCount) {
        this.cellCount = cellCount;
    }

    @Override
    public void setTrackCount(int trackCount) {
        this.trackCount = trackCount;
    }

    @Override
    public double getMessageRate() {
        double secs = (double)(System.currentTimeMillis() - startTime) / 1000.0;
        return (double) messageCount / secs;
    }
    
    @Override
    public void log(boolean force) {
        if (logInterval <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!force && (now - lastLog < logInterval)) {
            return;
        }
        LOG.info("==== Stat build statistics ====");
        LOG.info(String.format("%-30s %9d", "Packet count", packetCount));
        LOG.info(String.format("%-30s %9d", "Message count", messageCount));
        LOG.info(String.format("%-30s %9d", "Pos message count", posMsgCount));
        LOG.info(String.format("%-30s %9d", "Stat message count", statMsgCount));
        LOG.info(String.format("%-30s %9d", "Cell count", cellCount));
        LOG.info(String.format("%-30s %9d", "Track count at termination", trackCount));
        LOG.info(String.format("%-30s %9.0f msg/sec", "Message rate", getMessageRate()));
        LOG.info("==== Stat build statistics ====");
        lastLog = now;
    }

    @Override
    public void log() {
        log(false);
    }

}
