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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * A historic position recorded in conjunction with an abnormal event.
 */
@Table(
    indexes = {
        @Index(name="INDEX_TRACKINGPOINT_LATITUDE", columnList = "latitude"),
        @Index(name="INDEX_TRACKINGPOINT_LONGITUDE", columnList = "longitude")
    }
)
@Entity
public class TrackingPoint implements Comparable<TrackingPoint> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @NotNull
    private LocalDateTime timestamp;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    @Column(precision=4, scale=2)
    private Float speedOverGround;

    @Column(precision=4, scale=2)
    private Float courseOverGround;

    @Column(precision=4, scale=2)
    private Float trueHeading;

    @NotNull
    private Boolean positionInterpolated;

    @NotNull
    @Enumerated(EnumType.ORDINAL)
    private EventCertainty eventCertainty;

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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
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

    public Float getTrueHeading() {
        return trueHeading;
    }

    public void setTrueHeading(Float trueHeading) {
        this.trueHeading = trueHeading;
    }

    public void setPositionInterpolated(Boolean positionInterpolated) {
        this.positionInterpolated = positionInterpolated;
    }

    public Boolean getPositionInterpolated() {
        return positionInterpolated;
    }

    public void setEventCertainty(EventCertainty eventCertainty) {
        this.eventCertainty = eventCertainty;
    }

    public EventCertainty getEventCertainty() {
        return eventCertainty;
    }

    public enum EventCertainty {
        UNDEFINED(0), LOWERED(1), UNCERTAIN(2), RAISED(3);

        public static EventCertainty create(int certainty) {
            return EventCertainty.values()[certainty];
        }

        private int certainty;

        public int getCertainty() {
            return certainty;
        }

        EventCertainty(int certainty) {
            this.certainty = certainty;
        }
    }

}
