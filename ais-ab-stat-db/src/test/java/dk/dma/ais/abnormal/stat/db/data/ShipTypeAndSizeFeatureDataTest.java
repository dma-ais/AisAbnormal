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

public class ShipTypeAndSizeFeatureDataTest {


    ShipTypeAndSizeFeatureData featureData;

    @Before
    public void initTest() {
        final int MAX_KEY_1 = Categorizer.NUM_SHIP_TYPE_CATEGORIES - 1;  /* 1-8 -> 0..7 */
        final int MAX_KEY_2 = Categorizer.NUM_SHIP_SIZE_CATEGORIES - 1;  /* 1-6 -> 0..5 */
        final int MAX_NUM_KEY_3 = 1;

        featureData = ShipTypeAndSizeFeatureData.create();
    }

    @Test
    public void canStoreAndRetrieveSingleStatistic() {
        featureData.setValue(3, 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT, 42);

        assertEquals((Integer) 42, featureData.getValue(3, 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertNull(featureData.getValue(2, 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertNull(featureData.getValue(3, 2, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
    }

    @Test
    public void canIncrementInitializedStatistic() {
        featureData.setValue(3, 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT, 42);
        featureData.incrementValue(3, 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT);

        assertEquals((Integer) 43, featureData.getValue(3, 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
    }

    @Test
    public void canIncrementUninitializedStatistic() {
        featureData.incrementValue(3, 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT);
        assertEquals((Integer) 1, featureData.getValue(3, 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));

        featureData = ShipTypeAndSizeFeatureData.create();
        featureData.incrementValue(3, 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT);
        featureData.incrementValue(3, 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT);
        assertEquals((Integer) 2, featureData.getValue(3, 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
    }

    @Test
    public void canSumFor() {
        featureData.setValue(1, 3, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT, 17);
        featureData.setValue(2, 3, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT, 42);
        featureData.setValue(3, 5, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT, 22);
        featureData.setValue(4, 0, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT, 431);

        assertEquals(17+42+22+431, featureData.getSumFor(ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
    }

    @Test
    public void getData() {
        featureData.setValue(3, 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT, 42);

        TreeMap<Integer,TreeMap<Integer,HashMap<String,Integer>>> data = featureData.getData();

        assertEquals(42, (int) data.get(3).get(1).get(ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertNull(data.get(3).get(2));
        assertNull(data.get(2));
        assertEquals(1, data.keySet().size());
        assertEquals(1, data.get(3).keySet().size());
    }

    @Test
    public void computeKey() {
        assertEquals(0, featureData.computeMapKey(0, 0, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertEquals(5, featureData.computeMapKey(0, 5, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertEquals(6, featureData.computeMapKey(1, 0, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertEquals(7, featureData.computeMapKey(1, 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertEquals(12, featureData.computeMapKey(2, 0, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertEquals(17, featureData.computeMapKey(2, 5, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertEquals(18, featureData.computeMapKey(3, 0, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertEquals(19, featureData.computeMapKey(3, 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertEquals(35, featureData.computeMapKey(5, 5, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
    }

    @Test
    public void extractStatisticsId() {
        assertEquals(0, featureData.extractStatisticId((short) 0));
        assertEquals(0, featureData.extractStatisticId((short) 9));
        assertEquals(0, featureData.extractStatisticId((short) 11));
        assertEquals(0, featureData.extractStatisticId((short) 55));
    }

    @Test
    public void extractKey1() {
        assertEquals(0, featureData.extractKey1((short) 0));
        assertEquals(0, featureData.extractKey1((short) 5));
        assertEquals(1, featureData.extractKey1((short) 6));
        assertEquals(1, featureData.extractKey1((short) 7));
        assertEquals(2, featureData.extractKey1((short) 12));
        assertEquals(2, featureData.extractKey1((short) 17));
        assertEquals(3, featureData.extractKey1((short) 18));
        assertEquals(3, featureData.extractKey1((short) 19));
        assertEquals(5, featureData.extractKey1((short) 35));

    }

    @Test
    public void extractKey2() {
        assertEquals(0, featureData.extractKey2((short) 0));
        assertEquals(5, featureData.extractKey2((short) 5));
        assertEquals(0, featureData.extractKey2((short) 6));
        assertEquals(1, featureData.extractKey2((short) 7));
        assertEquals(0, featureData.extractKey2((short) 12));
        assertEquals(5, featureData.extractKey2((short) 17));
        assertEquals(0, featureData.extractKey2((short) 18));
        assertEquals(1, featureData.extractKey2((short) 19));
        assertEquals(5, featureData.extractKey2((short) 35));
    }

}
