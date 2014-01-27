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

public class CourseOverGroundDataTest {

    CourseOverGroundData featureData;

    @Before
    public void initTest() {
        final int MAX_KEY_1 = Categorizer.NUM_SHIP_TYPE_CATEGORIES - 1;  /* 1-8 -> 0..7 */
        final int MAX_KEY_2 = Categorizer.NUM_SHIP_SIZE_CATEGORIES - 1;  /* 1-6 -> 0..5 */
        final int MAX_KEY_3 = Categorizer.NUM_COURSE_OVER_GROUND_CATEGORIES - 1;  /* 1-12 -> 0..11 */
        final int MAX_NUM_KEY_4 = 1;

        featureData = new CourseOverGroundData(MAX_KEY_1, MAX_KEY_2, MAX_KEY_3, MAX_NUM_KEY_4);
    }

    @Test
    public void canStoreAndRetrieveSingleStatistic() {
        featureData.setValue(3, 1, 4, CourseOverGroundData.STAT_SHIP_COUNT, 42);

        assertEquals((Integer) 42, featureData.getValue(3, 1, 4, CourseOverGroundData.STAT_SHIP_COUNT));
        assertNull(featureData.getValue(2, 1, 4, CourseOverGroundData.STAT_SHIP_COUNT));
        assertNull(featureData.getValue(3, 2, 4, CourseOverGroundData.STAT_SHIP_COUNT));
    }

    @Test
    public void canIncrementInitializedStatistic() {
        featureData.setValue(3, 1, 4, CourseOverGroundData.STAT_SHIP_COUNT, 42);
        featureData.incrementValue(3, 1, 4, CourseOverGroundData.STAT_SHIP_COUNT);

        assertEquals((Integer) 43, featureData.getValue(3, 1, 4, CourseOverGroundData.STAT_SHIP_COUNT));
    }

    @Test
    public void canIncrementUninitializedStatistic() {
        featureData.incrementValue(3, 1, 4, CourseOverGroundData.STAT_SHIP_COUNT);
        assertEquals((Integer) 1, featureData.getValue(3, 1, 4, CourseOverGroundData.STAT_SHIP_COUNT));
    }

    @Test
    public void canIncrementUninitializedStatisticTwice() {
        featureData.incrementValue(3, 1, 4, CourseOverGroundData.STAT_SHIP_COUNT);
        featureData.incrementValue(3, 1, 4, CourseOverGroundData.STAT_SHIP_COUNT);
        assertEquals((Integer) 2, featureData.getValue(3, 1, 4, CourseOverGroundData.STAT_SHIP_COUNT));
    }

    @Test
    public void canSumFor() {
        featureData.setValue(1, 3, 4, CourseOverGroundData.STAT_SHIP_COUNT, 17);
        featureData.setValue(2, 3, 7, CourseOverGroundData.STAT_SHIP_COUNT, 42);
        featureData.setValue(3, 5, 1, CourseOverGroundData.STAT_SHIP_COUNT, 22);
        featureData.setValue(4, 4, 0, CourseOverGroundData.STAT_SHIP_COUNT, 431);

        assertEquals(17+42+22+431, featureData.getSumFor(CourseOverGroundData.STAT_SHIP_COUNT));
    }

    @Test
    public void getData() {
        featureData.setValue(3, 1, 4, CourseOverGroundData.STAT_SHIP_COUNT, 42);

        TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, HashMap<String, Integer>>>> data = featureData.getData();

        assertEquals(42, (int) data.get(3).get(1).get(4).get(CourseOverGroundData.STAT_SHIP_COUNT));
        assertNull(data.get(3).get(2));
        assertNull(data.get(2));
        assertEquals(1, data.keySet().size());
        assertEquals(1, data.get(3).keySet().size());
    }

    @Test
    public void computeKey() {
        //                                          -9  -6 -12
        assertEquals(  0, featureData.computeMapKey( 0,  0,  0, CourseOverGroundData.STAT_SHIP_COUNT));
        assertEquals(  1, featureData.computeMapKey( 0,  0,  1, CourseOverGroundData.STAT_SHIP_COUNT));
        assertEquals( 11, featureData.computeMapKey( 0,  0, 11, CourseOverGroundData.STAT_SHIP_COUNT));
        assertEquals( 12, featureData.computeMapKey( 0,  1,  0, CourseOverGroundData.STAT_SHIP_COUNT));
        assertEquals( 24, featureData.computeMapKey( 0,  2,  0, CourseOverGroundData.STAT_SHIP_COUNT));
        assertEquals( 59, featureData.computeMapKey( 0,  4, 11, CourseOverGroundData.STAT_SHIP_COUNT));
        assertEquals( 60, featureData.computeMapKey( 0,  5,  0, CourseOverGroundData.STAT_SHIP_COUNT));
        assertEquals( 72, featureData.computeMapKey( 1,  0,  0, CourseOverGroundData.STAT_SHIP_COUNT));
        assertEquals( 84, featureData.computeMapKey( 1,  1,  0, CourseOverGroundData.STAT_SHIP_COUNT));
        assertEquals( 85, featureData.computeMapKey( 1,  1,  1, CourseOverGroundData.STAT_SHIP_COUNT));
        assertEquals(207, featureData.computeMapKey( 2,  5,  3, CourseOverGroundData.STAT_SHIP_COUNT));
    }

