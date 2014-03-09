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

package dk.dma.ais.abnormal.stat.db.data;

import dk.dma.ais.abnormal.util.Categorizer;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ShipTypeAndSizeStatisticDataTest {


    ShipTypeAndSizeStatisticData statistics;

    @Before
    public void initTest() {
        final int MAX_KEY_1 = Categorizer.NUM_SHIP_TYPE_CATEGORIES - 1;  /* 1-8 -> 0..7 */
        final int MAX_KEY_2 = Categorizer.NUM_SHIP_SIZE_CATEGORIES - 1;  /* 1-6 -> 0..5 */
        final int MAX_NUM_KEY_3 = 1;

        statistics = ShipTypeAndSizeStatisticData.create();
    }

    @Test
    public void canStoreAndRetrieveSingleStatistic() {
        statistics.setValue(3, 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT, 42);

        assertEquals((Integer) 42, statistics.getValue(3, 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertNull(statistics.getValue(2, 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertNull(statistics.getValue(3, 2, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
    }

    @Test
    public void canIncrementInitializedStatistic() {
        statistics.setValue(3, 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT, 42);
        statistics.incrementValue(3, 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT);

        assertEquals((Integer) 43, statistics.getValue(3, 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
    }

    @Test
    public void canIncrementUninitializedStatistic() {
        statistics.incrementValue(3, 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT);
        assertEquals((Integer) 1, statistics.getValue(3, 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));

        statistics = ShipTypeAndSizeStatisticData.create();
        statistics.incrementValue(3, 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT);
        statistics.incrementValue(3, 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT);
        assertEquals((Integer) 2, statistics.getValue(3, 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
    }

    @Test
    public void canSumFor() {
        statistics.setValue(1, 3, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT, 17);
        statistics.setValue(2, 3, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT, 42);
        statistics.setValue(0, 4, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT, 22);
        statistics.setValue(4, 0, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT, 431);

        assertEquals(17+42+22+431, statistics.getSumFor(ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
    }

    @Test
    public void getData() {
        statistics.setValue(3, 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT, 42);

        TreeMap<Integer,TreeMap<Integer,HashMap<String,Integer>>> data = statistics.getData();

        assertEquals(42, (int) data.get(3+1).get(1+1).get(ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertNull(data.get(3+1).get(2+1));
        assertNull(data.get(2+1));
        assertEquals(1, data.keySet().size());
        assertEquals(1, data.get(3+1).keySet().size());
    }

    @Test
    public void computeKey() {
        assertEquals(0, statistics.computeMapKey(0, 0, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertEquals(4, statistics.computeMapKey(0, 4, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertEquals(5, statistics.computeMapKey(1, 0, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertEquals(6, statistics.computeMapKey(1, 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertEquals(10, statistics.computeMapKey(2, 0, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertEquals(14, statistics.computeMapKey(2, 4, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertEquals(15, statistics.computeMapKey(3, 0, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertEquals(16, statistics.computeMapKey(3, 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertEquals(29, statistics.computeMapKey(5, 4, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
    }

    @Test
    public void extractStatisticsId() {
        assertEquals(0, statistics.extractStatisticId((short) 0));
        assertEquals(0, statistics.extractStatisticId((short) 9));
        assertEquals(0, statistics.extractStatisticId((short) 11));
        assertEquals(0, statistics.extractStatisticId((short) 55));
    }

    @Test
    public void extractKeys() {
        int expectedKey = 0;

        for (int key1 = 0; key1 < Categorizer.NUM_SHIP_TYPE_CATEGORIES; key1++) {
            for (int key2 = 0; key2 < Categorizer.NUM_SHIP_SIZE_CATEGORIES; key2++) {
                short key = statistics.computeMapKey(key1, key2, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT);
                System.out.println("key:" + key + " key1:" + key1 + " key2:" + key2);
                assertEquals(expectedKey++, key);
                assertEquals(key1, statistics.extractKey1(key));
                assertEquals(key2, statistics.extractKey2(key));
            }
        }
    }

}
