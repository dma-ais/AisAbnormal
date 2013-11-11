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

@Immutable
public class AbnormalEventCog extends AbnormalEvent {
    
    private final double cog;

    public AbnormalEventCog(Date time, long shipMmsi, String shipName, ShipType shipType, Position position, String description, double cog) {
        super(time, shipMmsi, shipName, shipType, position, description);
        this.cog = cog;
    }
    
    public double getCog() {
        return cog;
    }

}
