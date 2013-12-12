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

import java.util.HashMap;
import java.util.TreeMap;

public class FeatureData2Key implements FeatureData {

    // TODO change implementation to com.google.common.collect.<sometable>
    private TreeMap<Short, TreeMap<Short, HashMap<String,Object>>> data = new TreeMap<>();

    private final String featureClassName;
    private final String meaningOfKey1;
    private final String meaningOfKey2;

    public FeatureData2Key(Class featureClass, String meaningOfKey1, String meaningOfKey2) {
        this.featureClassName = featureClass.getCanonicalName();
        this.meaningOfKey1 = meaningOfKey1;
        this.meaningOfKey2 = meaningOfKey2;
    }

    public Integer numberOfLevel1Entries() {
        return this.data.size();
    }

    public HashMap<String,Object> getStatistics(Short key1, Short key2) {
        HashMap<String,Object> statistics = null;

        TreeMap<Short, HashMap<String, Object>> level1 = data.get(key1);
        if (level1 != null) {
            statistics = level1.get(key2);
        }

        return statistics;
    }

    public Object getStatistic(Short key1, Short key2, String statisticName) {
        HashMap<String, Object> statistics = getStatistics(key1, key2);
        return statistics == null ? null : statistics.get(statisticName);
    }

    public void setStatistic(Short key1, Short key2, String statisticName, Object statisticValue) {
        TreeMap<Short, HashMap<String, Object>> level1 = data.get(key1);
        if (level1 == null) {
            level1 = new TreeMap<>();
            data.put(key1, level1);
        }

        HashMap<String, Object> statistics = level1.get(key2);
        if (statistics == null) {
            statistics = new HashMap<>();
            level1.put(key2, statistics);
        }

        statistics.put(statisticName, statisticValue);
    }

    public void incrementStatistic(Short key1, Short key2, String statisticName) {
        TreeMap<Short, HashMap<String, Object>> level1 = data.get(key1);
        if (level1 == null) {
            level1 = new TreeMap<>();
            data.put(key1, level1);
        }

        HashMap<String, Object> statistics = level1.get(key2);
        if (statistics == null) {
            statistics = new HashMap<>();
            level1.put(key2, statistics);
        }

        Object statistic = statistics.get(statisticName);
        if (! (statistic instanceof Integer)) {
            statistic = 0;
        }
        statistic = (Integer) statistic + 1;
        statistics.put(statisticName, statistic);
    }

    @Override
    public String getFeatureName() {
        final int n = featureClassName.lastIndexOf(".");
        return featureClassName.substring(n+1);
    }

    @Override
    public String getFeatureDataType() {
        return this.getClass().getSimpleName();
    }

    @Override
    public TreeMap<Short, TreeMap<Short, HashMap<String,Object>>> getData() {
        return data;
    }

    @SuppressWarnings("unused")
    public String getMeaningOfKey1() {
        return meaningOfKey1;
    }

    @SuppressWarnings("unused")
    public String getMeaningOfKey2() {
        return meaningOfKey2;
    }

}
