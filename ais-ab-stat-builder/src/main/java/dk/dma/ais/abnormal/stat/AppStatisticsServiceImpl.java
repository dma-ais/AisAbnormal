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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.dma.ais.concurrency.stripedexecutor.StripedExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class for holding information on the file processing process
 */
@Singleton
public final class AppStatisticsServiceImpl implements AppStatisticsService {

    private static final Logger LOG = LoggerFactory.getLogger(AppStatisticsServiceImpl.class);

    @Inject
    private StripedExecutorService executorService;

    private static final long DEFAULT_LOG_INTERVAL = 60 * 1000; // 1 minute
    private final long logInterval;
    private final AtomicLong lastLog = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong lastMessageCount = new AtomicLong(0);

    private final AtomicLong unfilteredPacketCount = new AtomicLong(0);
    private final AtomicLong filteredPacketCount = new AtomicLong(0);
    private final AtomicLong messageCount = new AtomicLong(0);
    private final AtomicLong posMsgCount = new AtomicLong(0);
    private final AtomicLong statMsgCount = new AtomicLong(0);
    private final AtomicInteger trackCount = new AtomicInteger(0);

    private Map<String, HashMap<String, Long>> allFeatureStatistics = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    public AppStatisticsServiceImpl() {
        this.logInterval = DEFAULT_LOG_INTERVAL;
    }

    @Override
    public void start() {
        LOG.debug("Starting statistics service.");
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
            dumpStatistics();
            }
        }, 0 /* logInterval */, logInterval, TimeUnit.MILLISECONDS);
        LOG.info("Statistics service started.");
    }

    @Override
    public void stop() {
        LOG.debug("Stopping statistics service.");
        scheduledExecutorService.shutdown();
        try {
            scheduledExecutorService.awaitTermination(2*logInterval, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.info("Statistics service stopped.");
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

    @Override
    public void incFilteredPacketCount() {
        filteredPacketCount.incrementAndGet();
    }

    @Override
    public void incUnfilteredPacketCount() {
        this.unfilteredPacketCount.incrementAndGet();
    }

    @Override
    public void incMessageCount() {
        messageCount.incrementAndGet();
    }

    @Override
    public void incPosMsgCount() {
        posMsgCount.incrementAndGet();
    }

    @Override
    public void incStatMsgCount() {
        statMsgCount.incrementAndGet();
    }
    
    @Override
    public long getFilteredPacketCount() {
        return filteredPacketCount.get();
    }

    @Override
    public long getUnfilteredPacketCount() {
        return unfilteredPacketCount.get();
    }

    @Override
    public long getMessageCount() {
        return messageCount.get();
    }

    @Override
    public long getPosMsgCount() {
        return posMsgCount.get();
    }

    @Override
    public long getStatMsgCount() {
        return statMsgCount.get();
    }

    @Override
    public void setTrackCount(int trackCount) {
        this.trackCount.set(trackCount);
    }

    @Override
    public double getMessageRate() {
        double secs = (double)(System.currentTimeMillis() - lastLog.get()) / 1000.0;
        long msgs = messageCount.get() - lastMessageCount.get();

        lastLog.set(System.currentTimeMillis());
        lastMessageCount.set(messageCount.get());

        return (double) msgs / secs;
    }

    @Override
    public void dumpStatistics() {
        LOG.info("==== Stat build application statistics (tasks) ====");

        LOG.info(String.format("%-30s %s", "Executor isShutdown", executorService.isShutdown()));
        LOG.info(String.format("%-30s %s", "Executor isTerminated", executorService.isTerminated()));
        LOG.info(String.format("%-30s %9d", "Executor no. of threads", executorService.numberOfExecutors()));
        Map<String,Integer> queueSizes = executorService.serialExecutorQueueSizes();
        for (Map.Entry<String,Integer> queueSize : queueSizes.entrySet()) {
            LOG.info(String.format("%-30s %9d", "Queue size, thread " + queueSize.getKey(), queueSize.getValue()));
        }
        LOG.info("==== Stat build application statistics ====");

        LOG.info("==== Stat build application statistics ====");
        LOG.info(String.format("%-30s %9d", "Unfiltered packet count", unfilteredPacketCount.get()));
        LOG.info(String.format("%-30s %9d", "Filtered packet count", filteredPacketCount.get()));
        LOG.info(String.format("%-30s %9d", "Message count", messageCount.get()));
        LOG.info(String.format("%-30s %9d", "Pos message count", posMsgCount.get()));
        LOG.info(String.format("%-30s %9d", "Stat message count", statMsgCount.get()));
        LOG.info(String.format("%-30s %9d", "Track count", trackCount.get()));
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
    }
}
