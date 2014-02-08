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

public abstract class FourKeyFeatureDataTest<T extends FourKeyFeatureData> {

    T featureData;

    @Test
    public void canStoreAndRetrieveSingleStatistic() {
        featureData.setValue(3, 1, 4, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 42);

        assertEquals((Integer) 42, featureData.getValue(3, 1, 4, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
        assertNull(featureData.getValue(2, 1, 4, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
        assertNull(featureData.getValue(3, 2, 4, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
    }

    @Test
    public void canIncrementInitializedStatistic() {
        featureData.setValue(3, 1, 4, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 42);
        featureData.incrementValue(3, 1, 4, CourseOverGroundFeatureData.STAT_SHIP_COUNT);

        assertEquals((Integer) 43, featureData.getValue(3, 1, 4, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
    }

    @Test
    public void canIncrementUninitializedStatistic() {
        featureData.incrementValue(3, 1, 4, CourseOverGroundFeatureData.STAT_SHIP_COUNT);
        assertEquals((Integer) 1, featureData.getValue(3, 1, 4, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
    }

    @Test
    public void canIncrementUninitializedStatisticTwice() {
        featureData.incrementValue(3, 1, 4, CourseOverGroundFeatureData.STAT_SHIP_COUNT);
        featureData.incrementValue(3, 1, 4, CourseOverGroundFeatureData.STAT_SHIP_COUNT);
        assertEquals((Integer) 2, featureData.getValue(3, 1, 4, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
    }

    @Test
    public void canSumFor() {
        featureData.setValue(1, 3, 4, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 17);
        featureData.setValue(2, 3, 7, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 42);
        featureData.setValue(3, 0, 1, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 22);
        featureData.setValue(4, 4, 0, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 431);

        assertEquals(17+42+22+431, featureData.getSumFor(CourseOverGroundFeatureData.STAT_SHIP_COUNT));
    }

    @Test
    public void getData() {
        featureData.setValue(3, 1, 4, CourseOverGroundFeatureData.STAT_SHIP_COUNT, 42);

        TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, HashMap<String, Integer>>>> data = featureData.getData();

        assertEquals(42, (int) data.get(3+1).get(1+1).get(4+1).get(CourseOverGroundFeatureData.STAT_SHIP_COUNT));
        assertNull(data.get(2));
        assertEquals(1, data.keySet().size());
        assertEquals(1, data.get(3+1).keySet().size());
    }

    @Test
    public void computeKey() {
        //                                          -9  -5 -12
        assertEquals( 0, featureData.computeMapKey( 0,  0,  0, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( 1, featureData.computeMapKey( 0,  0,  1, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( featureData.MAX_KEY_3, featureData.computeMapKey( 0,  0, featureData.MAX_KEY_3, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( 1*(featureData.MAX_KEY_3+1), featureData.computeMapKey( 0,  1,  0, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( 2*(featureData.MAX_KEY_3+1), featureData.computeMapKey( 0,  2,  0, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( 3*(featureData.MAX_KEY_3+1) + featureData.MAX_KEY_3, featureData.computeMapKey( 0,  3, featureData.MAX_KEY_3, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( 4*(featureData.MAX_KEY_3+1), featureData.computeMapKey( 0,  4,  0, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( (featureData.MAX_KEY_2+1)*(featureData.MAX_KEY_3+1), featureData.computeMapKey( 1,  0,  0, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( (featureData.MAX_KEY_2+1)*(featureData.MAX_KEY_3+1) + 1*(featureData.MAX_KEY_3+1), featureData.computeMapKey( 1,  1,  0, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( (featureData.MAX_KEY_2+1)*(featureData.MAX_KEY_3+1) + 1*(featureData.MAX_KEY_3+1) + 1, featureData.computeMapKey( 1,  1,  1, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( 2*(featureData.MAX_KEY_2+1)*(featureData.MAX_KEY_3+1) + 4*(featureData.MAX_KEY_3+1) + 3, featureData.computeMapKey( 2,  4,  3, CourseOverGroundFeatureData.STAT_SHIP_COUNT));
    }

    @Test
    public void computeKeyAcceptsLegalKey1() {
        featureData.computeMapKey(featureData.MAX_KEY_1, 0, 0, CourseOverGroundFeatureData.STAT_SHIP_COUNT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeKeyRejectsIllegalKey1() {
        featureData.computeMapKey(featureData.MAX_KEY_1 + 1, 0, 0, CourseOverGroundFeatureData.STAT_SHIP_COUNT);
    }

    @Test
    public void computeKeyAcceptsLegalKey2() {
        featureData.computeMapKey(0, featureData.MAX_KEY_2, 0, CourseOverGroundFeatureData.STAT_SHIP_COUNT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeKeyRejectsIllegalKey2() {
        featureData.computeMapKey(0, featureData.MAX_KEY_2+1, 0, CourseOverGroundFeatureData.STAT_SHIP_COUNT);
    }

    @Test
    public void computeKeyAcceptsLegalKey3() {
        featureData.computeMapKey(0, 0, featureData.MAX_KEY_3, CourseOverGroundFeatureData.STAT_SHIP_COUNT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeKeyRejectsIllegalKey3() {
        featureData.computeMapKey(0, 0, featureData.MAX_KEY_3+1, CourseOverGroundFeatureData.STAT_SHIP_COUNT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void computeKeyRejectsIllegalKey4() {
        featureData.computeMapKey(0, 0, 0, "strangeUnexpectedName");
    }

}
