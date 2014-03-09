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

/**
 * Statistic data is a set of statistics calculated for a certain statistic.
 */
public interface StatisticData extends Serializable {

    /**
     * Get the name of the statistic which produced these data.
     * @return the name of the statistic which produced these statistic data.
     */
    String getStatisticName();

    /**
     * Get the name of the statistic type / data structure.
     * @return the name of the statistic type / data structure.
     */
    String getStatisticDataType();

    /**
     * Get the internal data structure of this future.
     * Intended for use with JSON serialization.
     * @return the actual statistic data.
     */
    Object getData();

}
