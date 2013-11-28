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

public final class DatasetMetaData implements Serializable {

    // Time of first ais message
    // Time of last ais message

    public DatasetMetaData(Double gridResolution, Integer downSampling) {
        this.gridResolution = gridResolution;
        this.downSampling = downSampling;
    }

    public Short getFormatVersion() {
        return formatVersion;
    }

    public Double getGridResolution() {
        return gridResolution;
    }

    public Integer getDownsampling() {
        return downSampling;
    }

    private final Short   formatVersion = 1;
    private final Double  gridResolution;
    private final Integer downSampling;

}
