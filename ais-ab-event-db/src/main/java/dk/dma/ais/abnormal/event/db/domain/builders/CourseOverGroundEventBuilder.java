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

import dk.dma.ais.abnormal.event.db.domain.CourseOverGroundEvent;

/**
 * This builder follows the expression builder pattern
 * http://martinfowler.com/bliki/ExpressionBuilder.html
 */
public class CourseOverGroundEventBuilder extends EventBuilder {

    CourseOverGroundEvent event;

    public CourseOverGroundEventBuilder() {
        event = new CourseOverGroundEvent();
    }

    public static CourseOverGroundEventBuilder CourseOverGroundEvent() {
        return new CourseOverGroundEventBuilder();
    }

    public CourseOverGroundEventBuilder shipLength(int shipLength) {
        event.setShipLength(shipLength);
        return this;
    }

    public CourseOverGroundEventBuilder shipType(int shipType) {
        event.setShipType(shipType);
        return this;
    }

    public CourseOverGroundEventBuilder courseOverGround(int cog) {
        event.setCourseOverGround(cog);
        return this;
    }

    public CourseOverGroundEvent getEvent() {
        return event;
    }

}
