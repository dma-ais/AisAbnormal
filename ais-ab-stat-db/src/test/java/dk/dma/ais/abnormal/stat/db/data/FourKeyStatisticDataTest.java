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

public abstract class FourKeyStatisticDataTest<T extends FourKeyStatisticData> {

    T statistics;

    @Test
    public void canStoreAndRetrieveSingleStatistic() {
        statistics.setValue(3, 1, 4, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 42);

        assertEquals((Integer) 42, statistics.getValue(3, 1, 4, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
        assertNull(statistics.getValue(2, 1, 4, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
        assertNull(statistics.getValue(3, 2, 4, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
    }

    @Test
    public void canIncrementInitializedStatistic() {
        statistics.setValue(3, 1, 4, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 42);
        statistics.incrementValue(3, 1, 4, CourseOverGroundStatisticData.STAT_SHIP_COUNT);

        assertEquals((Integer) 43, statistics.getValue(3, 1, 4, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
    }

    @Test
    public void canIncrementUninitializedStatistic() {
        statistics.incrementValue(3, 1, 4, CourseOverGroundStatisticData.STAT_SHIP_COUNT);
        assertEquals((Integer) 1, statistics.getValue(3, 1, 4, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
    }

    @Test
    public void canIncrementUninitializedStatisticTwice() {
        statistics.incrementValue(3, 1, 4, CourseOverGroundStatisticData.STAT_SHIP_COUNT);
        statistics.incrementValue(3, 1, 4, CourseOverGroundStatisticData.STAT_SHIP_COUNT);
        assertEquals((Integer) 2, statistics.getValue(3, 1, 4, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
    }

    @Test
    public void canSumFor() {
        statistics.setValue(1, 3, 4, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 17);
        statistics.setValue(2, 3, 7, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 42);
        statistics.setValue(3, 0, 1, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 22);
        statistics.setValue(4, 4, 0, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 431);

        assertEquals(17+42+22+431, statistics.getSumFor(CourseOverGroundStatisticData.STAT_SHIP_COUNT));
    }

    @Test
    public void getData() {
        statistics.setValue(3, 1, 4, CourseOverGroundStatisticData.STAT_SHIP_COUNT, 42);

        TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, HashMap<String, Integer>>>> data = statistics.getData();

        assertEquals(42, (int) data.get(3+1).get(1+1).get(4+1).get(CourseOverGroundStatisticData.STAT_SHIP_COUNT));
        assertNull(data.get(2));
        assertEquals(1, data.keySet().size());
        assertEquals(1, data.get(3+1).keySet().size());
    }

    @Test
    public void computeKey() {
        //                                          -9  -5 -12
        assertEquals( 0, statistics.computeMapKey( 0,  0,  0, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
        assertEquals( 1, statistics.computeMapKey( 0,  0,  1, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
        assertEquals( statistics.MAX_KEY_3, statistics.computeMapKey( 0,  0, statistics.MAX_KEY_3, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
        assertEquals( 1*(statistics.MAX_KEY_3+1), statistics.computeMapKey( 0,  1,  0, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
        assertEquals( 2*(statistics.MAX_KEY_3+1), statistics.computeMapKey( 0,  2,  0, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
        assertEquals( 3*(statistics.MAX_KEY_3+1) + statistics.MAX_KEY_3, statistics.computeMapKey( 0,  3, statistics.MAX_KEY_3, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
        assertEquals( 4*(statistics.MAX_KEY_3+1), statistics.computeMapKey( 0,  4,  0, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
        assertEquals( (statistics.MAX_KEY_2+1)*(statistics.MAX_KEY_3+1), statistics.computeMapKey( 1,  0,  0, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
        assertEquals( (statistics.MAX_KEY_2+1)*(statistics.MAX_KEY_3+1) + 1*(statistics.MAX_KEY_3+1), statistics.computeMapKey( 1,  1,  0, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
        assertEquals( (statistics.MAX_KEY_2+1)*(statistics.MAX_KEY_3+1) + 1*(statistics.MAX_KEY_3+1) + 1, statistics.computeMapKey( 1,  1,  1, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
        assertEquals( 2*(statistics.MAX_KEY_2+1)*(statistics.MAX_KEY_3+1) + 4*(statistics.MAX_KEY_3+1) + 3, statistics.computeMapKey( 2,  4,  3, CourseOverGroundStatisticData.STAT_SHIP_COUNT));
    }

    @Test
    public void computeKeyAcceptsLegalKey1() {
        statistics.computeMapKey(statistics.MAX_KEY_1, 0, 0, CourseOverGroundStatisticData.STAT_SHIP_COUNT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeKeyRejectsIllegalKey1() {
        statistics.computeMapKey(statistics.MAX_KEY_1 + 1, 0, 0, CourseOverGroundStatisticData.STAT_SHIP_COUNT);
    }

    @Test
    public void computeKeyAcceptsLegalKey2() {
        statistics.computeMapKey(0, statistics.MAX_KEY_2, 0, CourseOverGroundStatisticData.STAT_SHIP_COUNT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeKeyRejectsIllegalKey2() {
        statistics.computeMapKey(0, statistics.MAX_KEY_2+1, 0, CourseOverGroundStatisticData.STAT_SHIP_COUNT);
    }

    @Test
    public void computeKeyAcceptsLegalKey3() {
        statistics.computeMapKey(0, 0, statistics.MAX_KEY_3, CourseOverGroundStatisticData.STAT_SHIP_COUNT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeKeyRejectsIllegalKey3() {
        statistics.computeMapKey(0, 0, statistics.MAX_KEY_3+1, CourseOverGroundStatisticData.STAT_SHIP_COUNT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeKeyRejectsIllegalKey4() {
        statistics.computeMapKey(0, 0, 0, "strangeUnexpectedName");
    }

}
