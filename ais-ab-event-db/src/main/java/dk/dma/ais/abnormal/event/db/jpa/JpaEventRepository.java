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

package dk.dma.ais.abnormal.event.db.jpa;

import com.google.inject.Inject;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.domain.Event;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

import static java.lang.System.currentTimeMillis;

/**
 * JpaEventRepository is an implementation of the EventRepository interface which
 * manages persistent Event objects in a relational database accessed via Hibernate.
 */
@SuppressWarnings("JpaQlInspection")
public class JpaEventRepository implements EventRepository {

    private static final Logger LOG = LoggerFactory.getLogger(JpaEventRepository.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final SessionFactory sessionFactory;
    private final boolean readonly;

    @Inject
    public JpaEventRepository(SessionFactory sessionFactory, boolean readonly) {
        this.readonly = readonly;
        this.sessionFactory = sessionFactory;
    }

    @Override
    protected void finalize() {
        LOG.info("Closing database session factory.");
        sessionFactory.close();
    }

    private Session getSession() {
        Session session = sessionFactory.openSession();
        if (readonly) {
            session.setDefaultReadOnly(true);
        }

        return session;
    }

    @Override
    public List<String> getEventTypes() {
        Session session = getSession();

        List events = null;
        try {
            Query query = session.createQuery("SELECT DISTINCT e.class AS c FROM Event e WHERE e.suppressed=false ORDER BY c");
            events = query.list();
        } finally {
            session.close();
        }

        return events;
    }

    @Override
    public void save(Event event) {
        Session session = getSession();
        try {
            session.beginTransaction();
            session.saveOrUpdate(event);
            session.getTransaction().commit();
        } finally {
            session.close();
        }
    }

    @Override
    public Event getEvent(long eventId) {
        Event event;
        Session session = getSession();
        try {
            event = (Event) session.get(Event.class, eventId);
        } finally {
            session.close();
        }
        return event;
    }

    @Override
    public List<Event> findEventsByFromAndToAndTypeAndVesselAndArea(Date from, Date to, String type, String vessel, Double north, Double east, Double south, Double west) {
        Session session = getSession();

        boolean usesFrom = false, usesTo = false, usesType = false, usesVessel = false, usesArea = false;

        List events = null;
        try {
            StringBuilder hql = new StringBuilder();

            hql.append("SELECT DISTINCT e FROM Event e ");

            if (! StringUtils.isBlank(vessel)) {
                hql.append("LEFT JOIN e.behaviours AS b ");
            }

            if (north != null && east != null && south != null && west != null) {
                hql.append("LEFT JOIN e.behaviours AS b LEFT JOIN b.trackingPoints AS tp WHERE e.suppressed=false AND latitude<:north AND latitude>:south AND longitude<:east AND longitude>:west AND ");
                usesArea = true;
            } else {
                hql.append("WHERE e.suppressed=false AND ");
            }

            // from
            if (from != null) {
                hql.append("(e.startTime >= :from OR e.endTime >= :from) AND ");
                usesFrom = true;
            }

            // to
            if (to != null) {
                hql.append("(e.startTime <= :to OR e.endTime <= :to) AND ");
                usesTo = true;
            }

            // type
            if (! StringUtils.isBlank(type)) {
                hql.append("TYPE(e) IN (:classes) AND ");
                usesType = true;
            }

            // vessel
            if (! StringUtils.isBlank(vessel)) {
                hql.append("(");
                hql.append("b.vessel.callsign LIKE :vessel OR ");
                hql.append("b.vessel.name LIKE :vessel OR ");
                try {
                    Long vesselAsLong = Long.valueOf(vessel);
                    hql.append("b.vessel.mmsi = :vessel OR ");
                    hql.append("b.vessel.imo = :vessel OR ");
                } catch (NumberFormatException e) {
                }
                hql.replace(hql.length()-3, hql.length(), ")"); // "OR " -> ")"
                if (! vessel.startsWith("%")) {
                    vessel = "%" + vessel;
                }
                if (! vessel.endsWith("%")) {
                    vessel = vessel + "%";
                }
                usesVessel = true;
            }

            //
            String hqlAsString = hql.toString().trim();
            if (hqlAsString.endsWith("AND")) {
                hqlAsString = hqlAsString.substring(0, hqlAsString.lastIndexOf("AND"));
            }

            //
            Query query = session.createQuery(hqlAsString);
            if (usesArea) {
                query.setParameter("north", north);
                query.setParameter("east", east);
                query.setParameter("south", south);
                query.setParameter("west", west);
            }
            if (usesFrom) {
                query.setParameter("from", from);
            }
            if (usesTo) {
                query.setParameter("to", to);
            }
            if (usesType) {
                String className = "dk.dma.ais.abnormal.event.db.domain." + type;
                try {
                    Class clazz = Class.forName(className);
                    query.setParameter("classes", clazz);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Class " + className + " not found.");
                }
            }
            if (usesVessel) {
                query.setParameter("vessel", vessel);
            }

            LOG.debug("Query: " + query.toString());
            final long t0 = currentTimeMillis();
            events = query.list();
            final long t1 = currentTimeMillis();
            LOG.debug("Found " + events.size() + " matching events in " + (t1-t0) + " msecs.");
        } finally {
            session.close();
        }

        return events;
    }

    @Override
    public List<Event> findEventsByFromAndTo(Date from, Date to) {
        LOG.debug("Searching events from " + from + " to " + to + ".");

        Session session = getSession();

        List events = null;
        try {
            StringBuilder hql = new StringBuilder();
            hql.append("SELECT e FROM Event e WHERE ");
            hql.append("(e.suppressed=false AND e.startTime >= :from AND e.startTime <= :to) OR ");
            hql.append("(e.suppressed=false AND e.endTime >= :from AND e.endTime <= :to) ");
            hql.append("ORDER BY e.startTime");

            Query query = session.createQuery(hql.toString());
            query.setParameter("from", from);
            query.setParameter("to", to);

            events = query.list();
        } finally {
            session.close();
        }

        LOG.debug("Found " + events.size() + " matching events.");

        return events;
    }

    @Override
    public List<Event> findRecentEvents(int numberOfEvents) {
        Session session = getSession();

        List events = null;
        try {
            Query query = session.createQuery("SELECT e FROM Event e WHERE e.suppressed=false ORDER BY e.startTime DESC");
            query.setMaxResults(numberOfEvents);
            events = query.list();
        } finally {
            session.close();
        }

        return events;
    }

    @Override
    public <T extends Event> T findOngoingEventByVessel(int mmsi, Class<T> eventClass) {
        Session session = getSession();

        T event = null;
        try {
            Query query = session.createQuery("SELECT e FROM Event e LEFT JOIN e.behaviours b WHERE TYPE(e) = :clazz AND e.state = :state AND b.vessel.mmsi = :mmsi AND e.suppressed=false");
            query.setCacheable(true);
            query.setParameter("clazz", eventClass);
            query.setString("state", "ONGOING");
            query.setInteger("mmsi", mmsi);
            List events = query.list();

            if (events.size() > 0) {
                if (events.size() > 1) {
                    LOG.warn("More than one (" + events.size() + ") ongoing event of type " + eventClass + "; expected max. 1. Using first.");
                }
                event = (T) events.get(0);
            }

        } finally {
            session.close();
        }

        return event;
    }
}
