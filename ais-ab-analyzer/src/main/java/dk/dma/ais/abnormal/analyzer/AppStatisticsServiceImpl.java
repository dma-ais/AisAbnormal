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
package dk.dma.ais.abnormal.analyzer;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class for holding information on the file processing process
 */
@Singleton
public class AppStatisticsServiceImpl extends dk.dma.ais.abnormal.application.statistics.AppStatisticsServiceImpl implements AppStatisticsService {

    private static final Logger LOG = LoggerFactory.getLogger(AppStatisticsServiceImpl.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private Map<String, HashMap<String, Long>> allAnalysisStatistics = new ConcurrentHashMap<>();

    public AppStatisticsServiceImpl() {
    }

    @Override
    public void incAnalysisStatistics(String analysisName, String statisticsName) {
        HashMap<String, Long> analysisStatistics = (HashMap<String, Long>) this.allAnalysisStatistics.get(analysisName);
        if (analysisStatistics == null) {
            analysisStatistics = new HashMap<>();
            this.allAnalysisStatistics.put(analysisName, analysisStatistics);
        }
        Long statistic = analysisStatistics.get(statisticsName);
        if (statistic == null) {
            statistic = 0L;
            analysisStatistics.put(statisticsName, statistic);
        }
        statistic++;
        analysisStatistics.put(statisticsName, statistic);
    }
}
