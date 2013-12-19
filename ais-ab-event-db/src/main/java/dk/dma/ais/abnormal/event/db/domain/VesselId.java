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

package dk.dma.ais.abnormal.event.db.domain;

import com.google.common.base.Strings;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class VesselId implements Serializable {

    private String name = "?";
    private int mmsi;
    private int imo;
    private String callsign = "?";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = trimAisString(name);
    }

    public int getMmsi() {
        return mmsi;
    }

    public void setMmsi(int mmsi) {
        this.mmsi = mmsi;
    }

    public int getImo() {
        return imo;
    }

    public void setImo(int imo) {
        this.imo = imo;
    }

    public String getCallsign() {
        return callsign;
    }

    public void setCallsign(String callsign) {
        this.callsign = trimAisString(callsign);
    }

    private static String trimAisString(String name) {
        if (!Strings.isNullOrEmpty(name)) {
            name = name.replace('@', ' ').trim();
        }
        return name;
    }
}
