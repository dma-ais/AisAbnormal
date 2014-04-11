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
import dk.dma.ais.abnormal.stat.db.data.CourseOverGroundStatisticData;
import dk.dma.ais.abnormal.stat.db.data.StatisticData;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.Tracker;
import dk.dma.ais.abnormal.tracker.events.CellChangedEvent;
import dk.dma.ais.abnormal.util.Categorizer;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.test.helpers.ArgumentCaptor;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

public class CourseOverGroundStatisticTest {
    final JUnit4Mockery context = new JUnit4Mockery();

    Tracker trackingService;
    AppStatisticsService statisticsService;
    StatisticDataRepository statisticsRepository;

    Track track;
    CellChangedEvent event;

    CourseOverGroundStatistic statistic;

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
    public void beforeTest() {
        // Mock dependencies
        trackingService = context.mock(Tracker.class);
        statisticsService = context.mock(AppStatisticsService.class);
        statisticsRepository = context.mock(StatisticDataRepository.class);

        // Setup test data
        track = new Track(219000606);
        track.setProperty(Track.CELL_ID, 5674365784L);
        track.update(packets[0]);
        track.update(packets[1]);

        event = new CellChangedEvent(track, null);

        statistic = new CourseOverGroundStatistic(statisticsService, trackingService, statisticsRepository);

        assertEquals(3, Categorizer.mapShipTypeToCategory(track.getShipType()));
        assertEquals(7, Categorizer.mapCourseOverGroundToCategory(track.getCourseOverGround()));
    }

    @Test
    public void testNewShipCountIsCreated() {
        // Setup expectations
        final ArgumentCaptor<StatisticData> statistics = ArgumentCaptor.forClass(StatisticData.class);
        context.checking(new Expectations() {{
            oneOf(trackingService).registerSubscriber(statistic);
            ignoring(statisticsService).incStatisticStatistics(with(CourseOverGroundStatistic.STATISTIC_NAME), with(any(String.class)));

            oneOf(statisticsRepository).getStatisticData(with(CourseOverGroundStatistic.STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID)));
            oneOf(statisticsRepository).putStatisticData(with(CourseOverGroundStatistic.STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID)), with(statistics.getMatcher()));
        }});

        // Execute
        statistic.start();
        statistic.onCellIdChanged(event);

        // Main assertations
        CourseOverGroundStatisticData capturedStatisticData = (CourseOverGroundStatisticData) statistics.getCapturedObject();

        context.assertIsSatisfied();

        TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, HashMap<String, Integer>>>> data = capturedStatisticData.getData();

        assertEquals(1, data.size()); // Assert one statistic recorded
        int shipTypeBucket = data.firstKey();
        assertEquals(3, shipTypeBucket);
        int shipSizeBucket = data.get(shipTypeBucket).firstKey();
        assertEquals(3, shipSizeBucket);
        int cogBucket = data.get(shipTypeBucket).get(shipSizeBucket).firstKey();
        assertEquals(7, cogBucket);

        int numberOfStats = data.get(shipTypeBucket).get(shipSizeBucket).get(cogBucket).size();
        assertEquals(1, numberOfStats);
        String statName = data.get(shipTypeBucket).get(shipSizeBucket).get(cogBucket).keySet().iterator().next();
        Object statValue = data.get(shipTypeBucket).get(shipSizeBucket).get(cogBucket).get(statName);
        assertEquals(Integer.class, statValue.getClass());
        assertEquals(1, statValue);

        // Other assertations now we're here
        assertEquals(CourseOverGroundStatisticData.class, statistics.getCapturedObject().getClass());
        assertEquals("type", capturedStatisticData.getMeaningOfKey1());
        assertEquals("size", capturedStatisticData.getMeaningOfKey2());
        assertEquals(TreeMap.class, statistics.getCapturedObject().getData().getClass());
        assertEquals(CourseOverGroundStatisticData.STAT_SHIP_COUNT, statName);
    }

    @Test
    public void testExistingShipCountIsUpdated() {
        final CourseOverGroundStatisticData existingStatisticData = CourseOverGroundStatisticData.create();
        existingStatisticData.setValue(3-1, 3-1, 7-1, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 1);

        final ArgumentCaptor<StatisticData> statistics1 = ArgumentCaptor.forClass(StatisticData.class);
        final ArgumentCaptor<StatisticData> statistics2 = ArgumentCaptor.forClass(StatisticData.class);
        context.checking(new Expectations() {{
            oneOf(trackingService).registerSubscriber(statistic);
            ignoring(statisticsService).incStatisticStatistics(with(CourseOverGroundStatistic.STATISTIC_NAME), with(any(String.class)));

            oneOf(statisticsRepository).getStatisticData(with(CourseOverGroundStatistic.STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID)));
            will(returnValue(null));
            oneOf(statisticsRepository).putStatisticData(with(CourseOverGroundStatistic.STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID)), with(statistics1.getMatcher()));

            oneOf(statisticsRepository).getStatisticData(with(CourseOverGroundStatistic.STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID)));
            will(returnValue(existingStatisticData));
            oneOf(statisticsRepository).putStatisticData(with(CourseOverGroundStatistic.STATISTIC_NAME), (Long) with(track.getProperty(Track.CELL_ID)), with(statistics2.getMatcher()));
        }});

        // Execute
        statistic.start();
        statistic.onCellIdChanged(event);
        statistic.onCellIdChanged(event);

        // Assert expectations and captured values
        context.assertIsSatisfied();
        // TODO assertEquals(CourseOverGroundStatistic.STATISTIC_NAME, statistics2.getCapturedObject().getStatisticName());
        assertEquals(CourseOverGroundStatisticData.class, statistics2.getCapturedObject().getClass());
        CourseOverGroundStatisticData capturedStatisticData = (CourseOverGroundStatisticData) statistics2.getCapturedObject();
        assertEquals("type", capturedStatisticData.getMeaningOfKey1());
        assertEquals("size", capturedStatisticData.getMeaningOfKey2());
        assertEquals(TreeMap.class, statistics2.getCapturedObject().getData().getClass());
        TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, HashMap<String, Integer>>>> data = capturedStatisticData.getData();
        assertEquals(1, data.size()); // Assert one statistic recorded
        int shipTypeBucket = data.firstKey();
        assertEquals(3, shipTypeBucket);
        int shipSizeBucket = data.get(shipTypeBucket).firstKey();
        assertEquals(3, shipSizeBucket);
        int cogBucket = data.get(shipTypeBucket).get(shipSizeBucket).firstKey();
        assertEquals(7, cogBucket);
        int numberOfStatsForShipTypeAndShipSize = data.get(shipTypeBucket).get(shipSizeBucket).size();
        assertEquals(1, numberOfStatsForShipTypeAndShipSize);
        String statName = data.get(shipTypeBucket).get(shipSizeBucket).get(cogBucket).keySet().iterator().next();
        assertEquals(CourseOverGroundStatisticData.STAT_SHIP_COUNT, statName);
        Object statValue = data.get(shipTypeBucket).get(shipSizeBucket).get(cogBucket).get(statName);
        assertEquals(2, statValue);
    }
}
