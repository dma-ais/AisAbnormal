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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for holding information on the file processing process
 */
public class AbnormalStatBuilderStatistics {

    static final Logger LOG = LoggerFactory.getLogger(AbnormalStatBuilderStatistics.class);

    static final long DEFAULT_LOG_INTERVAL = 60 * 1000; // 1 minute

    private final long logInterval;
    private final long startTime = System.currentTimeMillis();

    private long packetCount;
    private long messageCount;
    private long posMsgCount;
    private long statMsgCount;
    private long cellCount;

    private long lastLog;

    public AbnormalStatBuilderStatistics() {
        this.logInterval = DEFAULT_LOG_INTERVAL;
    }

    public AbnormalStatBuilderStatistics(long interval, TimeUnit unit) {
        this.logInterval = unit.toMillis(interval);
    }

    public void incPacketCount() {
        packetCount++;
    }

    public void incMessageCount() {
        messageCount++;
    }

    public void incPosMsgCount() {
        posMsgCount++;
    }

    public void incStatMsgCount() {
        statMsgCount++;
    }
    
    public long getPacketCount() {
        return packetCount;
    }

    public long getMessageCount() {
        return messageCount;
    }

    public long getPosMsgCount() {
        return posMsgCount;
    }

    public long getStatMsgCount() {
        return statMsgCount;
    }

    public long getStartTime() {
        return startTime;
    }
    
    public long getLastLog() {
        return lastLog;
    }
    
    public long getCellCount() {
        return cellCount;
    }
    
    public void setCellCount(long cellCount) {
        this.cellCount = cellCount;
    }

    public double getMessageRate() {
        double secs = (double)(System.currentTimeMillis() - startTime) / 1000.0;
        return (double) messageCount / secs;
    }
    
    public void log(boolean force) {
        if (logInterval <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!force && (now - lastLog < logInterval)) {
            return;
        }
        LOG.info("==== Stat build statistics ====");
        LOG.info(String.format("%-20s %9d", "Pakcet count", packetCount));
        LOG.info(String.format("%-20s %9d", "Message count", messageCount));
        LOG.info(String.format("%-20s %9d", "Pos message count", posMsgCount));
        LOG.info(String.format("%-20s %9d", "Stat message count", statMsgCount));
        LOG.info(String.format("%-20s %9d", "Cell count", cellCount));
        LOG.info(String.format("%-20s %9.0f msg/sec", "Message rate", getMessageRate()));
        LOG.info("==== Stat build statistics ====");
        lastLog = now;
    }

    public void log() {
        log(false);
    }

}
