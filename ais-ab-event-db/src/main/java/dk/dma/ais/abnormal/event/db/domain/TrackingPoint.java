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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import java.util.Date;

/**
 * A historic position recorded in conjunction with an abnormal event.
 */
@Entity
public class TrackingPoint implements Comparable<TrackingPoint> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @NotNull
    @Past
    private Date timestamp;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    @Column(precision=4, scale=2)
    private Float speedOverGround;

    @Column(precision=4, scale=2)
    private Float courseOverGround;

    @NotNull
    private Boolean positionInterpolated;

    @Override
    public int compareTo(TrackingPoint otherPosition) {
        return timestamp.compareTo(otherPosition.timestamp);
    }

    public long getId() {
        return id;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Float getSpeedOverGround() {
        return speedOverGround;
    }

    public void setSpeedOverGround(Float speedOverGround) {
        this.speedOverGround = speedOverGround;
    }

    public Float getCourseOverGround() {
        return courseOverGround;
    }

    public void setCourseOverGround(Float courseOverGround) {
        this.courseOverGround = courseOverGround;
    }

    public void setPositionInterpolated(Boolean positionInterpolated) {
        this.positionInterpolated = positionInterpolated;
    }

    public Boolean getPositionInterpolated() {
        return positionInterpolated;
    }
}
