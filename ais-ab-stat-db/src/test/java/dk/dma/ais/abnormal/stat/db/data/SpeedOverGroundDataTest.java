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

public class SpeedOverGroundDataTest extends FourKeyFeatureDataTest<SpeedOverGroundFeatureData> {

    @Before
    public void initTest() {
        final int MAX_KEY_1 = Categorizer.NUM_SHIP_TYPE_CATEGORIES - 1;         // 7(8)
        final int MAX_KEY_2 = Categorizer.NUM_SHIP_SIZE_CATEGORIES - 1;         // 4(5)
        final int MAX_KEY_3 = Categorizer.NUM_SPEED_OVER_GROUND_CATEGORIES - 1; // 7(8)
        final int MAX_NUM_KEY_4 = 1;

        featureData = new SpeedOverGroundFeatureData(MAX_KEY_1, MAX_KEY_2, MAX_KEY_3, MAX_NUM_KEY_4);
    }

    @Test
    public void computeKey() {                                      
        assertEquals( 0, featureData.computeMapKey( 0,  0,  0, SpeedOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( 1, featureData.computeMapKey( 0,  0,  1, SpeedOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( 7, featureData.computeMapKey( 0,  0, featureData.MAX_KEY_3, SpeedOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( 8, featureData.computeMapKey( 0,  1,  0, SpeedOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( 2*(featureData.MAX_KEY_3+1), featureData.computeMapKey( 0,  2,  0, SpeedOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( 3*(featureData.MAX_KEY_3+1) + featureData.MAX_KEY_3, featureData.computeMapKey( 0,  3, featureData.MAX_KEY_3, SpeedOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( 4*(featureData.MAX_KEY_3+1), featureData.computeMapKey( 0,  4,  0, SpeedOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( (featureData.MAX_KEY_2+1)*(featureData.MAX_KEY_3+1), featureData.computeMapKey( 1,  0,  0, SpeedOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( (featureData.MAX_KEY_2+1)*(featureData.MAX_KEY_3+1) + 1*(featureData.MAX_KEY_3+1), featureData.computeMapKey( 1,  1,  0, SpeedOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( (featureData.MAX_KEY_2+1)*(featureData.MAX_KEY_3+1) + 1*(featureData.MAX_KEY_3+1) + 1, featureData.computeMapKey( 1,  1,  1, SpeedOverGroundFeatureData.STAT_SHIP_COUNT));
        assertEquals( 2*(featureData.MAX_KEY_2+1)*(featureData.MAX_KEY_3+1) + 4*(featureData.MAX_KEY_3+1) + 3, featureData.computeMapKey( 2,  4,  3, SpeedOverGroundFeatureData.STAT_SHIP_COUNT));
    }


    @Test
    public void extractKeys() {
        int expectedKey = 0;

        for (int key1 = 0; key1 < Categorizer.NUM_SHIP_TYPE_CATEGORIES; key1++) {
            for (int key2 = 0; key2 < Categorizer.NUM_SHIP_SIZE_CATEGORIES; key2++) {
                for (int key3 = 0; key3 < Categorizer.NUM_SPEED_OVER_GROUND_CATEGORIES; key3++) {
                    short key = featureData.computeMapKey(key1, key2, key3, SpeedOverGroundFeatureData.STAT_SHIP_COUNT);
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
