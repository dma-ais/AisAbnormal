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

public class SpeedOverGroundDataTest extends FourKeyFeatureDataTest<SpeedOverGroundFeatureData> {

    @Before
    public void initTest() {
        final int MAX_KEY_1 = Categorizer.NUM_SHIP_TYPE_CATEGORIES - 1;  /* 1-8 -> 0..7 */
        final int MAX_KEY_2 = Categorizer.NUM_SHIP_SIZE_CATEGORIES - 1;  /* 1-6 -> 0..5 */
        final int MAX_KEY_3 = Categorizer.NUM_COURSE_OVER_GROUND_CATEGORIES - 1;  /* 1-12 -> 0..11 */
        final int MAX_NUM_KEY_4 = 1;

        featureData = new SpeedOverGroundFeatureData(MAX_KEY_1, MAX_KEY_2, MAX_KEY_3, MAX_NUM_KEY_4);
    }

}
