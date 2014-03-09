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

package dk.dma.ais.abnormal.stat.statistics;

import dk.dma.ais.abnormal.stat.AppStatisticsService;
import dk.dma.ais.abnormal.stat.db.StatisticDataRepository;
import dk.dma.ais.abnormal.stat.db.data.ShipTypeAndSizeStatisticData;
import dk.dma.ais.abnormal.stat.db.data.StatisticData;
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

import java.util.HashMap;
import java.util.TreeMap;

import static dk.dma.ais.abnormal.stat.statistics.ShipTypeAndSizeStatistic.STATISTIC_NAME;
import static org.junit.Assert.assertEquals;

public class ShipTypeAndSizeStatisticTest {
    final JUnit4Mockery context = new JUnit4Mockery();

    TrackingService trackingService;
    AppStatisticsService statisticsService;
    StatisticDataRepository statisticsRepository;

    Track track;

    ShipTypeAndSizeStatistic statistic;

    @Before
    public void setup() {
        // Mock dependencies
        trackingService = context.mock(TrackingService.class);
        statisticsService = context.mock(AppStatisticsService.class);
        statisticsRepository = context.mock(StatisticDataRepository.class);

        // Setup test data
        track = new Track(1234567);
        track.setProperty(Track.CELL_ID, 5674365784L);
        track.setProperty(Track.SHIP_TYPE, 40);
        track.setProperty(Track.VESSEL_LENGTH, 75);

        statistic = new ShipTypeAndSizeStatistic(statisticsService, trackingService, statisticsRepository);
    }

