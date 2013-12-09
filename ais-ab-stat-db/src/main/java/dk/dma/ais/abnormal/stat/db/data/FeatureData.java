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
 * Feature data is a set of statistics calculated for a certain feature.
 */
public interface FeatureData extends Serializable {

    /**
     * Get the name of the feature which produced these data.
     * @return the name of the feature which produced these feature data.
     */
    String getFeatureName();

    /**
     * Get the simple name this class.
     * @return the name of this feature class.
     */
    @SuppressWarnings("unused")
    String getFeatureDataType();

    /**
     * Get the internal data structure of this future.
     * Intended for use with JSON serialization.
     * @return the actual feature data.
     */
    Object getData();

}
