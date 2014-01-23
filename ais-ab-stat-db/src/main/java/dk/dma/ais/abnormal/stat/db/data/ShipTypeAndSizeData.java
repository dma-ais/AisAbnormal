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

import gnu.trove.iterator.TShortIterator;
import gnu.trove.map.hash.TShortIntHashMap;

import java.util.HashMap;
import java.util.TreeMap;

/**
 *
 * This is a memory-consumption optimised implementation of ThreeKeyMap intended to store
 * AIS feature statistics of type ShipTypeAndSizeData for one grid cell.
 *
 */
public class ShipTypeAndSizeData implements FeatureData, ThreeKeyMap {

    private TShortIntHashMap data = new TShortIntHashMap(1);

    private static final int MAX_KEY_1 = 9;
    private static final int MAX_KEY_2 = 9;

    private static final String MEANING_OF_KEY_1 = "shipType";
    private static final String MEANING_OF_KEY_2 = "shipSize";
    private static final String MEANING_OF_KEY_3 = "statName";

    public static final String STAT_SHIP_COUNT = "shipCount";

    public ShipTypeAndSizeData() {
    }

    @Override
    @SuppressWarnings("unused")
    public String getMeaningOfKey1() {
        return MEANING_OF_KEY_1;
    }

    @Override
    @SuppressWarnings("unused")
    public String getMeaningOfKey2() {
        return MEANING_OF_KEY_2;
    }

    @Override
    @SuppressWarnings("unused")
    public String getMeaningOfKey3() {
        return MEANING_OF_KEY_3;
    }

    @Override
    public String getFeatureName() {
        return ShipTypeAndSizeData.class.getSimpleName();
    }

    @Override
    public String getFeatureDataType() {
        return ThreeKeyMap.class.getSimpleName();
    }

    @Override
    public TreeMap<Integer, TreeMap<Integer, HashMap<String, Integer>>> getData() {
        TreeMap<Integer, TreeMap<Integer, HashMap<String, Integer>>> root = new TreeMap<>();

        TShortIterator keys = data.keySet().iterator();
        while(keys.hasNext()) {
            short key = keys.next();
            int shipTypeBucket = extractKey1(key);
            int shipSizeBucket = extractKey2(key);

            TreeMap<Integer, HashMap<String, Integer>> level1 = root.get(shipTypeBucket);
            if (level1 == null) {
                level1 = new TreeMap<>();
                root.put(shipTypeBucket, level1);
            }
            HashMap<String, Integer> statistics = level1.get(shipSizeBucket);
            if (statistics == null) {
                statistics = new HashMap<>();
                level1.put(shipSizeBucket, statistics);
            }
            String statisticsName = STAT_SHIP_COUNT;
            Integer statisticsValue = getValue(shipTypeBucket, shipSizeBucket, statisticsName);
            if (statisticsValue != null) {
                statistics.put(statisticsName, statisticsValue);
            }
        }

        return root;
    }

    @Override
    public void incrementValue(int shipTypeBucket, int shipSizeBucket, String key3) {
        short key = computeMapKey(shipTypeBucket, shipSizeBucket, key3);
        if (data.get(key) != data.getNoEntryValue()) {
            data.increment(key);
        } else {
            data.put(key, 1);
            data.compact();
        }
    }

    @Override
    public void setValue(int shipTypeBucket, int shipSizeBucket, String key3, int value) {
        short key = computeMapKey(shipTypeBucket, shipSizeBucket, key3);
        data.put(key, value);
        data.compact();
    }

    @Override
    public Integer getValue(int shipTypeBucket, int shipSizeBucket, String key3) {
        short key = computeMapKey(shipTypeBucket, shipSizeBucket, key3);

        Integer statisticsValue = data.get(key);
        statisticsValue = statisticsValue == data.getNoEntryValue() ? null : statisticsValue;

        return statisticsValue;
    }

    @Override
    public int getSumFor(String key3) {
        Integer sum = 0;

        for (short key1=0; key1<MAX_KEY_1; key1++) {
            for (short key2=0; key2<MAX_KEY_2; key2++) {
                short key = computeMapKey(key1, key2, key3);
                sum += data.get(key);
            }
        }

        return sum;
    }

    static short computeMapKey(int key1, int key2, String statisticName) {
        if (key1 > MAX_KEY_1) {
            throw new IllegalArgumentException("key1 must be 0-" + MAX_KEY_1 + ".");
        }
        if (key2 > MAX_KEY_2) {
            throw new IllegalArgumentException("key2 must be 0-" + MAX_KEY_2 + ".");
        }
        if (! STAT_SHIP_COUNT.equals(statisticName)) {
            throw new IllegalArgumentException("Statistic name '" + statisticName + "' is not supported.");
        }
        return (short) (key1*(MAX_KEY_1+1) + key2);
    }

    static int extractStatisticId(short key) {
        return 0;
    }

    static int extractKey1(short key) {
        return (key-extractKey2(key))/(MAX_KEY_1+1);
    }

    static int extractKey2(short key) {
        return key % (MAX_KEY_1+1);
    }


}