    @Test
    public void testNewShipCountIsCreated() {
        Long oldCellId = null;
        CellChangedEvent event = new CellChangedEvent(track, oldCellId);

        // Setup expectations
        final ArgumentCaptor<StatisticData> statistics = ArgumentCaptor.forClass(StatisticData.class);
        context.checking(new Expectations() {{
            oneOf(trackingService).registerSubscriber(statistic);
            ignoring(statisticsService).incStatisticStatistics(with(STATISTIC_NAME), with(any(String.class)));

            oneOf(statisticsRepository).getStatisticData(with(STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID)));
            oneOf(statisticsRepository).putStatisticData(with(STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID)), with(statistics.getMatcher()));
        }});

        // Execute
        statistic.start();
        statistic.onCellIdChanged(event);

        // Assert expectations and captured values
        context.assertIsSatisfied();
        // TODO assertEquals(ShipTypeAndSizeStatistic.STATISTIC_NAME, statistics.getCapturedObject().getStatisticName());
        assertEquals(ShipTypeAndSizeStatisticData.class, statistics.getCapturedObject().getClass());
        ShipTypeAndSizeStatisticData capturedStatisticData = (ShipTypeAndSizeStatisticData) statistics.getCapturedObject();
        assertEquals("type", capturedStatisticData.getMeaningOfKey1());
        assertEquals("size", capturedStatisticData.getMeaningOfKey2());
        assertEquals(TreeMap.class, statistics.getCapturedObject().getData().getClass());
        TreeMap<Integer, TreeMap<Integer, HashMap<String,Integer>>> data = capturedStatisticData.getData();
        assertEquals(1, data.size()); // Assert one statistic recorded
        int shipType = data.firstKey();
        assertEquals(3, shipType);
        int shipSize = data.get(shipType).firstKey();
        assertEquals(3, shipSize);
        int numberOfStatsForShipTypeAndShipSize = data.get(shipType).get(shipSize).size();
        assertEquals(1, numberOfStatsForShipTypeAndShipSize);
        String statName = data.get(shipType).get(shipSize).keySet().iterator().next();
        assertEquals(ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT, statName);
        Object statValue = data.get(shipType).get(shipSize).get(statName);
        assertEquals(Integer.class, statValue.getClass());
        assertEquals(1, statValue);
    }

    @Test
    public void testExistingShipCountIsUpdated() {
        Long oldCellId = null;
        CellChangedEvent event = new CellChangedEvent(track, oldCellId);

        // Setup expectations
        final ShipTypeAndSizeStatisticData existingStatisticData = ShipTypeAndSizeStatisticData.create();
        existingStatisticData.setValue(3 - 1 /* -1 because idx counts from zero */, 3 - 1 /* -1 because idx counts from zero */, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT, 1);

        final ArgumentCaptor<StatisticData> statistics1 = ArgumentCaptor.forClass(StatisticData.class);
        final ArgumentCaptor<StatisticData> statistics2 = ArgumentCaptor.forClass(StatisticData.class);
        context.checking(new Expectations() {{
            oneOf(trackingService).registerSubscriber(statistic);
            ignoring(statisticsService).incStatisticStatistics(with(STATISTIC_NAME), with(any(String.class)));

            oneOf(statisticsRepository).getStatisticData(with(STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID))); will(returnValue(null));
            oneOf(statisticsRepository).putStatisticData(with(STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID)), with(statistics1.getMatcher()));

            oneOf(statisticsRepository).getStatisticData(with(STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID))); will(returnValue(existingStatisticData));
            oneOf(statisticsRepository).putStatisticData(with(STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID)), with(statistics2.getMatcher()));
        }});

        // Execute
        statistic.start();
        statistic.onCellIdChanged(event);
        statistic.onCellIdChanged(event);

        // Assert expectations and captured values
        context.assertIsSatisfied();
        // TODO assertEquals(ShipTypeAndSizeStatistic.STATISTIC_NAME, statistics2.getCapturedObject().getStatisticName());
        assertEquals(ShipTypeAndSizeStatisticData.class, statistics2.getCapturedObject().getClass());
        ShipTypeAndSizeStatisticData capturedStatisticData = (ShipTypeAndSizeStatisticData) statistics2.getCapturedObject();
        assertEquals("type", capturedStatisticData.getMeaningOfKey1());
        assertEquals("size", capturedStatisticData.getMeaningOfKey2());
        assertEquals(TreeMap.class, statistics2.getCapturedObject().getData().getClass());
        TreeMap<Integer, TreeMap<Integer, HashMap<String,Integer>>> data = capturedStatisticData.getData();
        assertEquals(1, data.size()); // Assert one statistic recorded
        int shipType = data.firstKey();
        assertEquals(3, shipType);
        int shipSize = data.get(shipType).firstKey();
        assertEquals(3, shipSize);
        int numberOfStatsForShipTypeAndShipSize = data.get(shipType).get(shipSize).size();
        assertEquals(1, numberOfStatsForShipTypeAndShipSize);
        String statName = data.get(shipType).get(shipSize).keySet().iterator().next();
        assertEquals(ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT, statName);
        Object statValue = data.get(shipType).get(shipSize).get(statName);
        assertEquals(2, statValue);
    }

    @Test
    public void testDoNotCountTracksWithSogLessThanTwo() {
        track.updatePosition(TrackingReport.create(1000L, Position.create(56, 12), 0.0f, 1.99f, false));

        Long oldCellId = null;
        CellChangedEvent event = new CellChangedEvent(track, oldCellId);

        context.checking(new Expectations() {{
            oneOf(trackingService).registerSubscriber(statistic);
            ignoring(statisticsService).incStatisticStatistics(with(STATISTIC_NAME), with(any(String.class)));
            never(statisticsRepository).getStatisticData(with(STATISTIC_NAME), with(any(Long.class)));
            never(statisticsRepository).putStatisticData(with(STATISTIC_NAME), (Long) with(any(Long.class)), with(any(StatisticData.class)));
        }});

        statistic.start();
        statistic.onCellIdChanged(event);

        // Assert expectations and captured values
        context.assertIsSatisfied();
    }

    @Test
    public void testCountTracksWithSogOverTwo() {
        track.updatePosition(TrackingReport.create(1000L, Position.create(56, 12), 0.0f, 2.01f, false));

        Long oldCellId = null;
        CellChangedEvent event = new CellChangedEvent(track, oldCellId);

        context.checking(new Expectations() {{
            oneOf(trackingService).registerSubscriber(statistic);
            ignoring(statisticsService).incStatisticStatistics(with(STATISTIC_NAME), with(any(String.class)));
            oneOf(statisticsRepository).getStatisticData(with(STATISTIC_NAME), with(any(Long.class)));
            oneOf(statisticsRepository).putStatisticData(with(STATISTIC_NAME), (Long) with(any(Long.class)), with(any(StatisticData.class)));
        }});

        statistic.start();
        statistic.onCellIdChanged(event);

        // Assert expectations and captured values
        context.assertIsSatisfied();
    }

    @Test
    public void testCountTracksWithNoSog() {
        //track.setProperty(Track.SPEED_OVER_GROUND, null);

        Long oldCellId = null;
        CellChangedEvent event = new CellChangedEvent(track, oldCellId);

        context.checking(new Expectations() {{
            oneOf(trackingService).registerSubscriber(statistic);
            ignoring(statisticsService).incStatisticStatistics(with(STATISTIC_NAME), with(any(String.class)));
            oneOf(statisticsRepository).getStatisticData(with(STATISTIC_NAME), with(any(Long.class)));
            oneOf(statisticsRepository).putStatisticData(with(STATISTIC_NAME), (Long) with(any(Long.class)), with(any(StatisticData.class)));
        }});

        statistic.start();
        statistic.onCellIdChanged(event);

        // Assert expectations and captured values
        context.assertIsSatisfied();
    }

}