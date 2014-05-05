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
import dk.dma.ais.abnormal.tracker.Tracker;
import dk.dma.ais.abnormal.tracker.events.CellChangedEvent;
import dk.dma.ais.abnormal.util.Categorizer;
import dk.dma.ais.packet.AisPacket;
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

    // GatehouseSourceTag [baseMmsi=2190067, country=DK, region=, timestamp=Thu Apr 10 15:30:29 CEST 2014]
    // [msgId=5, repeat=0, userId=219000606, callsign=OWNM@@@, dest=BOEJDEN-FYNSHAV@@@@@, dimBow=12, dimPort=8, dimStarboard=4, dimStern=58, draught=30, dte=0, eta=67584, imo=8222824, name=FRIGG SYDFYEN@@@@@@@, posType=1, shipType=61, spare=0, version=0]
    final AisPacket msg5 = AisPacket.from(
        "$PGHP,1,2014,4,10,13,30,29,165,219,,2190067,1,28*22\r\n" +
        "!BSVDM,2,1,1,A,53@ng7P1uN6PuLpl000I8TLN1=T@ITDp0000000u1Pr844@P07PSiBQ1,0*7B\r\n" +
        "!BSVDM,2,2,1,A,CcAVCTj0EP00000,2*53");

    private Tracker trackingService;
    private AppStatisticsService statisticsService;
    private StatisticDataRepository statisticsRepository;
    private EventRepository eventRepository;
    private ShipTypeAndSizeStatisticData statistics1, statistics2;
    private BehaviourManager behaviourManager;

    @Before
    public void prepareTest() {
        context = new JUnit4Mockery();

        // Mock dependencies
        trackingService = context.mock(Tracker.class);
        statisticsService = context.mock(AppStatisticsService.class);
        statisticsRepository = context.mock(StatisticDataRepository.class);
        eventRepository = context.mock(EventRepository.class);
        behaviourManager = context.mock(BehaviourManager.class);

        // Mock shipCount table
        statistics1 = ShipTypeAndSizeStatisticData.create();
        statistics1.setValue((short) 2, (short) 0, "shipCount", 17);
        statistics1.setValue((short) 2, (short) 1, "shipCount", 87);
        statistics1.setValue((short) 2, (short) 2, "shipCount", 2);
        statistics1.setValue((short) 2, (short) 3, "shipCount", 618);
        statistics1.setValue((short) 3, (short) 2, "shipCount", 842);
        statistics1.setValue((short) 3, (short) 3, "shipCount", 954);
        statistics1.setValue((short) 4, (short) 2, "shipCount", 154);
        statistics1.setValue((short) 5, (short) 3, "shipCount", 34);

        // Mock shipCount table
        statistics2 = ShipTypeAndSizeStatisticData.create();
        statistics2.setValue((short) 2, (short) 0, "shipCount", 17);
        statistics2.setValue((short) 2, (short) 1, "shipCount", 87);
        statistics2.setValue((short) 2, (short) 2, "shipCount", 2000);
        statistics2.setValue((short) 2, (short) 3, "shipCount", 618);
        statistics2.setValue((short) 3, (short) 2, "shipCount", 842);
        statistics2.setValue((short) 3, (short) 3, "shipCount", 954);
        statistics2.setValue((short) 4, (short) 2, "shipCount", 154);
        statistics2.setValue((short) 5, (short) 3, "shipCount", 34);
    }

    @Test
    public void abnormalWhereNoShipCountStatistics() {
        // Assert that pre-conditions are as expected
        assertNull(statistics1.getValue((short) 1, (short) 1, "shipCount"));

        // Setup expectations
        final ArgumentCaptor<Analysis> analysisCaptor = ArgumentCaptor.forClass(Analysis.class);
        context.checking(new Expectations() {{
            oneOf(behaviourManager).registerSubscriber(with(any(ShipTypeAndSizeAnalysis.class)));
            oneOf(trackingService).registerSubscriber(with(analysisCaptor.getMatcher()));
            oneOf(statisticsRepository).getStatisticData("ShipTypeAndSizeStatistic", testCellId); will(returnValue(statistics1));
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
        Integer totalCount = statistics1.getSumFor("shipCount");
        Integer count = statistics1.getValue((short) 2, (short) 2, "shipCount");
        float pd = (float) count / (float) totalCount;
        assertTrue(pd < 0.001);

        // Setup expectations
        final ArgumentCaptor<Analysis> analysisCaptor = ArgumentCaptor.forClass(Analysis.class);
        context.checking(new Expectations() {{
            oneOf(behaviourManager).registerSubscriber(with(any(ShipTypeAndSizeAnalysis.class)));
            oneOf(trackingService).registerSubscriber(with(analysisCaptor.getMatcher()));
            oneOf(statisticsRepository).getStatisticData("ShipTypeAndSizeStatistic", testCellId); will(returnValue(statistics1));
            ignoring(statisticsService).incAnalysisStatistics(with(ShipTypeAndSizeAnalysis.class.getSimpleName()), with(any(String.class)));
        }});

        // Create object under test
        final ShipTypeAndSizeAnalysis analysis = new ShipTypeAndSizeAnalysis(statisticsService, statisticsRepository, trackingService, eventRepository, behaviourManager);
        analysis.start();

        // Perform test
        boolean isAbnormalEvent = analysis.isAbnormalCellForShipTypeAndSize(testCellId, (short) 2, (short) 2);

        // Assert results
        context.assertIsSatisfied();
        assertEquals(analysis, analysisCaptor.getCapturedObject());
        assertTrue(isAbnormalEvent);
    }

    @Test
    public void normalWhereHighShipCountStatistics() {
        // Assert that pre-conditions are as expected
        Integer totalCount = statistics1.getSumFor("shipCount");
        Integer count = statistics1.getValue((short) 2, (short) 3, "shipCount");
        float pd = (float) count / (float) totalCount;
        assertTrue(pd > 0.001);

        // Setup expectations
        final ArgumentCaptor<Analysis> analysisCaptor = ArgumentCaptor.forClass(Analysis.class);
        context.checking(new Expectations() {{
            oneOf(behaviourManager).registerSubscriber(with(any(ShipTypeAndSizeAnalysis.class)));
            oneOf(trackingService).registerSubscriber(with(analysisCaptor.getMatcher()));
            oneOf(statisticsRepository).getStatisticData("ShipTypeAndSizeStatistic", testCellId); will(returnValue(statistics1));
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
        Track track = new Track(219000606);
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

        // Repeat test - with ship type and length added
        context.checking(new Expectations() {{
            ignoring(statisticsService).incAnalysisStatistics(with(ShipTypeAndSizeAnalysis.class.getSimpleName()), with(any(String.class)));
            oneOf(statisticsRepository).getStatisticData("ShipTypeAndSizeStatistic", 123L);
            oneOf(behaviourManager).normalBehaviourDetected(ShipSizeOrTypeEvent.class, track);
        }});
        track.update(msg5); // add shiptype and vessel length
        analysis.onCellIdChanged(event);
        context.assertIsSatisfied();
    }

    /**
     * Abnormal event raised.
     */
    @Test
    public void testEventIsRaisedForAbnormalBehaviour() {
        // Create test data
        Track track = new Track(219000606);
        track.setProperty(Track.CELL_ID, 123L);
        track.update(msg5);

        assertEquals(61, track.getShipType().intValue());
        assertEquals(70, track.getVesselLength().intValue());
        assertTrue(statistics1.getValue(Categorizer.mapShipTypeToCategory(track.getShipType()) - 1, Categorizer.mapShipLengthToCategory(track.getVesselLength()) - 1, "shipCount") < 3);

        // These are needed to create an event object in the database:
        track.update(System.currentTimeMillis(), Position.create(56, 12), 45.0f, 10.1f, 45.0f);

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
            oneOf(statisticsRepository).getStatisticData("ShipTypeAndSizeStatistic", 123L); will(returnValue(statistics1));
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
        Track track = new Track(219000606);
        track.setProperty(Track.CELL_ID, 123L);
        track.update(msg5);

        assertEquals(61, track.getShipType().intValue());
        assertEquals(70, track.getVesselLength().intValue());
        assertTrue(statistics2.getValue(Categorizer.mapShipTypeToCategory(track.getShipType()) - 1, Categorizer.mapShipLengthToCategory(track.getVesselLength()) - 1, "shipCount") > 1000);

        // These are needed to create an event object in the database:
        track.update(1370589743L, Position.create(56, 12), 45.0f, 10.1f, 45.0f);

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
            oneOf(statisticsRepository).getStatisticData("ShipTypeAndSizeStatistic", 123L); will(returnValue(statistics2));
            oneOf(behaviourManager).normalBehaviourDetected(ShipSizeOrTypeEvent.class, track);
        }});
        analysis.start();
        analysis.onCellIdChanged(event);
        context.assertIsSatisfied();
    }

}