    @Test
    public void computeKeyAcceptsLegalKey1() {
        featureData.computeMapKey(featureData.MAX_KEY_1, 0, 0, CourseOverGroundData.STAT_SHIP_COUNT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeKeyRejectsIllegalKey1() {
        featureData.computeMapKey(featureData.MAX_KEY_1 + 1, 0, 0, CourseOverGroundData.STAT_SHIP_COUNT);
    }

    @Test
    public void computeKeyAcceptsLegalKey2() {
        featureData.computeMapKey(0, featureData.MAX_KEY_2, 0, CourseOverGroundData.STAT_SHIP_COUNT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeKeyRejectsIllegalKey2() {
        featureData.computeMapKey(0, featureData.MAX_KEY_2+1, 0, CourseOverGroundData.STAT_SHIP_COUNT);
    }

    @Test
    public void computeKeyAcceptsLegalKey3() {
        featureData.computeMapKey(0, 0, featureData.MAX_KEY_3, CourseOverGroundData.STAT_SHIP_COUNT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeKeyRejectsIllegalKey3() {
        featureData.computeMapKey(0, 0, featureData.MAX_KEY_3+1, CourseOverGroundData.STAT_SHIP_COUNT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeKeyRejectsIllegalKey4() {
        featureData.computeMapKey(0, 0, 0, "strangeUnexpectedName");
    }

    @Test
    public void extractKey1() {
        assertEquals(0, featureData.extractKey1((short)   0));
        assertEquals(0, featureData.extractKey1((short)   1));
        assertEquals(0, featureData.extractKey1((short)  11));
        assertEquals(0, featureData.extractKey1((short)  12));
        assertEquals(0, featureData.extractKey1((short)  24));
        assertEquals(0, featureData.extractKey1((short)  59));
        assertEquals(0, featureData.extractKey1((short)  60));
        assertEquals(0, featureData.extractKey1((short)  71));
        assertEquals(1, featureData.extractKey1((short)  72));
        assertEquals(1, featureData.extractKey1((short)  84));
        assertEquals(1, featureData.extractKey1((short)  85));
        assertEquals(2, featureData.extractKey1((short) 207));
    }

    @Test
    public void extractKey2() {
        assertEquals(0, featureData.extractKey2((short) 0));
        assertEquals(0, featureData.extractKey2((short) 1));
        assertEquals(0, featureData.extractKey2((short) 11));
        assertEquals(1, featureData.extractKey2((short) 12));
        assertEquals(2, featureData.extractKey2((short) 24));
        assertEquals(4, featureData.extractKey2((short) 59));
        assertEquals(5, featureData.extractKey2((short) 60));
        assertEquals(5, featureData.extractKey2((short) 71));
        assertEquals(0, featureData.extractKey2((short) 72));
        assertEquals(1, featureData.extractKey2((short) 84));
        assertEquals(1, featureData.extractKey2((short) 85));
        assertEquals(5, featureData.extractKey2((short) 207));
    }

    @Test
    public void extractKey3() {
        assertEquals( 0, featureData.extractKey3((short) 0));
        assertEquals( 1, featureData.extractKey3((short) 1));
        assertEquals(11, featureData.extractKey3((short) 11));
        assertEquals( 0, featureData.extractKey3((short) 12));
        assertEquals( 0, featureData.extractKey3((short) 24));
        assertEquals(11, featureData.extractKey3((short) 59));
        assertEquals( 0, featureData.extractKey3((short) 60));
        assertEquals(11, featureData.extractKey3((short) 71));
        assertEquals( 0, featureData.extractKey3((short) 72));
        assertEquals( 0, featureData.extractKey3((short) 84));
        assertEquals( 1, featureData.extractKey3((short) 85));
        assertEquals( 3, featureData.extractKey3((short) 207));
    }

    @Test
    public void extractKey4() {
        assertEquals(0, featureData.extractKey4((short) 0));
        assertEquals(0, featureData.extractKey4((short) 1));
        assertEquals(0, featureData.extractKey4((short) 11));
        assertEquals(0, featureData.extractKey4((short) 12));
        assertEquals(0, featureData.extractKey4((short) 24));
        assertEquals(0, featureData.extractKey4((short) 59));
        assertEquals(0, featureData.extractKey4((short) 60));
        assertEquals(0, featureData.extractKey4((short) 71));
        assertEquals(0, featureData.extractKey4((short) 72));
        assertEquals(0, featureData.extractKey4((short) 84));
        assertEquals(0, featureData.extractKey4((short) 85));
        assertEquals(0, featureData.extractKey4((short) 207));
    }

}
