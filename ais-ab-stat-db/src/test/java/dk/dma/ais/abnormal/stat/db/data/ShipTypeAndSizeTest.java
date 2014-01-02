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

import org.junit.Test;

import java.util.HashMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ShipTypeAndSizeTest {

    @Test
    public void canStoreAndRetrieveSingleStatistic() {
        ShipTypeAndSizeData featureData = new ShipTypeAndSizeData();

        featureData.setStatistic(3, 1, ShipTypeAndSizeData.STAT_SHIP_COUNT, 42);

        assertEquals((Integer) 42, featureData.getStatistic(3, 1, ShipTypeAndSizeData.STAT_SHIP_COUNT));
        assertNull(featureData.getStatistic(2, 1, ShipTypeAndSizeData.STAT_SHIP_COUNT));
        assertNull(featureData.getStatistic(3, 2, ShipTypeAndSizeData.STAT_SHIP_COUNT));
    }

    @Test
    public void canIncrementInitializedStatistic() {
        ShipTypeAndSizeData featureData = new ShipTypeAndSizeData();

        featureData.setStatistic(3, 1, ShipTypeAndSizeData.STAT_SHIP_COUNT, 42);
        featureData.incrementStatistic(3, 1,ShipTypeAndSizeData.STAT_SHIP_COUNT);

        assertEquals((Integer) 43, featureData.getStatistic(3, 1, ShipTypeAndSizeData.STAT_SHIP_COUNT));
    }

    @Test
    public void canIncrementUninitializedStatistic() {
        ShipTypeAndSizeData featureData = new ShipTypeAndSizeData();
        featureData.incrementStatistic(3, 1,ShipTypeAndSizeData.STAT_SHIP_COUNT);
        assertEquals((Integer) 1, featureData.getStatistic(3, 1, ShipTypeAndSizeData.STAT_SHIP_COUNT));

        featureData = new ShipTypeAndSizeData();
        featureData.incrementStatistic(3, 1,ShipTypeAndSizeData.STAT_SHIP_COUNT);
        featureData.incrementStatistic(3, 1,ShipTypeAndSizeData.STAT_SHIP_COUNT);
        assertEquals((Integer) 2, featureData.getStatistic(3, 1, ShipTypeAndSizeData.STAT_SHIP_COUNT));
    }

    @Test
    public void canSumFor() {
        ShipTypeAndSizeData featureData = new ShipTypeAndSizeData();
        featureData.setStatistic(1, 3, ShipTypeAndSizeData.STAT_SHIP_COUNT, 17);
        featureData.setStatistic(2, 3, ShipTypeAndSizeData.STAT_SHIP_COUNT, 42);
        featureData.setStatistic(3, 5, ShipTypeAndSizeData.STAT_SHIP_COUNT, 22);
        featureData.setStatistic(4, 7, ShipTypeAndSizeData.STAT_SHIP_COUNT, 431);

        assertEquals(17+42+22+431, featureData.getSumFor(ShipTypeAndSizeData.STAT_SHIP_COUNT));
    }

    @Test
    public void getData() {
        ShipTypeAndSizeData featureData = new ShipTypeAndSizeData();

        featureData.setStatistic(3, 1, ShipTypeAndSizeData.STAT_SHIP_COUNT, 42);

        TreeMap<Integer,TreeMap<Integer,HashMap<String,Integer>>> data = featureData.getData();

        assertEquals(42, (int) data.get(3).get(1).get(ShipTypeAndSizeData.STAT_SHIP_COUNT));
        assertNull(data.get(3).get(2));
        assertNull(data.get(2));
        assertEquals(1, data.keySet().size());
        assertEquals(1, data.get(3).keySet().size());
    }

    @Test
    public void computeKey() {
        assertEquals(0, ShipTypeAndSizeData.computeMapKey(0, 0, ShipTypeAndSizeData.STAT_SHIP_COUNT));
        assertEquals(9, ShipTypeAndSizeData.computeMapKey(0, 9, ShipTypeAndSizeData.STAT_SHIP_COUNT));
        assertEquals(11, ShipTypeAndSizeData.computeMapKey(1, 1, ShipTypeAndSizeData.STAT_SHIP_COUNT));
        assertEquals(55, ShipTypeAndSizeData.computeMapKey(5, 5, ShipTypeAndSizeData.STAT_SHIP_COUNT));
    }

    @Test
    public void extractStatisticsId() {
        assertEquals(0, ShipTypeAndSizeData.extractStatisticId((short) 0));
        assertEquals(0, ShipTypeAndSizeData.extractStatisticId((short) 9));
        assertEquals(0, ShipTypeAndSizeData.extractStatisticId((short) 11));
        assertEquals(0, ShipTypeAndSizeData.extractStatisticId((short) 55));
    }

    @Test
    public void extractKey1() {
        assertEquals(0, ShipTypeAndSizeData.extractKey1((short) 0));
        assertEquals(0, ShipTypeAndSizeData.extractKey1((short) 9));
        assertEquals(1, ShipTypeAndSizeData.extractKey1((short) 11));
        assertEquals(5, ShipTypeAndSizeData.extractKey1((short) 55));
    }

    @Test
    public void extractKey2() {
        assertEquals(0, ShipTypeAndSizeData.extractKey2((short) 0));
        assertEquals(9, ShipTypeAndSizeData.extractKey2((short) 9));
        assertEquals(1, ShipTypeAndSizeData.extractKey2((short) 11));
        assertEquals(5, ShipTypeAndSizeData.extractKey2((short) 55));
    }

}
