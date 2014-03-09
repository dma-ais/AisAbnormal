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
import dk.dma.ais.abnormal.event.db.domain.ShipSizeOrTypeEvent;
import dk.dma.ais.abnormal.stat.db.StatisticDataRepository;
import dk.dma.ais.abnormal.stat.db.data.ShipTypeAndSizeStatisticData;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingReport;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.abnormal.tracker.events.CellChangedEvent;
import dk.dma.ais.test.helpers.ArgumentCaptor;
import dk.dma.enav.model.geometry.Position;
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
    private StatisticDataRepository statisticsRepository;
    private EventRepository eventRepository;
    private ShipTypeAndSizeStatisticData statistics;
    private BehaviourManager behaviourManager;

    @Before
    public void prepareTest() {
        context = new JUnit4Mockery();

        // Mock dependencies
        trackingService = context.mock(TrackingService.class);
        statisticsService = context.mock(AppStatisticsService.class);
        statisticsRepository = context.mock(StatisticDataRepository.class);
        eventRepository = context.mock(EventRepository.class);
        behaviourManager = context.mock(BehaviourManager.class);

        // Mock shipCount table
        statistics = ShipTypeAndSizeStatisticData.create();
        statistics.setValue((short) 2, (short) 0, "shipCount", 17);
        statistics.setValue((short) 2, (short) 1, "shipCount", 2);
        statistics.setValue((short) 2, (short) 2, "shipCount", 87);
        statistics.setValue((short) 2, (short) 3, "shipCount", 618);
        statistics.setValue((short) 3, (short) 2, "shipCount", 842);
        statistics.setValue((short) 3, (short) 3, "shipCount", 954);
        statistics.setValue((short) 4, (short) 2, "shipCount", 154);
        statistics.setValue((short) 5, (short) 3, "shipCount", 34);
    }

    @Test
    public void abnormalWhereNoShipCountStatistics() {
        // Assert that pre-conditions are as expected
        assertNull(statistics.getValue((short) 1, (short) 1, "shipCount"));

        // Setup expectations
        final ArgumentCaptor<Analysis> analysisCaptor = ArgumentCaptor.forClass(Analysis.class);
        context.checking(new Expectations() {{
            oneOf(behaviourManager).registerSubscriber(with(any(ShipTypeAndSizeAnalysis.class)));
            oneOf(trackingService).registerSubscriber(with(analysisCaptor.getMatcher()));
            oneOf(statisticsRepository).getStatisticData("ShipTypeAndSizeStatistic", testCellId); will(returnValue(statistics));
            ignoring(statisticsService).incAnalysisStatistics(with(ShipTypeAndSizeAnalysis.class.getSimpleName()), with(any(String.class)));
        }});

        // Create object under test
        final ShipTypeAndSizeAnalysis analysis = new ShipTypeAndSizeAnalysis(statisticsService, statisticsRepository, trackingService, eventRepository, behaviourManager);
        analysis.start();

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
        Integer totalCount = statistics.getSumFor("shipCount");
        Integer count = statistics.getValue((short) 2, (short) 1, "shipCount");
        float pd = (float) count / (float) totalCount;
        assertTrue(pd < 0.001);

        // Setup expectations
        final ArgumentCaptor<Analysis> analysisCaptor = ArgumentCaptor.forClass(Analysis.class);
        context.checking(new Expectations() {{
            oneOf(behaviourManager).registerSubscriber(with(any(ShipTypeAndSizeAnalysis.class)));
            oneOf(trackingService).registerSubscriber(with(analysisCaptor.getMatcher()));
            oneOf(statisticsRepository).getStatisticData("ShipTypeAndSizeStatistic", testCellId); will(returnValue(statistics));
            ignoring(statisticsService).incAnalysisStatistics(with(ShipTypeAndSizeAnalysis.class.getSimpleName()), with(any(String.class)));
        }});

        // Create object under test
        final ShipTypeAndSizeAnalysis analysis = new ShipTypeAndSizeAnalysis(statisticsService, statisticsRepository, trackingService, eventRepository, behaviourManager);
        analysis.start();

        // Perform test
        boolean isAbnormalEvent = analysis.isAbnormalCellForShipTypeAndSize(testCellId, (short) 2, (short) 1);

        // Assert results
        context.assertIsSatisfied();
        assertEquals(analysis, analysisCaptor.getCapturedObject());
        assertTrue(isAbnormalEvent);
    }

    @Test
    public void normalWhereHighShipCountStatistics() {
        // Assert that pre-conditions are as expected
        Integer totalCount = statistics.getSumFor("shipCount");
        Integer count = statistics.getValue((short) 2, (short) 3, "shipCount");
        float pd = (float) count / (float) totalCount;
        assertTrue(pd > 0.001);

        // Setup expectations
        final ArgumentCaptor<Analysis> analysisCaptor = ArgumentCaptor.forClass(Analysis.class);
        context.checking(new Expectations() {{
            oneOf(behaviourManager).registerSubscriber(with(any(ShipTypeAndSizeAnalysis.class)));
            oneOf(trackingService).registerSubscriber(with(analysisCaptor.getMatcher()));
            oneOf(statisticsRepository).getStatisticData("ShipTypeAndSizeStatistic", testCellId); will(returnValue(statistics));
            ignoring(statisticsService).incAnalysisStatistics(with(ShipTypeAndSizeAnalysis.class.getSimpleName()), with(any(String.class)));
        }});

        // Create object under test
        final ShipTypeAndSizeAnalysis analysis = new ShipTypeAndSizeAnalysis(statisticsService, statisticsRepository, trackingService, eventRepository, behaviourManager);
        analysis.start();

        // Perform test
        boolean isAbnormalEvent = analysis.isAbnormalCellForShipTypeAndSize(testCellId, (short) 2, (short) 3);

        // Assert results
        context.assertIsSatisfied();
        assertEquals(analysis, analysisCaptor.getCapturedObject());
        assertFalse(isAbnormalEvent);
    }

    /**
     * No analysis will take place until all of cellId, shipType and shipLength are contained
     * in the tracking event.
     */
    @Test
    public void testNoAnalysisDoneForInsufficientData() {
        // Create test data
        Track track = new Track(123456);
        CellChangedEvent event = new CellChangedEvent(track, null);

        // Create object under test
        context.checking(new Expectations() {{
            oneOf(behaviourManager).registerSubscriber(with(any(ShipTypeAndSizeAnalysis.class)));
        }});
        final ShipTypeAndSizeAnalysis analysis = new ShipTypeAndSizeAnalysis(statisticsService, statisticsRepository, trackingService, eventRepository, behaviourManager);

        // Perform test - none of the required data are there
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(ShipTypeAndSizeAnalysis.class.getSimpleName()), with(any(String.class)));
            oneOf(trackingService).registerSubscriber(analysis);
        }});
        analysis.start();
        analysis.onCellIdChanged(event);
        context.assertIsSatisfied();

        // Repeat test - with cellId added
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(ShipTypeAndSizeAnalysis.class.getSimpleName()), with(any(String.class)));
        }});
        event.getTrack().setProperty(Track.CELL_ID, 123L);
        analysis.onCellIdChanged(event);
        context.assertIsSatisfied();

        // Repeat test - with ship type added
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(ShipTypeAndSizeAnalysis.class.getSimpleName()), with(any(String.class)));
        }});
        event.getTrack().setProperty(Track.SHIP_TYPE, 3);
        analysis.onCellIdChanged(event);
        context.assertIsSatisfied();

        // Repeat test - with ship length added
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(ShipTypeAndSizeAnalysis.class.getSimpleName()), with(any(String.class)));
            oneOf(statisticsRepository).getStatisticData("ShipTypeAndSizeStatistic", 123L);
            oneOf(behaviourManager).normalBehaviourDetected(ShipSizeOrTypeEvent.class, track);
        }});
        event.getTrack().setProperty(Track.VESSEL_LENGTH, 4);
        analysis.onCellIdChanged(event);
        context.assertIsSatisfied();
    }

    /**
     * Abnormal event raised.
     */
    @Test
    public void testEventIsRaisedForAbnormalBehaviour() {
        // Create test data
        Track track = new Track(123456);
        track.setProperty(Track.CELL_ID, 123L);
        track.setProperty(Track.SHIP_TYPE, 40);
        track.setProperty(Track.VESSEL_LENGTH, 15);

        // These are needed to create an event object in the database:
        track.updatePosition(TrackingReport.create(1370589743L, Position.create(56, 12), 45.0f, 10.1f, false));

        CellChangedEvent event = new CellChangedEvent(track, null);

        // Create object under test
        context.checking(new Expectations() {{
            oneOf(behaviourManager).registerSubscriber(with(any(ShipTypeAndSizeAnalysis.class)));
        }});
        final ShipTypeAndSizeAnalysis analysis = new ShipTypeAndSizeAnalysis(statisticsService, statisticsRepository, trackingService, eventRepository, behaviourManager);

        // Perform test - none of the required data are there
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(ShipTypeAndSizeAnalysis.class.getSimpleName()), with(any(String.class)));
            oneOf(trackingService).registerSubscriber(analysis);
            oneOf(statisticsRepository).getStatisticData("ShipTypeAndSizeStatistic", 123L); will(returnValue(statistics));
            oneOf(behaviourManager).abnormalBehaviourDetected(ShipSizeOrTypeEvent.class, track);
        }});
        analysis.start();
        analysis.onCellIdChanged(event);
        context.assertIsSatisfied();
    }

    /**
     * No event is raised for normal behaviour.
     */
    @Test
    public void testNoEventIsRaisedForNormalBehaviour() {
        // Create test data
        Track track = new Track(123456);
        track.setProperty(Track.CELL_ID, 123L);
        track.setProperty(Track.SHIP_TYPE, 40);
        track.setProperty(Track.VESSEL_LENGTH, 150);

        // These are needed to create an event object in the database:
        track.updatePosition(TrackingReport.create(1370589743L, Position.create(56, 12), 45.0f, 10.1f, false));

        CellChangedEvent event = new CellChangedEvent(track, null);

        // Create object under test
        context.checking(new Expectations() {{
            oneOf(behaviourManager).registerSubscriber(with(any(ShipTypeAndSizeAnalysis.class)));
        }});
        final ShipTypeAndSizeAnalysis analysis = new ShipTypeAndSizeAnalysis(statisticsService, statisticsRepository, trackingService, eventRepository, behaviourManager);

        // Perform test - none of the required data are there
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(ShipTypeAndSizeAnalysis.class.getSimpleName()), with(any(String.class)));
            oneOf(trackingService).registerSubscriber(analysis);
            oneOf(statisticsRepository).getStatisticData("ShipTypeAndSizeStatistic", 123L); will(returnValue(statistics));
            oneOf(behaviourManager).normalBehaviourDetected(ShipSizeOrTypeEvent.class, track);
        }});
        analysis.start();
        analysis.onCellIdChanged(event);
        context.assertIsSatisfied();
    }

}
