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
import dk.dma.ais.abnormal.event.db.domain.Position;

public class BehaviourBuilder {

    Behaviour behaviour;
    VesselBuilder vesselBuilder;

    public BehaviourBuilder(VesselBuilder vesselBuilder) {
        this.behaviour = new Behaviour();
        this.vesselBuilder = vesselBuilder;
    }

    public BehaviourBuilder() {
        this.behaviour = new Behaviour();
    }

    public static BehaviourBuilder Behaviour() {
        return new BehaviourBuilder();
    }

    public PositionBuilder position(){
        PositionBuilder builder = new PositionBuilder(this);
        behaviour.addPosition(builder.getPosition());
        return builder;
    }

    public BehaviourBuilder positions(Position... positions){
        for (Position position : positions) {
            behaviour.addPosition(position);
        }
        return this;
    }

    public Behaviour getBehaviour() {
        return behaviour;
    }

}
