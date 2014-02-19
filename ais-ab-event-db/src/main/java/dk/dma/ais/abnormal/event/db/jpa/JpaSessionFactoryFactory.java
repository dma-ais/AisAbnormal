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

import dk.dma.ais.abnormal.event.db.domain.Behaviour;
import dk.dma.ais.abnormal.event.db.domain.CourseOverGroundEvent;
import dk.dma.ais.abnormal.event.db.domain.ShipSizeOrTypeEvent;
import dk.dma.ais.abnormal.event.db.domain.SpeedOverGroundEvent;
import dk.dma.ais.abnormal.event.db.domain.SuddenSpeedChangeEvent;
import dk.dma.ais.abnormal.event.db.domain.TrackingPoint;
import dk.dma.ais.abnormal.event.db.domain.Vessel;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * JpaSessionFactoryFactory builds and delivers Hibernate SessionFactory objects that can
 * be used to connect to a relational database.
 */
public final class JpaSessionFactoryFactory {

    private static final Logger LOG = LoggerFactory.getLogger(JpaSessionFactoryFactory.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    /**
     * Create a new SessionFactory which can be used to connect to an H2 file based database.
     */
    public static SessionFactory newH2SessionFactory(File dbFilename) {
        LOG.debug("Loading Hibernate configuration.");

        Configuration configuration = new Configuration()
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", buildH2ConnectionUrl(dbFilename))
                .setProperty("hibernate.connection.username", "sa")
                .setProperty("hibernate.connection.password", "")
                .setProperty("hibernate.default_schema", "PUBLIC")
                .setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
                //.setProperty("hibernate.show_sql", "true")
                .setProperty("hibernate.hbm2ddl.auto", "update")
                .setProperty("hibernate.order_updates", "true")
                .setProperty("hibernate.cache.provider_class", "org.hibernate.cache.EhCacheProvider")
                .setProperty("hibernate.cache.use_second_level_cache", "true")
                .setProperty("hibernate.cache.use_query_cache", "true")
                .setProperty("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory")
                .addAnnotatedClass(CourseOverGroundEvent.class)
                .addAnnotatedClass(SpeedOverGroundEvent.class)
                .addAnnotatedClass(ShipSizeOrTypeEvent.class)
                .addAnnotatedClass(SuddenSpeedChangeEvent.class)
                .addAnnotatedClass(Vessel.class)
                .addAnnotatedClass(Behaviour.class)
                .addAnnotatedClass(TrackingPoint.class);

        ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder();
        serviceRegistryBuilder.applySettings(configuration.getProperties());
        ServiceRegistry serviceRegistry = serviceRegistryBuilder.buildServiceRegistry();

        LOG.info("Starting Hibernate.");
        SessionFactory sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        testConnection(sessionFactory);
        LOG.info("Hibernate started.");

        return sessionFactory;
    }

    /**
     * Create a new SessionFactory which can be used to connect to a Postgresql database
     * via TCP/IP.
     *
     * @param dbHost
     * @param dbPort
     * @param dbName
     * @param dbUsername
     * @param dbPassword
     * @return
     */
    public static SessionFactory newPostgresSessionFactory(String dbHost, int dbPort, String dbName, String dbUsername, String dbPassword) {
        LOG.debug("Loading Hibernate configuration.");

        Configuration configuration = new Configuration()
                .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
                .setProperty("hibernate.connection.url", buildPgsqlConnectionUrl(dbHost, dbPort, dbName))
                .setProperty("hibernate.connection.username", dbUsername)
                .setProperty("hibernate.connection.password", dbPassword)
                .setProperty("hibernate.default_schema", "PUBLIC")
                .setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                //.setProperty("hibernate.show_sql", "true")
                .setProperty("hibernate.hbm2ddl.auto", "update")
                .setProperty("hibernate.order_updates", "true")
                .setProperty("hibernate.connection_pool_size", "1")
                .setProperty("hibernate.cache.provider_class", "org.hibernate.cache.EhCacheProvider")
                .setProperty("hibernate.cache.use_second_level_cache", "true")
                .setProperty("hibernate.cache.use_query_cache", "true")
                .setProperty("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory")
                .addAnnotatedClass(CourseOverGroundEvent.class)
                .addAnnotatedClass(SpeedOverGroundEvent.class)
                .addAnnotatedClass(ShipSizeOrTypeEvent.class)
                .addAnnotatedClass(SuddenSpeedChangeEvent.class)
                .addAnnotatedClass(Vessel.class)
                .addAnnotatedClass(Behaviour.class)
                .addAnnotatedClass(TrackingPoint.class);

        ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder();
        serviceRegistryBuilder.applySettings(configuration.getProperties());
        ServiceRegistry serviceRegistry = serviceRegistryBuilder.buildServiceRegistry();

        LOG.info("Starting Hibernate.");
        SessionFactory sessionFactory = null;
        sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        testConnection(sessionFactory);
        LOG.info("Hibernate started.");

        return sessionFactory;
    }

    private static void testConnection(SessionFactory sessionFactory) {
        Session session = null;
        try {
            session = sessionFactory.openSession();
            session.beginTransaction();
            session.getTransaction().rollback();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private static String buildH2ConnectionUrl(File dbFilename) {
        StringBuffer connectionUrl = new StringBuffer();
        connectionUrl.append("jdbc:h2:");
        connectionUrl.append(dbFilename.getAbsolutePath());
        connectionUrl.append(";");
        connectionUrl.append("TRACE_LEVEL_FILE=0");
        connectionUrl.append(";");
        connectionUrl.append("TRACE_LEVEL_SYSTEM_OUT=1");
        LOG.debug("Using connectionUrl=" + connectionUrl.toString());
        return connectionUrl.toString();
    }

    private static String buildPgsqlConnectionUrl(String host, int port, String dbname) {
        StringBuffer connectionUrl = new StringBuffer();
        connectionUrl.append("jdbc:postgresql://");
        connectionUrl.append(host);
        connectionUrl.append(":");
        connectionUrl.append(port);
        connectionUrl.append("/");
        connectionUrl.append(dbname);
        LOG.debug("Using connectionUrl=" + connectionUrl.toString());
        return connectionUrl.toString();
    }

}
