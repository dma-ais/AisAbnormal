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
import dk.dma.ais.abnormal.event.db.domain.AbnormalShipSizeOrTypeEvent;
import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import dk.dma.ais.abnormal.stat.db.data.ShipTypeAndSizeData;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.abnormal.tracker.events.CellIdChangedEvent;
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
        analysis.start();

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
        analysis.start();

        // Perform test
        boolean isAbnormalEvent = analysis.isAbnormalCellForShipTypeAndSize(testCellId, (short) 3, (short) 4);

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
        CellIdChangedEvent event = new CellIdChangedEvent(track, null);

        // Create object under test
        final ShipTypeAndSizeAnalysis analysis = new ShipTypeAndSizeAnalysis(statisticsService, featureDataRepository, trackingService, eventRepository);

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
            oneOf(featureDataRepository).getFeatureData("ShipTypeAndSizeFeature", 123L);
            oneOf(eventRepository).findOngoingEventByVessel(with(123456), with(AbnormalShipSizeOrTypeEvent.class));
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
        track.setProperty(Track.TIMESTAMP_POSITION_UPDATE, 1370589743L);
        track.setProperty(Track.POSITION_IS_INTERPOLATED, false);
        track.setProperty(Track.POSITION, Position.create(56, 12));

        CellIdChangedEvent event = new CellIdChangedEvent(track, null);

        // Create object under test
        final ShipTypeAndSizeAnalysis analysis = new ShipTypeAndSizeAnalysis(statisticsService, featureDataRepository, trackingService, eventRepository);

        // Perform test - none of the required data are there
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(ShipTypeAndSizeAnalysis.class.getSimpleName()), with(any(String.class)));
            oneOf(trackingService).registerSubscriber(analysis);
            oneOf(featureDataRepository).getFeatureData("ShipTypeAndSizeFeature", 123L); will(returnValue(featureData));
            oneOf(eventRepository).findOngoingEventByVessel(with(123456), with(AbnormalShipSizeOrTypeEvent.class)); will(returnValue(null));
            oneOf(eventRepository).save(with(any(AbnormalShipSizeOrTypeEvent.class)));
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
        track.setProperty(Track.TIMESTAMP_POSITION_UPDATE, 1370589743L);
        track.setProperty(Track.POSITION_IS_INTERPOLATED, false);
        track.setProperty(Track.POSITION, Position.create(56, 12));

        CellIdChangedEvent event = new CellIdChangedEvent(track, null);

        // Create object under test
        final ShipTypeAndSizeAnalysis analysis = new ShipTypeAndSizeAnalysis(statisticsService, featureDataRepository, trackingService, eventRepository);

        // Perform test - none of the required data are there
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(ShipTypeAndSizeAnalysis.class.getSimpleName()), with(any(String.class)));
            oneOf(trackingService).registerSubscriber(analysis);
            oneOf(featureDataRepository).getFeatureData("ShipTypeAndSizeFeature", 123L); will(returnValue(featureData));
            oneOf(eventRepository).findOngoingEventByVessel(with(123456), with(AbnormalShipSizeOrTypeEvent.class)); will(returnValue(null));
            never(eventRepository).save(with(any(AbnormalShipSizeOrTypeEvent.class)));
        }});
        analysis.start();
        analysis.onCellIdChanged(event);
        context.assertIsSatisfied();
    }

}
