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
        Event event = eventRepository.getEvent(id);
        return event;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Object get(@QueryParam("from") DateParameter from, @QueryParam("to") DateParameter to, @QueryParam("type") String type, @QueryParam("vessel") String vessel, @QueryParam("numberOfReventEvents") Integer numberOfReventEvents) {
        if (numberOfReventEvents != null) {
            return eventRepository.findRecentEvents(numberOfReventEvents);
        } else {
            return eventRepository.findEventsByFromAndToAndTypeAndVessel(from == null ? null : from.value(), to == null ? null : to.value(), type, vessel);
        }
    }
}
