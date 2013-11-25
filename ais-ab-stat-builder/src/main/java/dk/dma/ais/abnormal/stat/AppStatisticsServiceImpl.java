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

import java.util.Date;
import java.util.HashMap;
import java.util.Set;

/**
 * Class for holding information on the file processing process
 */
@Singleton
public final class AppStatisticsServiceImpl implements AppStatisticsService {

    private static final Logger LOG = LoggerFactory.getLogger(AppStatisticsServiceImpl.class);

    private final long DEFAULT_LOG_INTERVAL = 60 * 1000; // 1 minute

    private final long logInterval;
    private final long startTime = System.currentTimeMillis();

    private long unfilteredPacketCount;
    private long filteredPacketCount;
    private long messageCount;
    private long posMsgCount;
    private long statMsgCount;
    private int trackCount;

    private HashMap<String, HashMap<String, Long>> allFeatureStatistics = new HashMap<>();

    private long lastLog;

    public AppStatisticsServiceImpl() {
        this.logInterval = DEFAULT_LOG_INTERVAL;
        this.lastLog = new Date().getTime();
    }

    @Override
    public void incFeatureStatistics(String featureName, String statisticsName) {
        HashMap<String, Long> featureStatistics = (HashMap<String, Long>) this.allFeatureStatistics.get(featureName);
        if (featureStatistics == null) {
            featureStatistics = new HashMap<>();
            this.allFeatureStatistics.put(featureName, featureStatistics);
        }
        Long statistic = featureStatistics.get(statisticsName);
        if (statistic == null) {
            statistic = 0L;
            featureStatistics.put(statisticsName, statistic);
        }
        statistic++;
        featureStatistics.put(statisticsName, statistic);
    }

    @Override
    public Long getFeatureStatistics(String featureName, String statisticsName) {
        HashMap<String, Long> featureStatistics = (HashMap<String, Long>) this.allFeatureStatistics.get(featureName);
        if (featureStatistics == null) {
            return null;
        }
        Long statistic = featureStatistics.get(statisticsName);
        if (statistic == null) {
            return null;
        }
        return statistic;
    }

    @Override
    public void setFeatureStatistics(String featureName, String statisticsName, Long statisticsValue) {
        HashMap<String, Long> featureStatistics = (HashMap<String, Long>) this.allFeatureStatistics.get(featureName);
        if (featureStatistics == null) {
            featureStatistics = new HashMap<>();
            this.allFeatureStatistics.put(featureName, featureStatistics);
        }
        featureStatistics.put(statisticsName, statisticsValue);
    }

    /*
    public AppStatisticsServiceImpl(long interval, TimeUnit unit) {
        this.logInterval = unit.toMillis(interval);
    }
    */

    @Override
    public void incFilteredPacketCount() {
        filteredPacketCount++;
    }

    @Override
    public void incUnfilteredPacketCount() {
        unfilteredPacketCount++;
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
    public long getFilteredPacketCount() {
        return filteredPacketCount;
    }

    @Override
    public long getUnfilteredPacketCount() {
        return unfilteredPacketCount;
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
        LOG.info("==== Stat build application statistics ====");
        LOG.info(String.format("%-30s %9d", "Unfiltered packet count", unfilteredPacketCount));
        LOG.info(String.format("%-30s %9d", "Filtered packet count", filteredPacketCount));
        LOG.info(String.format("%-30s %9d", "Message count", messageCount));
        LOG.info(String.format("%-30s %9d", "Pos message count", posMsgCount));
        LOG.info(String.format("%-30s %9d", "Stat message count", statMsgCount));
        LOG.info(String.format("%-30s %9d", "Track count", trackCount));
        LOG.info(String.format("%-30s %9.0f msg/sec", "Message rate", getMessageRate()));
        LOG.info("==== Stat build application statistics ====");

        LOG.info("==== Stat build feature statistics ====");
        Set<String> featureNames = this.allFeatureStatistics.keySet();
        for (String featureName : featureNames) {
            LOG.info(String.format("%-30s %s", "Feature name", featureName));

            HashMap<String, Long> featureStatistics = this.allFeatureStatistics.get(featureName);
            Set<String> statisticsNames = featureStatistics.keySet();
            for (String statisticsName : statisticsNames) {
                Long statistics = featureStatistics.get(statisticsName);
                LOG.info(String.format("     %-25s %9d", statisticsName, statistics));
            }

        }
        LOG.info("==== Stat build feature statistics ====");

        lastLog = now;
    }

    @Override
    public void log() {
        log(false);
    }

}
