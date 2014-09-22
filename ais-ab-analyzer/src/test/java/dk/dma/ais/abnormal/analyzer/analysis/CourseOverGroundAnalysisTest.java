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

package dk.dma.ais.abnormal.analyzer.analysis;

import dk.dma.ais.abnormal.analyzer.AppStatisticsService;
import dk.dma.ais.abnormal.analyzer.behaviour.BehaviourManager;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.stat.db.StatisticDataRepository;
import dk.dma.ais.abnormal.stat.db.data.CourseOverGroundStatisticData;
import dk.dma.ais.abnormal.tracker.Tracker;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_COG_CELL_SHIPCOUNT_MIN;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_COG_PD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CourseOverGroundAnalysisTest {

    private JUnit4Mockery context;

    private Configuration configuration;
    private Tracker trackingService;
    private AppStatisticsService statisticsService;
    private StatisticDataRepository statisticsRepository;
    private EventRepository eventRepository;
    private BehaviourManager behaviourManager;
    private CourseOverGroundStatisticData statistics;

    @Before
    public void prepareTest() {
        context = new JUnit4Mockery();

        // Mock dependencies
        trackingService = context.mock(Tracker.class);
        statisticsService = context.mock(AppStatisticsService.class);
        statisticsRepository = context.mock(StatisticDataRepository.class);
        eventRepository = context.mock(EventRepository.class);
        behaviourManager = context.mock(BehaviourManager.class);

        configuration = new PropertiesConfiguration();
        configuration.setProperty(CONFKEY_ANALYSIS_COG_CELL_SHIPCOUNT_MIN, 1000);
        configuration.setProperty(CONFKEY_ANALYSIS_COG_PD, 0.001);
    }

    /**
     * COG is never considered abnormal when shipCount is lower than threshold.
     */
    @Test
    public void neverAbnormalCOGWhenShipCountBelowThreshold() {
        // Mock statistics table
        statistics = CourseOverGroundStatisticData.create();
        statistics.setValue((short) 0, (short) 0, (short) 0, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 1, (short) 0, (short) 1, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 1, (short) 1, (short) 1, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 1, (short) 2, (short) 0, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 2, (short) 0, (short) 0, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 2, (short) 4, (short) 0, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 2, (short) 4, (short) 1, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 975);
        statistics.setValue((short) 2, (short) 4, (short) 2, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 2, (short) 3, (short) 1, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 2, (short) 3, (short) 2, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 2, (short) 3, (short) 3, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 2, (short) 3, (short) 4, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 2, (short) 3, (short) 5, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);

        final int sum  = statistics.getSumFor(CourseOverGroundStatisticData.STAT_SHIP_COUNT);
        final int n1    = statistics.getValue(2, 4, 1, CourseOverGroundStatisticData.STAT_SHIP_COUNT);
        final int n2    = statistics.getValue(2, 4, 2, CourseOverGroundStatisticData.STAT_SHIP_COUNT);
        assertTrue(sum < configuration.getInt(CONFKEY_ANALYSIS_COG_CELL_SHIPCOUNT_MIN));
        assertTrue((float) n1 / (float) sum > 0.01);
        assertTrue((float) n2 / (float) sum < 0.01);

        context.checking(new Expectations() {{
            oneOf(behaviourManager).registerSubscriber(with(any(CourseOverGroundAnalysis.class)));
        }});
        final CourseOverGroundAnalysis analysis = new CourseOverGroundAnalysis(configuration, statisticsService, statisticsRepository, trackingService, eventRepository, behaviourManager);

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with("CourseOverGroundAnalysis"), with(any(String.class)));
            oneOf(statisticsRepository).getStatisticData("CourseOverGroundStatistic", 123456L); will(returnValue(statistics));
        }});
        assertFalse(analysis.isAbnormalCourseOverGround(123456L, 2, 4, 1));

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with("CourseOverGroundAnalysis"), with(any(String.class)));
            oneOf(statisticsRepository).getStatisticData("CourseOverGroundStatistic", 123456L); will(returnValue(statistics));
        }});
        assertFalse(analysis.isAbnormalCourseOverGround(123456L, 2, 4, 2));
    }

    /**
     * COG is considered abnormal where appropriate when shipCount is above threshold.
     */
    @Test
    public void abnormalCOGWhenShipCountAboveThreshold() {
        // Mock statistics table
        statistics = CourseOverGroundStatisticData.create();
        statistics.setValue((short) 0, (short) 0, (short) 0, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 1, (short) 0, (short) 1, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 1, (short) 1, (short) 1, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 1, (short) 2, (short) 0, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 2, (short) 0, (short) 0, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 2, (short) 4, (short) 0, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 2, (short) 4, (short) 1, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 2, (short) 4, (short) 2, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 2, (short) 3, (short) 0, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 2, (short) 3, (short) 1, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 2, (short) 3, (short) 2, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 2, (short) 3, (short) 3, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);
        statistics.setValue((short) 2, (short) 3, (short) 4, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 254);
        statistics.setValue((short) 2, (short) 3, (short) 5, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1000);

        final int sum   = statistics.getSumFor(CourseOverGroundStatisticData.STAT_SHIP_COUNT);
        final int n1    = statistics.getValue(2, 4, 1, CourseOverGroundStatisticData.STAT_SHIP_COUNT);
        final int n2    = statistics.getValue(2, 3, 4, CourseOverGroundStatisticData.STAT_SHIP_COUNT);
        final int n3    = statistics.getValue(2, 3, 5, CourseOverGroundStatisticData.STAT_SHIP_COUNT);
        final float pd1 = (float) n1 / (float) sum;
        final float pd2 = (float) n2 / (float) sum;
        final float pd3 = (float) n3 / (float) sum;
        assertTrue(sum > configuration.getInt(CONFKEY_ANALYSIS_COG_CELL_SHIPCOUNT_MIN));
        assertEquals(configuration.getFloat(CONFKEY_ANALYSIS_COG_PD), 0.001, 1e-6);
        assertTrue(pd1 < 0.001);
        assertTrue(pd2 > 0.001);
        assertTrue(pd2 > 0.001);

        context.checking(new Expectations() {{
            oneOf(behaviourManager).registerSubscriber(with(any(CourseOverGroundAnalysis.class)));
        }});
        final CourseOverGroundAnalysis analysis = new CourseOverGroundAnalysis(configuration, statisticsService, statisticsRepository, trackingService, eventRepository, behaviourManager);

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with("CourseOverGroundAnalysis"), with(any(String.class)));
            oneOf(statisticsRepository).getStatisticData("CourseOverGroundStatistic", 123456L); will(returnValue(statistics));
        }});
        assertNotNull(statistics.getValue(2, 4, 0, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
        assertTrue(analysis.isAbnormalCourseOverGround(123456L, 2, 4, 0));

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with("CourseOverGroundAnalysis"), with(any(String.class)));
            oneOf(statisticsRepository).getStatisticData("CourseOverGroundStatistic", 123456L); will(returnValue(statistics));
        }});
        assertNull(statistics.getValue(2, 0, 2, CourseOverGroundStatisticData.STAT_SHIP_COUNT)); // null
        assertTrue(analysis.isAbnormalCourseOverGround(123456L, 2, 0, 2));

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with("CourseOverGroundAnalysis"), with(any(String.class)));
            oneOf(statisticsRepository).getStatisticData("CourseOverGroundStatistic", 123456L); will(returnValue(statistics));
        }});
        assertFalse(analysis.isAbnormalCourseOverGround(123456L, 2, 3, 4));

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with("CourseOverGroundAnalysis"), with(any(String.class)));
            oneOf(statisticsRepository).getStatisticData("CourseOverGroundStatistic", 123456L); will(returnValue(statistics));
        }});
        assertFalse(analysis.isAbnormalCourseOverGround(123456L, 2, 3, 5));
    }

}
