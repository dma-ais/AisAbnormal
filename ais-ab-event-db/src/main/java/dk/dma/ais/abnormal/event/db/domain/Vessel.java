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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
public class Vessel implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    /** Vessel's MMSI number */
    @NotNull
    private int mmsi;

    /** Vessel's hull name */
    private String name;

    /** Vessel's IMO number */
    private Integer imo;

    /** Vessel's international radio call sign */
    private String callsign;

    /** Vessel's type (as per ITU 1371) */
    private Integer type;

    /** Vessel's dimensions in metres; measured from reported position reference / position sensor location */
    private Integer toBow, toStern, toPort, toStarboard;

    public Vessel() {
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMmsi() {
        return mmsi;
    }

    public void setMmsi(int mmsi) {
        this.mmsi = mmsi;
    }

    public Integer getImo() {
        return imo;
    }

    public void setImo(Integer imo) {
        this.imo = imo;
    }

    public String getCallsign() {
        return callsign;
    }

    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    /* TODO: Obsoleted by toBow and toStern */
    public Integer getLength() {
        return toBow == null || toStern == null ? null : toBow + toStern;
    }

    public Integer getToBow() {
        return toBow;
    }

    public void setToBow(Integer toBow) {
        this.toBow = toBow;
    }

    public Integer getToStern() {
        return toStern;
    }

    public void setToStern(Integer toStern) {
        this.toStern = toStern;
    }

    public Integer getToPort() {
        return toPort;
    }

    public void setToPort(Integer toPort) {
        this.toPort = toPort;
    }

    public Integer getToStarboard() {
        return toStarboard;
    }

    public void setToStarboard(Integer toStarboard) {
        this.toStarboard = toStarboard;
    }
}
