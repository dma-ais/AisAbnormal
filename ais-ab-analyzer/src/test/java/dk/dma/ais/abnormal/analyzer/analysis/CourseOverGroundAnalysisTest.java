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
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import dk.dma.ais.abnormal.stat.db.data.CourseOverGroundFeatureData;
import dk.dma.ais.abnormal.tracker.TrackingService;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CourseOverGroundAnalysisTest {

    private JUnit4Mockery context;

    private long testCellId = 24930669189L;

    private TrackingService trackingService;
    private AppStatisticsService statisticsService;
    private FeatureDataRepository featureDataRepository;
    private EventRepository eventRepository;
    private CourseOverGroundFeatureData featureData;

    @Before
    public void prepareTest() {
        context = new JUnit4Mockery();

        // Mock dependencies
        trackingService = context.mock(TrackingService.class);
        statisticsService = context.mock(AppStatisticsService.class);
        featureDataRepository = context.mock(FeatureDataRepository.class);
        eventRepository = context.mock(EventRepository.class);
    }

    /**
     * COG is never considered abnormal when shipCount is lower than threshold.
     */
    @Test
    public void neverAbnormalCOGWhenShipCountBelowThreshold() {
        // Mock featureData table
        featureData = CourseOverGroundFeatureData.create();
        featureData.setValue((short) 0, (short) 0, (short) 0, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 1, (short) 0, (short) 1, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 1, (short) 1, (short) 1, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 1, (short) 2, (short) 0, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 0, (short) 0, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 4, (short) 0, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 4, (short) 1, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 975);
        featureData.setValue((short) 2, (short) 4, (short) 2, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 3, (short) 1, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 3, (short) 2, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 3, (short) 3, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 3, (short) 4, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 3, (short) 5, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);

        final int sum  = featureData.getSumFor(CourseOverGroundFeatureData.STAT_SHIP_COUNT);
        final int n1    = featureData.getValue(2, 4, 1, CourseOverGroundFeatureData.STAT_SHIP_COUNT);
        final int n2    = featureData.getValue(2, 4, 2, CourseOverGroundFeatureData.STAT_SHIP_COUNT);
        assertTrue(sum < CourseOverGroundAnalysis.TOTAL_SHIP_COUNT_THRESHOLD);
        assertTrue((float) n1 / (float) sum > 0.01);
        assertTrue((float) n2 / (float) sum < 0.01);
        
        final CourseOverGroundAnalysis analysis = new CourseOverGroundAnalysis(statisticsService, featureDataRepository, trackingService, eventRepository);

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with("CourseOverGroundAnalysis"), with(any(String.class)));
            oneOf(featureDataRepository).getFeatureData("CourseOverGroundFeature", 123456L); will(returnValue(featureData));
        }});
        assertFalse(analysis.isAbnormalCourseOverGround(123456L, 2, 4, 1));

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with("CourseOverGroundAnalysis"), with(any(String.class)));
            oneOf(featureDataRepository).getFeatureData("CourseOverGroundFeature", 123456L); will(returnValue(featureData));
        }});
        assertFalse(analysis.isAbnormalCourseOverGround(123456L, 2, 4, 2));
    }

    /**
     * COG is considered abnormal where appropriate when shipCount is above threshold.
     */
    @Test
    public void abnormalCOGWhenShipCountAboveThreshold() {
        // Mock featureData table
        featureData = CourseOverGroundFeatureData.create();
        featureData.setValue((short) 0, (short) 0, (short) 0, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 1, (short) 0, (short) 1, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 1, (short) 1, (short) 1, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 1, (short) 2, (short) 0, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 0, (short) 0, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 4, (short) 0, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 4, (short) 1, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 4, (short) 2, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 3, (short) 0, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 3, (short) 1, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 3, (short) 2, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 3, (short) 3, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 3, (short) 4, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 254);
        featureData.setValue((short) 2, (short) 3, (short) 5, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 1000);

        final int sum   = featureData.getSumFor(CourseOverGroundFeatureData.STAT_SHIP_COUNT);
        final int n1    = featureData.getValue(2, 4, 1, CourseOverGroundFeatureData.STAT_SHIP_COUNT);
        final int n2    = featureData.getValue(2, 3, 4, CourseOverGroundFeatureData.STAT_SHIP_COUNT);
        final int n3    = featureData.getValue(2, 3, 5, CourseOverGroundFeatureData.STAT_SHIP_COUNT);
        final float pd1 = (float) n1 / (float) sum;
        final float pd2 = (float) n2 / (float) sum;
        final float pd3 = (float) n3 / (float) sum;
        assertTrue(sum > CourseOverGroundAnalysis.TOTAL_SHIP_COUNT_THRESHOLD);
        assertTrue(pd1 < 0.001);
        assertTrue(pd2 > 0.001);
        assertTrue(pd2 > 0.001);

        final CourseOverGroundAnalysis analysis = new CourseOverGroundAnalysis(statisticsService, featureDataRepository, trackingService, eventRepository);

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with("CourseOverGroundAnalysis"), with(any(String.class)));
            oneOf(featureDataRepository).getFeatureData("CourseOverGroundFeature", 123456L); will(returnValue(featureData));
        }});
        assertNotNull(featureData.getValue(2, 4, 0, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
        assertTrue(analysis.isAbnormalCourseOverGround(123456L, 2, 4, 0));

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with("CourseOverGroundAnalysis"), with(any(String.class)));
            oneOf(featureDataRepository).getFeatureData("CourseOverGroundFeature", 123456L); will(returnValue(featureData));
        }});
        assertNull(featureData.getValue(2, 0, 2, CourseOverGroundFeatureData.STAT_SHIP_COUNT)); // null
        assertTrue(analysis.isAbnormalCourseOverGround(123456L, 2, 0, 2));

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with("CourseOverGroundAnalysis"), with(any(String.class)));
            oneOf(featureDataRepository).getFeatureData("CourseOverGroundFeature", 123456L); will(returnValue(featureData));
        }});
        assertFalse(analysis.isAbnormalCourseOverGround(123456L, 2, 3, 4));

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with("CourseOverGroundAnalysis"), with(any(String.class)));
            oneOf(featureDataRepository).getFeatureData("CourseOverGroundFeature", 123456L); will(returnValue(featureData));
        }});
        assertFalse(analysis.isAbnormalCourseOverGround(123456L, 2, 3, 5));
    }

}
