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
import dk.dma.ais.abnormal.tracker.Tracker;
import dk.dma.ais.abnormal.tracker.events.CellChangedEvent;
import dk.dma.ais.packet.AisPacket;
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

    Tracker trackingService;
    AppStatisticsService statisticsService;
    StatisticDataRepository statisticsRepository;

    Track track;

    ShipTypeAndSizeStatistic statistic;

    final String[] NMEA_TEST_STRINGS = {
        // GatehouseSourceTag [baseMmsi=2190067, country=DK, region=, timestamp=Thu Apr 10 15:30:28 CEST 2014]
        // [msgId=1, repeat=0, userId=219000606, cog=2010, navStatus=0, pos=(33024811,6011092) = (33024811,6011092), posAcc=1, raim=0, specialManIndicator=0, rot=0, sog=108, spare=0, syncState=1, trueHeading=200, utcSec=60, slotTimeout=6, subMessage=1063]
        "$PGHP,1,2014,4,10,13,30,28,385,219,,2190067,1,12*26\r\n" +
        "!BSVDM,1,1,,A,13@ng7P01dPeo6`OOc:onVAp0p@W,0*12",

        // GatehouseSourceTag [baseMmsi=2190067, country=DK, region=, timestamp=Thu Apr 10 15:30:29 CEST 2014]
        // [msgId=5, repeat=0, userId=219000606, callsign=OWNM@@@, dest=BOEJDEN-FYNSHAV@@@@@, dimBow=12, dimPort=8, dimStarboard=4, dimStern=58, draught=30, dte=0, eta=67584, imo=8222824, name=FRIGG SYDFYEN@@@@@@@, posType=1, shipType=61, spare=0, version=0]
        "$PGHP,1,2014,4,10,13,30,29,165,219,,2190067,1,28*22\r\n" +
        "!BSVDM,2,1,1,A,53@ng7P1uN6PuLpl000I8TLN1=T@ITDp0000000u1Pr844@P07PSiBQ1,0*7B\r\n" +
        "!BSVDM,2,2,1,A,CcAVCTj0EP00000,2*53"
    };

    final AisPacket[] packets = { AisPacket.from(NMEA_TEST_STRINGS[0]), AisPacket.from(NMEA_TEST_STRINGS[1])};

    @Before
    public void setup() {
        // Mock dependencies
        trackingService = context.mock(Tracker.class);
        statisticsService = context.mock(AppStatisticsService.class);
        statisticsRepository = context.mock(StatisticDataRepository.class);

        // Setup test data
        track = new Track(219000606);
        track.update(packets[0]);
        track.update(packets[1]);
        track.setProperty(Track.CELL_ID, 5674365784L);

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
        track.update(System.currentTimeMillis(), Position.create(56, 12), 0.0f, 1.99f, 1.99f);

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
        track.update(System.currentTimeMillis(), Position.create(56, 12), 0.0f, 2.01f, 2.01f);

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