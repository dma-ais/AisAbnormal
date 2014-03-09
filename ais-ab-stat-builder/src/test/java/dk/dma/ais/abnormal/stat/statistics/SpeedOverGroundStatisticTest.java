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
import dk.dma.ais.abnormal.stat.db.data.SpeedOverGroundStatisticData;
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

import static org.junit.Assert.assertEquals;

public class SpeedOverGroundStatisticTest {
    final JUnit4Mockery context = new JUnit4Mockery();

    TrackingService trackingService;
    AppStatisticsService statisticsService;
    StatisticDataRepository statisticsRepository;

    Track track;
    CellChangedEvent event;

    SpeedOverGroundStatistic statistic;

    @Before
    public void beforeTest() {
        // Mock dependencies
        trackingService = context.mock(TrackingService.class);
        statisticsService = context.mock(AppStatisticsService.class);
        statisticsRepository = context.mock(StatisticDataRepository.class);

        // Setup test data
        track = new Track(1234567);
        track.setProperty(Track.CELL_ID, 5674365784L);
        track.setProperty(Track.SHIP_TYPE, 40);     /* bucket 3 */
        track.setProperty(Track.VESSEL_LENGTH, 75); /* bucket 3 */
        track.updatePosition(TrackingReport.create(1000L, Position.create(56, 12), 0.0f, 15.0f /* bucket 5 */, false));
        event = new CellChangedEvent(track, null);

        statistic = new SpeedOverGroundStatistic(statisticsService, trackingService, statisticsRepository);
    }

    @Test
    public void testNewShipCountIsCreated() {
        // Setup expectations
        final ArgumentCaptor<StatisticData> statistics = ArgumentCaptor.forClass(StatisticData.class);
        context.checking(new Expectations() {{
            oneOf(trackingService).registerSubscriber(statistic);
            ignoring(statisticsService).incStatisticStatistics(with(SpeedOverGroundStatistic.STATISTIC_NAME), with(any(String.class)));

            oneOf(statisticsRepository).getStatisticData(with(SpeedOverGroundStatistic.STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID)));
            oneOf(statisticsRepository).putStatisticData(with(SpeedOverGroundStatistic.STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID)), with(statistics.getMatcher()));
        }});

        // Execute
        statistic.start();
        statistic.onCellIdChanged(event);

        // Main assertations
        SpeedOverGroundStatisticData capturedStatisticData = (SpeedOverGroundStatisticData) statistics.getCapturedObject();

        context.assertIsSatisfied();

        TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, HashMap<String, Integer>>>> data = capturedStatisticData.getData();

        assertEquals(1, data.size()); // Assert one statistic recorded
        int shipTypeBucket = data.firstKey();
        assertEquals(3, shipTypeBucket);
        int shipSizeBucket = data.get(shipTypeBucket).firstKey();
        assertEquals(3, shipSizeBucket);
        int sogBucket = data.get(shipTypeBucket).get(shipSizeBucket).firstKey();
        assertEquals(5, sogBucket);

        int numberOfStats = data.get(shipTypeBucket).get(shipSizeBucket).get(sogBucket).size();
        assertEquals(1, numberOfStats);
        String statName = data.get(shipTypeBucket).get(shipSizeBucket).get(sogBucket).keySet().iterator().next();
        Object statValue = data.get(shipTypeBucket).get(shipSizeBucket).get(sogBucket).get(statName);
        assertEquals(Integer.class, statValue.getClass());
        assertEquals(1, statValue);

        // Other assertations now we're here
        assertEquals(SpeedOverGroundStatisticData.class, statistics.getCapturedObject().getClass());
        assertEquals("type", capturedStatisticData.getMeaningOfKey1());
        assertEquals("size", capturedStatisticData.getMeaningOfKey2());
        assertEquals(TreeMap.class, statistics.getCapturedObject().getData().getClass());
        assertEquals(SpeedOverGroundStatisticData.STAT_SHIP_COUNT, statName);
    }

    @Test
    public void testExistingShipCountIsUpdated() {
        final SpeedOverGroundStatisticData existingStatisticData = SpeedOverGroundStatisticData.create();
        existingStatisticData.setValue(3 - 1 /* -1 because idx counts from zero */, 3 - 1 /* -1 because idx counts from zero */, 5 - 1 /* -1 because idx counts from zero */, SpeedOverGroundStatisticData.STAT_SHIP_COUNT, 1);

        final ArgumentCaptor<StatisticData> statistics1 = ArgumentCaptor.forClass(StatisticData.class);
        final ArgumentCaptor<StatisticData> statistics2 = ArgumentCaptor.forClass(StatisticData.class);
        context.checking(new Expectations() {{
            oneOf(trackingService).registerSubscriber(statistic);
            ignoring(statisticsService).incStatisticStatistics(with(SpeedOverGroundStatistic.STATISTIC_NAME), with(any(String.class)));

            oneOf(statisticsRepository).getStatisticData(with(SpeedOverGroundStatistic.STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID)));
            will(returnValue(null));
            oneOf(statisticsRepository).putStatisticData(with(SpeedOverGroundStatistic.STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID)), with(statistics1.getMatcher()));

            oneOf(statisticsRepository).getStatisticData(with(SpeedOverGroundStatistic.STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID)));
            will(returnValue(existingStatisticData));
            oneOf(statisticsRepository).putStatisticData(with(SpeedOverGroundStatistic.STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID)), with(statistics2.getMatcher()));
        }});

        // Execute
        statistic.start();
        statistic.onCellIdChanged(event);
        statistic.onCellIdChanged(event);

        // Assert expectations and captured values
        context.assertIsSatisfied();
        // TODO assertEquals(SpeedOverGroundStatistic.STATISTIC_NAME, statistics2.getCapturedObject().getStatisticName());
        assertEquals(SpeedOverGroundStatisticData.class, statistics2.getCapturedObject().getClass());
        SpeedOverGroundStatisticData capturedStatisticData = (SpeedOverGroundStatisticData) statistics2.getCapturedObject();
        assertEquals("type", capturedStatisticData.getMeaningOfKey1());
        assertEquals("size", capturedStatisticData.getMeaningOfKey2());
        assertEquals(TreeMap.class, statistics2.getCapturedObject().getData().getClass());
        TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, HashMap<String, Integer>>>> data = capturedStatisticData.getData();
        assertEquals(1, data.size()); // Assert one statistic recorded
        int shipTypeBucket = data.firstKey();
        assertEquals(3, shipTypeBucket);
        int shipSizeBucket = data.get(shipTypeBucket).firstKey();
        assertEquals(3, shipSizeBucket);
        int sogBucket = data.get(shipTypeBucket).get(shipSizeBucket).firstKey();
        assertEquals(5, sogBucket);
        int numberOfStatsForShipTypeAndShipSize = data.get(shipTypeBucket).get(shipSizeBucket).size();
        assertEquals(1, numberOfStatsForShipTypeAndShipSize);
        String statName = data.get(shipTypeBucket).get(shipSizeBucket).get(sogBucket).keySet().iterator().next();
        assertEquals(SpeedOverGroundStatisticData.STAT_SHIP_COUNT, statName);
        Object statValue = data.get(shipTypeBucket).get(shipSizeBucket).get(sogBucket).get(statName);
        assertEquals(2, statValue);
    }
}
