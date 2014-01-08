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

import dk.dma.ais.abnormal.event.db.domain.Behaviour;
import dk.dma.ais.abnormal.event.db.domain.TrackingPoint;

public class BehaviourBuilder {

    Behaviour behaviour;
    EventBuilder eventBuilder;

    public BehaviourBuilder(EventBuilder eventBuilder) {
        this.behaviour = new Behaviour();
        this.eventBuilder = eventBuilder;
    }

    public BehaviourBuilder() {
        this.behaviour = new Behaviour();
    }

    public static BehaviourBuilder Behaviour() {
        return new BehaviourBuilder();
    }

    public VesselBuilder vessel() {
        VesselBuilder builder = new VesselBuilder(this);
        behaviour.setVessel(builder.getVessel());
        return builder;
    }

    public TrackingPointBuilder trackingPoint(){
        TrackingPointBuilder builder = new TrackingPointBuilder(this);
        behaviour.addTrackingPoint(builder.trackingPoint);
        return builder;
    }

    public BehaviourBuilder trackingPoints(TrackingPoint... trackingPoints){
        for (TrackingPoint trackingPoint : trackingPoints) {
            behaviour.addTrackingPoint(trackingPoint);
        }
        return this;
    }

    public Behaviour getBehaviour() {
        return behaviour;
    }

}
