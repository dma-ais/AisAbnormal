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

import com.google.common.base.MoreObjects;

import javax.persistence.Entity;

/**
 * An event
 */
@Entity
public class CourseOverGroundEvent extends Event {

    private int shipType;
    private int shipLength;
    private int courseOverGround;

    public int getShipType() {
        return shipType;
    }

    public void setShipType(int shipType) {
        this.shipType = shipType;
    }

    public int getShipLength() {
        return shipLength;
    }

    public void setShipLength(int shipLength) {
        this.shipLength = shipLength;
    }

    public int getCourseOverGround() {
        return courseOverGround;
    }

    public void setCourseOverGround(int courseOverGround) {
        this.courseOverGround = courseOverGround;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("shipType", shipType)
                .add("shipLength", shipLength)
                .add("courseOverGround", courseOverGround)
                .toString();
    }

    public CourseOverGroundEvent() {
    }
}
