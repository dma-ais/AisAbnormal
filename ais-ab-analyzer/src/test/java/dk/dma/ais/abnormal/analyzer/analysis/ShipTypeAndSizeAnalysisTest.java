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
import dk.dma.ais.abnormal.stat.db.data.ShipTypeAndSizeData;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.test.helpers.ArgumentCaptor;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ShipTypeAndSizeAnalysisTest {

    private JUnit4Mockery context;

    private long testCellId = 24930669189L;

    private TrackingService trackingService;
    private AppStatisticsService statisticsService;
    private FeatureDataRepository featureDataRepository;
    private EventRepository eventRepository;
    private ShipTypeAndSizeData featureData;

    @Before
    public void prepareTest() {
        context = new JUnit4Mockery();

        // Mock dependencies
        trackingService = context.mock(TrackingService.class);
        statisticsService = context.mock(AppStatisticsService.class);
        featureDataRepository = context.mock(FeatureDataRepository.class);
        eventRepository = context.mock(EventRepository.class);

        // Mock shipCount table
        featureData = new ShipTypeAndSizeData();
        featureData.setStatistic((short) 3, (short) 1, "shipCount", 17);
        featureData.setStatistic((short) 3, (short) 2, "shipCount", 2);
        featureData.setStatistic((short) 3, (short) 3, "shipCount", 87);
        featureData.setStatistic((short) 3, (short) 4, "shipCount", 618);
        featureData.setStatistic((short) 4, (short) 3, "shipCount", 842);
        featureData.setStatistic((short) 4, (short) 4, "shipCount", 954);
        featureData.setStatistic((short) 5, (short) 3, "shipCount", 154);
        featureData.setStatistic((short) 6, (short) 4, "shipCount", 34);
    }

    @Test
    public void abnormalWhereNoShipCountStatistics() {
        // Assert that pre-conditions are as expected
        assertNull(featureData.getStatistic((short) 1, (short) 1, "shipCount"));

        // Setup expectations
        final ArgumentCaptor<Analysis> analysisCaptor = ArgumentCaptor.forClass(Analysis.class);
        context.checking(new Expectations() {{
            oneOf(trackingService).registerSubscriber(with(analysisCaptor.getMatcher()));
            oneOf(featureDataRepository).getFeatureData("ShipTypeAndSizeFeature", testCellId); will(returnValue(featureData));
            ignoring(statisticsService).incAnalysisStatistics(with(ShipTypeAndSizeAnalysis.class.getSimpleName()), with(any(String.class)));
        }});

        // Create object under test
        final ShipTypeAndSizeAnalysis analysis = new ShipTypeAndSizeAnalysis(statisticsService, featureDataRepository, trackingService, eventRepository);

        // Perform test
        boolean isAbnormalEvent = analysis.isAbnormalCellForShipTypeAndSize(testCellId, (short) 1, (short) 1);

        // Assert results
        context.assertIsSatisfied();
        assertEquals(analysis, analysisCaptor.getCapturedObject());
        assertTrue(isAbnormalEvent);
    }

    @Test
    public void abnormalWhereLowShipCountStatistics() {
        // Assert that pre-conditions are as expected
        Integer totalCount = featureData.getSumFor("shipCount");
        Integer count = (Integer) featureData.getStatistic((short) 3, (short) 2, "shipCount");
        float pd = (float) count / (float) totalCount;
        assertTrue(pd < 0.001);

        // Setup expectations
        final ArgumentCaptor<Analysis> analysisCaptor = ArgumentCaptor.forClass(Analysis.class);
        context.checking(new Expectations() {{
            oneOf(trackingService).registerSubscriber(with(analysisCaptor.getMatcher()));
            oneOf(featureDataRepository).getFeatureData("ShipTypeAndSizeFeature", testCellId); will(returnValue(featureData));
            ignoring(statisticsService).incAnalysisStatistics(with(ShipTypeAndSizeAnalysis.class.getSimpleName()), with(any(String.class)));
        }});

        // Create object under test
        final ShipTypeAndSizeAnalysis analysis = new ShipTypeAndSizeAnalysis(statisticsService, featureDataRepository, trackingService, eventRepository);

        // Perform test
        boolean isAbnormalEvent = analysis.isAbnormalCellForShipTypeAndSize(testCellId, (short) 3, (short) 2);

        // Assert results
        context.assertIsSatisfied();
        assertEquals(analysis, analysisCaptor.getCapturedObject());
        assertTrue(isAbnormalEvent);
    }

    @Test
    public void normalWhereHighShipCountStatistics() {
        // Assert that pre-conditions are as expected
        Integer totalCount = featureData.getSumFor("shipCount");
        Integer count = (Integer) featureData.getStatistic((short) 3, (short) 4, "shipCount");
        float pd = (float) count / (float) totalCount;
        assertTrue(pd > 0.001);

        // Setup expectations
        final ArgumentCaptor<Analysis> analysisCaptor = ArgumentCaptor.forClass(Analysis.class);
        context.checking(new Expectations() {{
            oneOf(trackingService).registerSubscriber(with(analysisCaptor.getMatcher()));
            oneOf(featureDataRepository).getFeatureData("ShipTypeAndSizeFeature", testCellId); will(returnValue(featureData));
            ignoring(statisticsService).incAnalysisStatistics(with(ShipTypeAndSizeAnalysis.class.getSimpleName()), with(any(String.class)));
        }});

        // Create object under test
        final ShipTypeAndSizeAnalysis analysis = new ShipTypeAndSizeAnalysis(statisticsService, featureDataRepository, trackingService, eventRepository);

        // Perform test
        boolean isAbnormalEvent = analysis.isAbnormalCellForShipTypeAndSize(testCellId, (short) 3, (short) 4);

        // Assert results
        context.assertIsSatisfied();
        assertEquals(analysis, analysisCaptor.getCapturedObject());
        assertFalse(isAbnormalEvent);
    }
}
