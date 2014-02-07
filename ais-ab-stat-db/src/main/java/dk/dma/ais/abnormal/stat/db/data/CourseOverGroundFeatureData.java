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

/**
 *
 * This is a memory-consumption optimised implementation of FourKeyMap intended to store
 * AIS feature statistics of type CourseOverGroundFeatureData for one grid cell.
 *
 */
public class CourseOverGroundFeatureData extends FourKeyFeatureData {

    public static final String STAT_SHIP_COUNT = "shipCount";

    @Override
    @SuppressWarnings("unused")
    public String getMeaningOfKey1() {
        return "type";
    }

    @Override
    @SuppressWarnings("unused")
    public String getMeaningOfKey2() {
        return "size";
    }

    @Override
    @SuppressWarnings("unused")
    public String getMeaningOfKey3() {
        return "cog";
    }

    @Override
    @SuppressWarnings("unused")
    public String getMeaningOfKey4() {
        return "statName";
    }

    @Override
    protected String getNameOfOnlySupportedValueOfKey4() {
        return STAT_SHIP_COUNT;
    }

    public static CourseOverGroundFeatureData create() {
        return new CourseOverGroundFeatureData(Categorizer.NUM_SHIP_TYPE_CATEGORIES - 1, Categorizer.NUM_SHIP_SIZE_CATEGORIES - 1, Categorizer.NUM_COURSE_OVER_GROUND_CATEGORIES - 1, 1);
    }

    protected CourseOverGroundFeatureData(int maxKey1, int maxKey2, int maxKey3, int maxNumKey4) {
        super(maxKey1, maxKey2, maxKey3, maxNumKey4);
    }

}
