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

/**
 * Class for holding information on the file processing process
 */
@Singleton
public final class AppStatisticsServiceImpl extends dk.dma.ais.abnormal.application.statistics.AppStatisticsServiceImpl implements AppStatisticsService {

    private static final Logger LOG = LoggerFactory.getLogger(AppStatisticsServiceImpl.class);

    @Inject
    private StripedExecutorService executorService;

    private Map<String, HashMap<String, Long>> allStatisticStatistics = new ConcurrentHashMap<>();

    public AppStatisticsServiceImpl() {
    }

    @Override
    public void incStatisticStatistics(String statisticName, String statisticsName) {
        HashMap<String, Long> statisticStatistics = (HashMap<String, Long>) this.allStatisticStatistics.get(statisticName);
        if (statisticStatistics == null) {
            statisticStatistics = new HashMap<>();
            this.allStatisticStatistics.put(statisticName, statisticStatistics);
        }
        Long statistic = statisticStatistics.get(statisticsName);
        if (statistic == null) {
            statistic = 0L;
            statisticStatistics.put(statisticsName, statistic);
        }
        statistic++;
        statisticStatistics.put(statisticsName, statistic);
    }

    @Override
    public Long getStatisticStatistics(String statisticName, String statisticsName) {
        HashMap<String, Long> statisticStatistics = (HashMap<String, Long>) this.allStatisticStatistics.get(statisticName);
        if (statisticStatistics == null) {
            return null;
        }
        Long statistic = statisticStatistics.get(statisticsName);
        if (statistic == null) {
            return null;
        }
        return statistic;
    }

    @Override
    public void dumpStatistics() {
        super.dumpStatistics();

        LOG.info("==== Stat builder application statistics (tasks) ====");
        LOG.info(String.format("%-30s %s", "Executor isShutdown", executorService.isShutdown()));
        LOG.info(String.format("%-30s %s", "Executor isTerminated", executorService.isTerminated()));
        LOG.info(String.format("%-30s %9d", "Executor no. of threads", executorService.numberOfExecutors()));
        Map<String,Integer> queueSizes = executorService.serialExecutorQueueSizes();
        for (Map.Entry<String,Integer> queueSize : queueSizes.entrySet()) {
            LOG.info(String.format("%-30s %9d", "Queue size, thread " + queueSize.getKey(), queueSize.getValue()));
        }

        LOG.info("==== Stat builder statistic statistics (statistics) ====");
        Set<String> statisticNames = this.allStatisticStatistics.keySet();
        for (String statisticName : statisticNames) {
            LOG.info(String.format("%-30s %s", "TrackingEventListener name", statisticName));

            HashMap<String, Long> statisticStatistics = this.allStatisticStatistics.get(statisticName);
            Set<String> statisticsNames = statisticStatistics.keySet();
            for (String statisticsName : statisticsNames) {
                Long statistics = statisticStatistics.get(statisticsName);
                LOG.info(String.format("     %-25s %9d", statisticsName, statistics));
            }

        }
        LOG.info("==== Stat builder statistic statistics ====");
    }
}
