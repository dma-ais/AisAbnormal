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

package dk.dma.ais.abnormal.tracker;

import java.util.HashMap;
import java.util.Map;

public final class Track {

    public static final String TIMESTAMP = "lastUpdate";
    public static final String POSITION = "position";
    public static final String SPEED_OVER_GROUND = "sog";
    public static final String COURSE_OVER_GROUND = "cog";
    public static final String CELL_ID = "cellId";
    public static final String SHIP_TYPE = "vesselType";
    public static final String VESSEL_LENGTH = "vesselLength";
    public static final String SHIP_NAME = "vesselName";
    public static final String IMO = "imo";
    public static final String CALLSIGN = "callsign";
    public static final String POSITION_IS_INTERPOLATED = "posEst";

    private final Integer mmsi;
    private final Map<String, Object> properties = new HashMap<>(10);

    public Track(Integer mmsi) {
        this.mmsi = mmsi;
    }

    public Integer getMmsi() {
        return mmsi;
    }

    public Object getProperty(String propertyName) {
        return properties.get(propertyName);
    }

    public void setProperty(String propertyName, Object propertyValue) {
        properties.put(propertyName, propertyValue);
    }

    public void removeProperty(String propertyName) {
        properties.remove(propertyName);
    }
}
