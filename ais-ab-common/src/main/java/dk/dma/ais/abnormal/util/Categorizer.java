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

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.TreeMap;

/**
 * The Categorizer maps vessel properties into "categories"
 * (or "buckets") for use by those Analyses which require
 * categorized/bucketted statistic data.
 *
 * Numbering of categories starts from 0 (as this is consistent
 * with indexing used by e.g. ThreeKeyMap and FourKeyMap.
 *
 */
public final class Categorizer {

    private final static String[] SHIP_TYPE_CATEGORIES = {
            "tanker",    //  1
            "cargo",     //  2
            "passenger", //  3
            "support",   //  4
            "fishing",   //  5
            "class b",   //  6
            "other",     //  7
            "undef"      //  8
    };

    private final static String[] SHIP_SIZE_CATEGORIES = {
            "undef",     //  1
            "1-50m",     //  2
            "50-100m",   //  3
            "100-200m",  //  4
            "200-999m"   //  5
    };

    private final static String[] COG_CATEGORIES = {
            "000-030",   //  1
            "030-060",   //  2
            "060-090",   //  3
            "090-120",   //  4
            "120-150",   //  5
            "150-180",   //  6
            "180-210",   //  7
            "210-240",   //  8
            "240-270",   //  9
            "270-300",   // 10
            "300-330",   // 11
            "330-360"    // 12
    };

    private final static String[] SOG_CATEGORIES = {
            "0-1kts",    //  1
            "1-5kts",    //  2
            "5-10kts",   //  3
            "10-15kts",  //  4
            "15-20kts",  //  5
            "20-30kts",  //  6
            "30-50kts",  //  7
            "50-100kts"  //  8
    };

    public static final int NUM_SHIP_TYPE_CATEGORIES = SHIP_TYPE_CATEGORIES.length;
    public static final int NUM_SHIP_SIZE_CATEGORIES = SHIP_SIZE_CATEGORIES.length;
    public static final int NUM_COURSE_OVER_GROUND_CATEGORIES = COG_CATEGORIES.length;
    public static final int NUM_SPEED_OVER_GROUND_CATEGORIES = SOG_CATEGORIES.length;

    private static Map<Short, String> ALL_SHIP_TYPE_CATEGORY_MAPPINGS;
    private static Map<Short, String> ALL_SHIP_SIZE_CATEGORY_MAPPINGS;
    private static Map<Short, String> ALL_COURSE_OVER_GROUND_CATEGORY_MAPPINGS;
    private static Map<Short, String> ALL_SPEED_OVER_GROUND_CATEGORY_MAPPINGS;

    static {
        ALL_SHIP_TYPE_CATEGORY_MAPPINGS = initMapping(ALL_SHIP_TYPE_CATEGORY_MAPPINGS, SHIP_TYPE_CATEGORIES);
        ALL_SHIP_SIZE_CATEGORY_MAPPINGS = initMapping(ALL_SHIP_SIZE_CATEGORY_MAPPINGS, SHIP_SIZE_CATEGORIES);
        ALL_COURSE_OVER_GROUND_CATEGORY_MAPPINGS = initMapping(ALL_COURSE_OVER_GROUND_CATEGORY_MAPPINGS, COG_CATEGORIES);
        ALL_SPEED_OVER_GROUND_CATEGORY_MAPPINGS = initMapping(ALL_SPEED_OVER_GROUND_CATEGORY_MAPPINGS, SOG_CATEGORIES);
    }

    private static ImmutableMap<Short, String> initMapping(Map mapping, String[] categories) {
        mapping = new TreeMap<>();
        for (short c=1; c<=categories.length; c++) {
            mapping.put(c, mapCategoryToString(categories, c));
        }
        return ImmutableMap.copyOf(mapping);
    }

    /**
     * Map AIS ship type to DMA category
     * @param shipType
     * @return
     */
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

        return (short) category;
    }

    /**
     * Map AIS ship length to DMA category
     * @param shipLength
     * @return
     */
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
            throw new IllegalArgumentException("shipLength: " + shipLength);
        }

        return (short) category;
    }

    /**
     * Map course to DMA category
     * @param cog
     * @return
     */
    public static short mapCourseOverGroundToCategory(float cog) {
        cog = cog % (float) 360.0;
        return (short) ((short) (cog / 30) + 1);
    }

    /**
     * Map speed over ground to DMA category
     * @param sog
     * @return
     */
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

        return (short) category;
    }

    /**
     * Map ship type category to English text
     * @param category
     * @return
     */
    public static String mapShipTypeCategoryToString(short category) {
        return mapCategoryToString(SHIP_TYPE_CATEGORIES, category);
    }

    /**
     * Map ship length category to English text
     * @param category
     * @return
     */
    public static String mapShipSizeCategoryToString(short category) {
        return mapCategoryToString(SHIP_SIZE_CATEGORIES, category);
    }

    /**
     * Map course over ground category to text
     * @param category
     * @return
     */
    public static String mapCourseOverGroundCategoryToString(short category) {
        return mapCategoryToString(COG_CATEGORIES, category);
    }

    /**
     * Map speed over ground category to text
     * @param category
     * @return
     */
    public static String mapSpeedOverGroundCategoryToString(short category) {
        return mapCategoryToString(SOG_CATEGORIES, category);
    }

    private static String mapCategoryToString(String[] categories, short category) {
        String categoryAsString;

        if (category >= 1 && category <= categories.length) {
            categoryAsString = categories[category-1];
        } else {
            categoryAsString = String.valueOf(category);
        }

        return categoryAsString;
    }

    public static Map<Short, String> getAllShipTypeCategoryMappings() {
        return ALL_SHIP_TYPE_CATEGORY_MAPPINGS;
    }

    public static Map<Short, String> getAllShipSizeCategoryMappings() {
        return ALL_SHIP_SIZE_CATEGORY_MAPPINGS;
    }

    public static Map<Short, String> getAllCourseOverGroundCategoryMappings() {
        return ALL_COURSE_OVER_GROUND_CATEGORY_MAPPINGS;
    }

    public static Map<Short, String> getAllSpeedOverGroundCategoryMappings() {
        return ALL_SPEED_OVER_GROUND_CATEGORY_MAPPINGS;
    }
}
