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

import com.google.common.primitives.Ints;
import gnu.trove.iterator.TShortIterator;
import gnu.trove.map.hash.TShortIntHashMap;

import java.util.HashMap;
import java.util.TreeMap;

/**
 *
 * This is a memory-consumption optimised implementation of FourKeyMap intended to store
 * AIS feature statistics for one grid cell.
 *
 */
public abstract class FourKeyFeatureData implements FeatureData, FourKeyMap {

    private final TShortIntHashMap data;

    final byte MAX_KEY_1;
    final byte MAX_KEY_2;
    final byte MAX_KEY_3;

    protected FourKeyFeatureData(int maxKey1, int maxKey2, int maxKey3, int maxNumKey4) {
        if (maxKey1 <= 0) {
            throw new IllegalArgumentException("maxKey1 <= 0 not supported.");
        }
        if (maxKey2 <= 0) {
            throw new IllegalArgumentException("maxKey2 <= 0 not supported.");
        }
        if (maxKey3 <= 0) {
            throw new IllegalArgumentException("maxKey3 <= 0 not supported.");
        }
        if (maxNumKey4 != 1) {
            throw new IllegalArgumentException("maxNumKey4 != 1 not supported.");
        }

        this.MAX_KEY_1 = (byte) maxKey1;
        this.MAX_KEY_2 = (byte) maxKey2;
        this.MAX_KEY_3 = (byte) maxKey3;

        this.data = new TShortIntHashMap(1);
    }

    @Override
    public void incrementValue(int key1, int key2, int key3, String key4) {
        short key = computeMapKey(key1, key2, key3, key4);
        if (data.get(key) != data.getNoEntryValue()) {
            data.increment(key);
        } else {
            data.put(key, 1);
            data.compact();
        }
    }

    @Override
    public void setValue(int key1, int key2, int key3, String key4, int value) {
        short key = computeMapKey(key1, key2, key3, key4);
        data.put(key, value);
        data.compact();
    }

    @Override
    public Integer getValue(int key1, int key2, int key3, String key4) {
        short key = computeMapKey(key1, key2, key3, key4);
        Integer statisticsValue = data.get(key);
        statisticsValue = statisticsValue == data.getNoEntryValue() ? null : statisticsValue;
        return statisticsValue;
    }

    @Override
    public int getSumFor(String key4) {
        return Ints.asList(data.values()).stream().mapToInt(value -> value).sum();
    }

    @Override
    public String getFeatureName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getFeatureDataType() {
        return FourKeyMap.class.getSimpleName();
    }

    @Override
    public TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, HashMap<String, Integer>>>> getData() {
        TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, HashMap<String, Integer>>>> root = new TreeMap<>();

        TShortIterator keys = data.keySet().iterator();
        while(keys.hasNext()) {
            short key = keys.next();
            int key1 = extractKey1(key);
            int key2 = extractKey2(key);
            int key3 = extractKey3(key);
            int bucket1 = key1 + 1;
            int bucket2 = key2 + 1;
            int bucket3 = key3 + 1;

            TreeMap<Integer, TreeMap<Integer, HashMap<String, Integer>>> level1 = root.get(bucket1);
            if (level1 == null) {
                level1 = new TreeMap<>();
                root.put(bucket1, level1);
            }

            TreeMap<Integer, HashMap<String, Integer>> level2 = level1.get(bucket2);
            if (level2 == null) {
                level2 = new TreeMap<>();
                level1.put(bucket2, level2);
            }

            HashMap<String, Integer> level3 = level2.get(bucket3);
            if (level3 == null) {
                level3 = new HashMap<>();
                level2.put(bucket3, level3);
            }

            Integer value = getValue(key1, key2, key3, getNameOfOnlySupportedValueOfKey4());
            if (value != null) {
                level3.put(getNameOfOnlySupportedValueOfKey4(), value);
            }
        }

        return root;
    }

    protected abstract String getNameOfOnlySupportedValueOfKey4();

    short computeMapKey(int key1, int key2, int key3, String key4) {
        if (key1 > MAX_KEY_1) {
            throw new IllegalArgumentException("key1 must be 0-" + MAX_KEY_1 + " - not " + key1 + ".");
        }
        if (key2 > MAX_KEY_2) {
            throw new IllegalArgumentException("key2 must be 0-" + MAX_KEY_2 + " - not " + key2 + ".");
        }
        if (key3 > MAX_KEY_3) {
            throw new IllegalArgumentException("key3 must be 0-" + MAX_KEY_3 + " - not " + key3 + ".");
        }
        if (! getNameOfOnlySupportedValueOfKey4().equals(key4)) {
            throw new IllegalArgumentException("key4 '" + key4 + "' is not supported.");
        }

        final int d2 = MAX_KEY_2 + 1;
        final int d3 = MAX_KEY_3 + 1;

        return (short) (key1*d2*d3 + key2*d3 + key3);
    }

    int extractKey1(short key) {
        final int d2 = MAX_KEY_2 + 1;
        final int d3 = MAX_KEY_3 + 1;

        return key / (d2*d3);
    }

    int extractKey2(short key) {
        final int d2 = MAX_KEY_2 + 1;
        final int d3 = MAX_KEY_3 + 1;

        return (key - extractKey1(key)*d2*d3) / d3;
    }

    int extractKey3(short key) {
        final int d2 = MAX_KEY_2 + 1;
        final int d3 = MAX_KEY_3 + 1;

        return (key - extractKey1(key)*d2*d3 - extractKey2(key)*d3);
    }

    int extractKey4(short key) {
        return 0;
    }

}
