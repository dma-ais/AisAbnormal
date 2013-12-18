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
import dk.dma.ais.abnormal.event.db.domain.Position;

import java.util.Date;

public class PositionBuilder {

    Position position;
    BehaviourBuilder behaviourBuilder;

    public PositionBuilder() {
        position = new Position();
    }

    public PositionBuilder(BehaviourBuilder behaviourBuilder) {
        position = new Position();
        this.behaviourBuilder = behaviourBuilder;
    }

    public static PositionBuilder Position(){
        return new PositionBuilder();
    }

    public PositionBuilder latitude(float latitude) {
        position.setLatitude(latitude);
        return this;
    }

    public PositionBuilder longitude(float longitude) {
        position.setLongitude(longitude);
        return this;
    }

    public PositionBuilder timestamp(Date timestamp) {
        position.setTimestamp(timestamp);
        return this;
    }

    public Position getPosition() {
        return position;
    }

    public Event buildEvent() {
        return this.behaviourBuilder.vesselBuilder.eventBuilder.getEvent();
    }
}
