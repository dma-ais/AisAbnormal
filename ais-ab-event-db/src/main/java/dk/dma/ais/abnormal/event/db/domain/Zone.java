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
import java.util.Date;

/**
 * The defining parameters of an oval.
 */
@Entity
public class Zone  {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @NotNull
    private Date targetTimestamp;

    @NotNull
    private Double centerLatitude;

    @NotNull
    private Double centerLongitude;

    @NotNull
    private Double majorAxisHeading;

    @NotNull
    private Double majorSemiAxisLength;

    @NotNull
    private Double minorSemiAxisLength;

    public Zone() {
    }

    public void setTargetTimestamp(Date targetTimestamp) {
        this.targetTimestamp = targetTimestamp;
    }

    public void setCenterLatitude(Double centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    public void setCenterLongitude(Double centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    public void setMajorAxisHeading(Double majorAxisHeading) {
        this.majorAxisHeading = majorAxisHeading;
    }

    public void setMajorSemiAxisLength(Double majorSemiAxisLength) {
        this.majorSemiAxisLength = majorSemiAxisLength;
    }

    public void setMinorSemiAxisLength(Double minorSemiAxisLength) {
        this.minorSemiAxisLength = minorSemiAxisLength;
    }

    public long getId() {
        return id;
    }

    public Date getTargetTimestamp() {
        return targetTimestamp;
    }

    public Double getCenterLatitude() {
        return centerLatitude;
    }

    public Double getCenterLongitude() {
        return centerLongitude;
    }

    public Double getMajorAxisHeading() {
        return majorAxisHeading;
    }

    public Double getMajorSemiAxisLength() {
        return majorSemiAxisLength;
    }

    public Double getMinorSemiAxisLength() {
        return minorSemiAxisLength;
    }
}