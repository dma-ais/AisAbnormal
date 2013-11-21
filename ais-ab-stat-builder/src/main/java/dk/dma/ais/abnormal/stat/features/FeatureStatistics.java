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

package dk.dma.ais.abnormal.stat.features;

import java.util.HashMap;
import java.util.TreeMap;

public class FeatureStatistics {

    private TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, HashMap<String,Object>>>> data = new TreeMap<>();

    private TreeMap<Integer, HashMap<String,Object>> findLevel2(Integer key1, Integer key2) {
        TreeMap<Integer, TreeMap<Integer, HashMap<String,Object>>> level1 = data.get(key1);
        if (level1 == null) {
            level1 = new TreeMap<>();
            data.put(key1, level1);
        }

        TreeMap<Integer, HashMap<String,Object>> level2 = level1.get(key2);
        if (level2 == null) {
            level2 = new TreeMap<>();
            level1.put(key2, level2);
        }

        return level2;
    }

    public void setStatistics(Integer key1, Integer key2, Integer key3, HashMap<String,Object> statistics) {
        TreeMap<Integer, HashMap<String,Object>> level2 = findLevel2(key1, key2);
        level2.put(key3, statistics);
    }

    public HashMap<String,Object> getStatistics(Integer key1, Integer key2, Integer key3) {
        TreeMap<Integer, HashMap<String,Object>> level2 = findLevel2(key1, key2);
        HashMap<String, Object> statistics = level2.get(key3);
        if (statistics == null) {
            statistics = new HashMap<>();
            level2.put(key3, statistics);
        }
        return statistics;
    }

    public void setStatistic(Integer key1, Integer key2, Integer key3, String statisticName, Object statisticValue) {
        HashMap<String, Object> statistics = getStatistics(key1, key2, key3);
        statistics.put(statisticName, statisticValue);
    }

    public void incrementStatistic(Integer key1, Integer key2, Integer key3, String statisticName) {
        HashMap<String, Object> statistics = getStatistics(key1, key2, key3);
        Object statistic = statistics.get(statisticName);
        if (! (statistic instanceof Integer)) {
            statistic = Integer.valueOf(0);
        }
        statistic = (Integer) statistic + 1;
        statistics.put(statisticName, statistic);
    }

    public Object getStatistic(Integer key1, Integer key2, Integer key3, String statisticName) {
        HashMap<String, Object> statistics = getStatistics(key1, key2, key3);
        return statistics.get(statisticName);
    }

    public Integer getNumberOfLevel1Entries() {
        return this.data.size();
    }
}
