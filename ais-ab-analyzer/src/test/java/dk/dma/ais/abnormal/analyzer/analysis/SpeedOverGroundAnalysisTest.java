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
import dk.dma.ais.abnormal.stat.db.data.SpeedOverGroundFeatureData;
import dk.dma.ais.abnormal.tracker.TrackingService;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SpeedOverGroundAnalysisTest {

    private JUnit4Mockery context;

    private long testCellId = 24930669189L;

    private TrackingService trackingService;
    private AppStatisticsService statisticsService;
    private FeatureDataRepository featureDataRepository;
    private EventRepository eventRepository;
    private SpeedOverGroundFeatureData featureData;

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
     * SOG is never considered abnormal when shipCount is lower than threshold.
     */
    @Test
    public void neverAbnormalSOGWhenShipCountBelowThreshold() {
        // Mock featureData table
        featureData = SpeedOverGroundFeatureData.create();
        featureData.setValue((short) 0, (short) 0, (short) 0, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 1, (short) 0, (short) 1, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 1, (short) 1, (short) 1, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 1, (short) 2, (short) 0, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 0, (short) 0, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 4, (short) 0, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 4, (short) 1, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 4, (short) 2, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 975);
        featureData.setValue((short) 2, (short) 5, (short) 0, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 5, (short) 1, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 5, (short) 2, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 5, (short) 3, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 5, (short) 4, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 5, (short) 5, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);

        final int sum  = featureData.getSumFor(SpeedOverGroundFeatureData.STAT_SHIP_COUNT);
        final int n1    = featureData.getValue(2, 4, 2, SpeedOverGroundFeatureData.STAT_SHIP_COUNT);
        final int n2    = featureData.getValue(2, 4, 1, SpeedOverGroundFeatureData.STAT_SHIP_COUNT);
        assertTrue(sum < SpeedOverGroundAnalysis.TOTAL_SHIP_COUNT_THRESHOLD);
        assertTrue((float) n1 / (float) sum > 0.01);
        assertTrue((float) n2 / (float) sum < 0.01);

        final SpeedOverGroundAnalysis analysis = new SpeedOverGroundAnalysis(statisticsService, featureDataRepository, trackingService, eventRepository);

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with("SpeedOverGroundAnalysis"), with(any(String.class)));
            oneOf(featureDataRepository).getFeatureData("SpeedOverGroundFeature", 123456L); will(returnValue(featureData));
        }});
        assertFalse(analysis.isAbnormalSpeedOverGround(123456L, 2, 4, 2));

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with("SpeedOverGroundAnalysis"), with(any(String.class)));
            oneOf(featureDataRepository).getFeatureData("SpeedOverGroundFeature", 123456L);
            will(returnValue(featureData));
        }});
        assertFalse(analysis.isAbnormalSpeedOverGround(123456L, 2, 4, 1));
    }

    /**
     * SOG is considered abnormal where appropriate when shipCount is above threshold.
     */
    @Test
    public void abnormalSOGWhenShipCountAboveThreshold() {
        // Mock featureData table
        featureData = SpeedOverGroundFeatureData.create();
        featureData.setValue((short) 0, (short) 0, (short) 0, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 1, (short) 0, (short) 1, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 1, (short) 1, (short) 1, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 1, (short) 2, (short) 0, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 0, (short) 0, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 4, (short) 0, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 4, (short) 1, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 4, (short) 2, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 5, (short) 0, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 5, (short) 1, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 5, (short) 2, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 5, (short) 3, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 5, (short) 4, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1);
        featureData.setValue((short) 2, (short) 5, (short) 5, SpeedOverGroundFeatureData.STAT_SHIP_COUNT, 1000);

        final int sum   = featureData.getSumFor(SpeedOverGroundFeatureData.STAT_SHIP_COUNT);
        final int n1    = featureData.getValue(2, 4, 1, SpeedOverGroundFeatureData.STAT_SHIP_COUNT);
        final int n2    = featureData.getValue(2, 5, 5, SpeedOverGroundFeatureData.STAT_SHIP_COUNT);
        final float pd1 = (float) n1 / (float) sum;
        final float pd2 = (float) n2 / (float) sum;
        assertTrue(sum > SpeedOverGroundAnalysis.TOTAL_SHIP_COUNT_THRESHOLD);
        assertTrue(pd1 < 0.001);
        assertTrue(pd2 > 0.001);

        final SpeedOverGroundAnalysis analysis = new SpeedOverGroundAnalysis(statisticsService, featureDataRepository, trackingService, eventRepository);

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with("SpeedOverGroundAnalysis"), with(any(String.class)));
            oneOf(featureDataRepository).getFeatureData("SpeedOverGroundFeature", 123456L); will(returnValue(featureData));
        }});
        assertNotNull(featureData.getValue(2, 4, 1, SpeedOverGroundFeatureData.STAT_SHIP_COUNT));
        assertTrue(analysis.isAbnormalSpeedOverGround(123456L, 2, 4, 1));

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with("SpeedOverGroundAnalysis"), with(any(String.class)));
            oneOf(featureDataRepository).getFeatureData("SpeedOverGroundFeature", 123456L); will(returnValue(featureData));
        }});
        assertNull(featureData.getValue(1, 3, 1, SpeedOverGroundFeatureData.STAT_SHIP_COUNT));
        assertTrue(analysis.isAbnormalSpeedOverGround(123456L, 1, 3, 1));

        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with("SpeedOverGroundAnalysis"), with(any(String.class)));
            oneOf(featureDataRepository).getFeatureData("SpeedOverGroundFeature", 123456L); will(returnValue(featureData));
        }});
        assertFalse(analysis.isAbnormalSpeedOverGround(123456L, 2, 5, 5));
    }

}
