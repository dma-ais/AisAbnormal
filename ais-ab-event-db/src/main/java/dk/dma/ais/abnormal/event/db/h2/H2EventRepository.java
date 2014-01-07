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

package dk.dma.ais.abnormal.event.db.h2;

import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.domain.AbnormalShipSizeOrTypeEvent;
import dk.dma.ais.abnormal.event.db.domain.Behaviour;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.Position;
import dk.dma.ais.abnormal.event.db.domain.SuddenSpeedChangeEvent;
import dk.dma.ais.abnormal.event.db.domain.Vessel;
import dk.dma.ais.abnormal.event.db.domain.VesselId;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;
import java.util.List;

@SuppressWarnings("JpaQlInspection")
public class H2EventRepository implements EventRepository {

    private static final Logger LOG = LoggerFactory.getLogger(H2EventRepository.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final SessionFactory sessionFactory;
    private final boolean readonly;

    public H2EventRepository(File dbFilename, boolean readonly) {
        this.readonly = readonly;
        try{
            LOG.debug("Loading Hibernate configuration.");
            Configuration configuration = new Configuration()
                    .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                    .setProperty("hibernate.connection.url", buildConnectionUrl(dbFilename))
                    .setProperty("hibernate.connection.username", "sa")
                    .setProperty("hibernate.connection.password", "")
                    .setProperty("hibernate.default_schema", "PUBLIC")
                    .setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                    //.setProperty("hibernate.show_sql", "true")
                    .setProperty("hibernate.hbm2ddl.auto", "update")
                    .setProperty("hibernate.order_updates", "true")
                    .addAnnotatedClass(AbnormalShipSizeOrTypeEvent.class)
                    .addAnnotatedClass(SuddenSpeedChangeEvent.class)
                    .addAnnotatedClass(Vessel.class)
                    .addAnnotatedClass(VesselId.class)
                    .addAnnotatedClass(Behaviour.class)
                    .addAnnotatedClass(Position.class);
            ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder();
            serviceRegistryBuilder.applySettings(configuration.getProperties());
            ServiceRegistry serviceRegistry = serviceRegistryBuilder.buildServiceRegistry();
            LOG.info("Starting Hibernate.");
            sessionFactory = configuration.buildSessionFactory(serviceRegistry);
            LOG.info("Hibernate started.");

            Session session = getSession();
            try {
                List<Number> list = session.createQuery("SELECT count(*) FROM Event").list();
                Number number = list.get(0);
                LOG.info("Connected to an event database containing " + number + " events.");
            } finally {
                session.close();
            }

        }catch (Throwable ex) {
            System.err.println("Failed to create sessionFactory object." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    @Override
    protected void finalize() {
        LOG.info("Closing database session factory.");
        sessionFactory.close();
    }

    private static String buildConnectionUrl(File dbFilename) {
        StringBuffer connectionUrl = new StringBuffer();
        connectionUrl.append("jdbc:h2:");
        connectionUrl.append(dbFilename.getAbsolutePath());
        connectionUrl.append(";");
        connectionUrl.append("TRACE_LEVEL_FILE=0");
        connectionUrl.append(";");
        connectionUrl.append("TRACE_LEVEL_SYSTEM_OUT=2");
        LOG.debug("Using connectionUrl=" + connectionUrl);
        return connectionUrl.toString();
    }

    private Session getSession() {
        Session session = sessionFactory.openSession();
        if (readonly) {
            session.setDefaultReadOnly(true);
        }
        return session;
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
        Session session = getSession();
        Event event = (Event) session.get(Event.class, eventId);
        return event;
    }

    @Override
    public List<Event> findEventsByFromAndToAndTypeAndVessel(Date from, Date to, String type, String vessel) {
        Session session = getSession();

        boolean usesFrom = false, usesTo = false, usesType = false, usesVessel = false;

        List events = null;
        try {
            //Query query = session.createQuery("SELECT e FROM Event e WHERE e.behaviour.vessel.id.name LIKE :vessel OR e.behaviour.vessel.id.callsign LIKE :vessel OR e.behaviour.vessel.id.imo=:vessel OR e.behaviour.vessel.id.mmsi=:vessel");
            StringBuilder hql = new StringBuilder();
            hql.append("SELECT e FROM Event e WHERE ");

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
                hql.append("e.behaviour.vessel.id.callsign LIKE :vessel OR ");
                hql.append("e.behaviour.vessel.id.name LIKE :vessel OR ");
                try {
                    Long vesselAsLong = Long.valueOf(vessel);
                    hql.append("e.behaviour.vessel.id.mmsi = :vessel OR ");
                    hql.append("e.behaviour.vessel.id.imo = :vessel OR ");
                } catch (NumberFormatException e) {
                }
                hql.replace(hql.length()-3, hql.length(), ")"); // "OR " -> ")"
                usesVessel = true;
            }

            //
            String hqlAsString = hql.toString().trim();
            if (hqlAsString.endsWith("AND")) {
                hqlAsString = hqlAsString.substring(0, hqlAsString.lastIndexOf("AND"));
            }

            //
            Query query = session.createQuery(hqlAsString);
            if (usesFrom) {
                query.setParameter("from", from);
            }
            if (usesTo) {
                query.setParameter("to", to);
            }
            if (usesType) {
                query.setParameter("type", "NIY");
            }
            if (usesVessel) {
                query.setParameter("vessel", vessel);
            }
            events = query.list();
        } finally {
            session.close();
        }

        return events;

    }

    @Override
    public List<Event> findOngoingEventsByVessel(VesselId vesselId) {
        Session session = getSession();

        List events = null;
        try {
            Query query = session.createQuery("SELECT e FROM Event e WHERE e.state=:state AND e.behaviour.vessel.id.name=:name AND e.behaviour.vessel.id.callsign=:callsign AND e.behaviour.vessel.id.imo=:imo AND e.behaviour.vessel.id.mmsi=:mmsi");
            query.setString("state", "ONGOING");
            query.setString("name", vesselId.getName());
            query.setString("callsign", vesselId.getCallsign());
            query.setInteger("imo", vesselId.getImo());
            query.setInteger("mmsi", vesselId.getMmsi());
            events = query.list();
        } finally {
            session.close();
        }

        return events;
    }

    @Override
    public <T extends Event> T findOngoingEventByVessel(VesselId vesselId, Class<T> eventClass) {
        Session session = getSession();

        T event = null;
        try {
            Query query = session.createQuery("SELECT e FROM Event e WHERE TYPE(e)=:class AND e.state=:state AND e.behaviour.vessel.id.name=:name AND e.behaviour.vessel.id.callsign=:callsign AND e.behaviour.vessel.id.imo=:imo AND e.behaviour.vessel.id.mmsi=:mmsi");
            query.setParameter("class", eventClass);
            query.setString("state", "ONGOING");
            query.setString("name", vesselId.getName());
            query.setString("callsign", vesselId.getCallsign());
            query.setInteger("imo", vesselId.getImo());
            query.setInteger("mmsi", vesselId.getMmsi());
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
