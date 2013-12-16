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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class for holding information on the file processing process
 */
@Singleton
public final class AppStatisticsServiceImpl extends dk.dma.ais.abnormal.application.statistics.AppStatisticsServiceImpl implements AppStatisticsService {

    private static final Logger LOG = LoggerFactory.getLogger(AppStatisticsServiceImpl.class);

    @Inject
    private StripedExecutorService executorService;

    private final AtomicInteger trackCount = new AtomicInteger(0);
    private Map<String, HashMap<String, Long>> allFeatureStatistics = new ConcurrentHashMap<>();

    public AppStatisticsServiceImpl() {
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
    public void setTrackCount(int trackCount) {
        this.trackCount.set(trackCount);
    }

    @Override
    public void dumpStatistics() {
        super.dumpStatistics();

        LOG.info("==== Stat build application statistics (tasks) ====");

        LOG.info(String.format("%-30s %s", "Executor isShutdown", executorService.isShutdown()));
        LOG.info(String.format("%-30s %s", "Executor isTerminated", executorService.isTerminated()));
        LOG.info(String.format("%-30s %9d", "Executor no. of threads", executorService.numberOfExecutors()));
        Map<String,Integer> queueSizes = executorService.serialExecutorQueueSizes();
        for (Map.Entry<String,Integer> queueSize : queueSizes.entrySet()) {
            LOG.info(String.format("%-30s %9d", "Queue size, thread " + queueSize.getKey(), queueSize.getValue()));
        }
        LOG.info("==== Stat build application statistics ====");
        LOG.info(String.format("%-30s %9d", "Track count", trackCount.get()));
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
