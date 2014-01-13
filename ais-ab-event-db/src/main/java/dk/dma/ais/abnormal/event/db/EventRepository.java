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

package dk.dma.ais.abnormal.event.db;

import dk.dma.ais.abnormal.event.db.domain.Event;

import java.util.Date;
import java.util.List;

/**
 * Database-agnostic interface for the Event repository.
 * Some people would call this a DAO.
 */
public interface EventRepository {
    void save(Event event);

    /**
     * Get an event from the database.
     *
     * @param eventId The id of the event to get.
     * @return the event loaded from the database.
     */
    Event getEvent(long eventId);

    /**
     * Get events (past and ongoing) where active period spans inside from and to parameters, where
     * event type is of given type, and where vessel has given name, callsign or IMO no.
     *
     * @param from
     * @param to
     * @param type
     * @param vessel
     * @return
     */
    List<Event> findEventsByFromAndToAndTypeAndVessel(Date from, Date to, String type, String vessel);

    /**
     * Get all events (past and ongoing) which are active inside the given time period. All event which have a second
     * of its lifespan inside the query time span is included - e.g. events starting before 'from' but ending after
     * 'from', are also included.
     *
     * @param from
     * @param to
     * @return
     */
    List<Event> findEventsByFromAndTo(Date from, Date to);

    /**
     * Find the most recently raised events.
     *
     * @param numberOfEvents
     * @return
     */
    List<Event> findRecentEvents(int numberOfEvents);

    <T extends Event> T findOngoingEventByVessel(int mmsi, Class<T> eventClass);
}
