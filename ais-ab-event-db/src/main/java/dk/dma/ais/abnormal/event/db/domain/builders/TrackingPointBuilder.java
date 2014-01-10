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

package dk.dma.ais.abnormal.event.db.domain.builders;

import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.TrackingPoint;

import java.util.Date;

public class TrackingPointBuilder {

    TrackingPoint trackingPoint;
    BehaviourBuilder behaviourBuilder;

    public TrackingPointBuilder() {
        trackingPoint = new TrackingPoint();
    }

    public TrackingPointBuilder(BehaviourBuilder behaviourBuilder) {
        trackingPoint = new TrackingPoint();
        this.behaviourBuilder = behaviourBuilder;
    }

    public static TrackingPointBuilder TrackingPoint(){
        return new TrackingPointBuilder();
    }

    public TrackingPointBuilder timestamp(Date timestamp) {
        trackingPoint.setTimestamp(timestamp);
        return this;
    }

    public TrackingPointBuilder latitude(Double latitude) {
        trackingPoint.setLatitude(latitude);
        return this;
    }

    public TrackingPointBuilder longitude(Double longitude) {
        trackingPoint.setLongitude(longitude);
        return this;
    }

    public TrackingPointBuilder positionInterpolated(Boolean interpolated) {
        trackingPoint.setPositionInterpolated(interpolated);
        return this;
    }

    public TrackingPointBuilder speedOverGround(Float speedOverGround) {
        trackingPoint.setSpeedOverGround(speedOverGround);
        return this;
    }

    public TrackingPointBuilder courseOverGround(Float courseOverGround) {
        trackingPoint.setCourseOverGround(courseOverGround);
        return this;
    }

    public Event getEvent() {
        return this.behaviourBuilder.eventBuilder.getEvent();
    }

    public TrackingPoint getTrackingPoint() {
        return trackingPoint;
    }

}
