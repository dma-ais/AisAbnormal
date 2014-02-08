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

import static org.junit.Assert.assertEquals;

public class CourseOverGroundDataTest extends FourKeyFeatureDataTest<CourseOverGroundFeatureData> {

    @Before
    public void initTest() {
        final int MAX_KEY_1 = Categorizer.NUM_SHIP_TYPE_CATEGORIES - 1;  /* 1-8 -> 0..7 */
        final int MAX_KEY_2 = Categorizer.NUM_SHIP_SIZE_CATEGORIES - 1;  /* 1-6 -> 0..5 */
        final int MAX_KEY_3 = Categorizer.NUM_COURSE_OVER_GROUND_CATEGORIES - 1;  /* 1-12 -> 0..11 */
        final int MAX_NUM_KEY_4 = 1;

        featureData = new CourseOverGroundFeatureData(MAX_KEY_1, MAX_KEY_2, MAX_KEY_3, MAX_NUM_KEY_4);
    }

    @Test
    public void extractKeys() {
        int expectedKey = 0;

        for (int key1 = 0; key1 < Categorizer.NUM_SHIP_TYPE_CATEGORIES; key1++) {
            for (int key2 = 0; key2 < Categorizer.NUM_SHIP_SIZE_CATEGORIES; key2++) {
                for (int key3 = 0; key3 < Categorizer.NUM_COURSE_OVER_GROUND_CATEGORIES; key3++) {
                    short key = featureData.computeMapKey(key1, key2, key3, CourseOverGroundFeatureData.STAT_SHIP_COUNT);
                    System.out.println("key:" + key + " key1:" + key1 + " key2:" + key2 + " key3:" + key3);
                    assertEquals(expectedKey++, key);
                    assertEquals(key1, featureData.extractKey1(key));
                    assertEquals(key2, featureData.extractKey2(key));
                    assertEquals(key3, featureData.extractKey3(key));
                }
            }
        }
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
