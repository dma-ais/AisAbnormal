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
package dk.dma.ais.abnormal.rest.stat;

import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import dk.dma.ais.abnormal.event.AbnormalEventCog;
import dk.dma.commons.web.rest.AbstractResource;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.model.ship.ShipType;


@Path("/stat")
public class StatServices extends AbstractResource {
    
    @GET
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    public AbnormalEventCog test() {
        return new AbnormalEventCog(new Date(), 999999999, "MARTHA", ShipType.CARGO, Position.create(55, 12), "dasdda", 100); 
    }

}
