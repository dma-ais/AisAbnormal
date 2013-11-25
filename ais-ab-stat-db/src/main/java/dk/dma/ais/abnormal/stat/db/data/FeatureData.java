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

import java.io.Serializable;
import java.util.HashMap;
import java.util.TreeMap;

public class FeatureData implements Serializable {

    private TreeMap<Short, TreeMap<Short, HashMap<String,Object>>> data = new TreeMap<>();

    public FeatureData() {
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
            statistic = Integer.valueOf(0);
        }
        statistic = (Integer) statistic + 1;
        statistics.put(statisticName, statistic);
    }

    public Integer getNumberOfLevel1Entries() {
        return this.data.size();
    }

    /*
    public void printStatistics(PrintStream stream) {
        for (Integer key1 : data.keySet()) {
            TreeMap<Integer, TreeMap<Integer, HashMap<String, Object>>> level2 = data.getFeatureData(key1);
            for (Integer key2 : level2.keySet()) {
                TreeMap<Integer, HashMap<String, Object>> level3 = level2.getFeatureData(key2);
                for (Integer key3 : level3.keySet()) {
                    HashMap<String, Object> statistics = level3.getFeatureData(key3);
                    for (String statisticsName : statistics.keySet()) {
                        Object statisticsValue =  statistics.getFeatureData(statisticsName);
                        String output = String.format("%12d | %9d | %9d | %-16s | %9s", key1, key2, key3, statisticsName, statisticsValue);
                        stream.println(output);
                    }
                }
            }
        }
    }
    */

}
