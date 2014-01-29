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

package dk.dma.ais.abnormal.util;

/**
 * The Categorizer maps vessel properties into "categories"
 * (or "buckets") for use by those Analyses which require
 * categorized/bucketted feature data.
 *
 * Numbering of categories starts from 0 (as this is consistent
 * with indexing used by e.g. ThreeKeyMap and FourKeyMap.
 *
 */
public final class Categorizer {

    public static final int NUM_SHIP_TYPE_CATEGORIES = 8;
    public static final int NUM_SHIP_SIZE_CATEGORIES = 6;
    public static final int NUM_COURSE_OVER_GROUND_CATEGORIES = 12;

    public static short mapShipTypeToCategory(int shipType) {
        short category = 8;

        if (shipType > 79 && shipType < 90) {
            category = 1;
        } else if (shipType > 69 && shipType < 80) {
            category = 2;
        } else if ((shipType > 39 && shipType < 50) || (shipType > 59 && shipType < 70)) {
            category = 3;
        } else if ((shipType > 30 && shipType < 36) || (shipType > 49 && shipType < 56)) {
            category = 4;
        } else if (shipType == 30) {
            category = 5;
        } else if (shipType == 36 || shipType == 37) {    // TODO Class B
            category = 6;
        } else if ((shipType > 0 && shipType < 30) || (shipType > 89 && shipType < 100)) {
            category = 7;
        } else if (shipType == 0) {
            category = 8;
        }

        return (short) (category - 1);
    }

    public static short mapShipLengthToCategory(int shipLength) {
        short category;

        if (shipLength >= 0 && shipLength < 1) {
            category = 1;
        } else if (shipLength >= 1 && shipLength < 50) {
            category = 2;
        } else if (shipLength >= 50 && shipLength < 100) {
            category = 3;
        } else if (shipLength >= 100 && shipLength < 200) {
            category = 4;
        } else if (shipLength >= 200 && shipLength < 999) {
            category = 5;
        } else {
            category = 6;
        }

        return (short) (category - 1);
    }

    public static short mapCourseOverGroundToCategory(float cog) {
        cog = cog % (float) 360.0;
        return (short) (cog / 30);
    }

    public static short mapSpeedOverGroundToCategory(float sog) {
        short category;

        if (sog >= 0 && sog < 1) {
            category = 1;
        } else if (sog >= 1 && sog < 5) {
            category = 2;
        } else if (sog >= 5 && sog < 10) {
            category = 3;
        } else if (sog >= 10 && sog < 15) {
            category = 4;
        } else if (sog >= 15 && sog < 20) {
            category = 5;
        } else if (sog >= 20 && sog < 30) {
            category = 6;
        } else if (sog >= 30 && sog < 50) {
            category = 7;
        } else {
            category = 8;
        }

        return (short) (category - 1);
    }
}
