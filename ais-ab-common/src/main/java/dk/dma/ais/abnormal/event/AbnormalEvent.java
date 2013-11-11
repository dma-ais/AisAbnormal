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
package dk.dma.ais.abnormal.event;

import java.util.Date;

import net.jcip.annotations.Immutable;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.model.ship.ShipType;

/**
 * Abstract base for all abnormal events
 */
@Immutable
public abstract class AbnormalEvent {

    private final Date time;
    private final long shipMmsi;
    private final String shipName;
    private final ShipType shipType;
    private final Position position;
    private final String description;

    public AbnormalEvent(Date time, long shipMmsi, String shipName, ShipType shipType, Position position, String description) {
        super();
        this.time = time;
        this.shipMmsi = shipMmsi;
        this.shipName = shipName;
        this.shipType = shipType;
        this.position = position;
        this.description = description;
    }

    public Date getTime() {
        return time;
    }

    public long getShipMmsi() {
        return shipMmsi;
    }

    public String getShipName() {
        return shipName;
    }

    public ShipType getShipType() {
        return shipType;
    }

    public Position getPosition() {
        return position;
    }

    public String getDescription() {
        return description;
    }

}
