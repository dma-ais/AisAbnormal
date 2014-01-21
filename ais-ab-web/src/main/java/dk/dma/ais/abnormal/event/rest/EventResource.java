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

package dk.dma.ais.abnormal.event.rest;

import com.google.inject.Inject;
import com.google.inject.servlet.RequestScoped;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.rest.parameters.DateParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@RequestScoped
@Path("/event")
public class EventResource {
    private static final Logger LOG = LoggerFactory.getLogger(EventResource.class);
    static {
        LOG.debug("EventResource loaded.");
    }
    {
        LOG.debug(this.getClass().getSimpleName() + " created (" + this + " ).");
    }

    private EventRepository eventRepository;

    @Inject
    public EventResource(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    public Event get(@PathParam("id") int id) {
        return eventRepository.getEvent(id);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/type")
    public List<String> get() {
        return eventRepository.getEventTypes();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Object get(
            @QueryParam("from") DateParameter from,
            @QueryParam("to") DateParameter to,
            @QueryParam("type") String type,
            @QueryParam("vessel") String vessel,
            @QueryParam("numberOfRecentEvents") Integer numberOfRecentEvents,
            @QueryParam("north") Double north,
            @QueryParam("east") Double east,
            @QueryParam("south") Double south,
            @QueryParam("west") Double west
        ) {

        // Check validity of parameters and parameter combinations
        if (north != null || east != null || south != null || west != null ) {
            if (! (north != null && east != null && south != null && west != null)) {
                throw new IllegalArgumentException("Most provide all of north, east, south, west.");
            }
        }

        if (numberOfRecentEvents != null) {
            if (from != null || to != null || type != null || vessel != null || north != null) {
                throw new IllegalArgumentException("Parameter 'numberOfRecentEvents' cannot be used in combination with other parameters.");
            }
        }

        // Figure out which service method to call
        if (numberOfRecentEvents != null) {
            return eventRepository.findRecentEvents(numberOfRecentEvents);
        } else {
            return eventRepository.findEventsByFromAndToAndTypeAndVesselAndArea(from == null ? null : from.value(), to == null ? null : to.value(), type, vessel, north, east, south, west);
        }
    }
}
