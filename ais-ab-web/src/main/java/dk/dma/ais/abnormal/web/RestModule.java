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

package dk.dma.ais.abnormal.web;

import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.jpa.JpaEventRepository;
import dk.dma.ais.abnormal.event.db.jpa.JpaSessionFactoryFactory;
import dk.dma.ais.abnormal.stat.db.StatisticDataRepository;
import dk.dma.ais.abnormal.stat.db.mapdb.StatisticDataRepositoryMapDB;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


public final class RestModule extends ServletModule {

    static final Logger LOG = LoggerFactory.getLogger(RestModule.class);

    private final String repositoryFilename;
    private final String pathToEventDatabase;
    private final String eventRepositoryType;
    private final String eventDataDbHost;
    private final Integer eventDataDbPort;
    private final String eventDataDbName;
    private final String eventDataDbUsername;
    private final String eventDataDbPassword;

    public RestModule(String repositoryFilename, String pathToEventDatabase, String eventRepositoryType, String eventDataDbHost, Integer eventDataDbPort, String eventDataDbName, String eventDataDbUsername, String eventDataDbPassword) {
        this.repositoryFilename = repositoryFilename;
        this.pathToEventDatabase = pathToEventDatabase;
        this.eventRepositoryType = eventRepositoryType;
        this.eventDataDbHost = eventDataDbHost;
        this.eventDataDbPort = eventDataDbPort;
        this.eventDataDbName = eventDataDbName;
        this.eventDataDbUsername = eventDataDbUsername;
        this.eventDataDbPassword = eventDataDbPassword;
    }

    @Override
    protected void configureServlets() {
        ResourceConfig rc = new PackagesResourceConfig(
                "dk.dma.ais.abnormal.event.rest",
                "dk.dma.ais.abnormal.stat.rest",
                "dk.dma.commons.web.rest.defaults",
                "org.codehaus.jackson.jaxrs"
        );

        for ( Class<?> resource : rc.getClasses() ) {
            String packageName = resource.getPackage().getName();
            if (packageName.equals("dk.dma.commons.web.rest.defaults") || packageName.equals("org.codehaus.jackson.jaxrs")) {
                bind(resource).in(Scopes.SINGLETON);
            } else {
                bind(resource);
            }
        }

        serve("/rest/*").with( GuiceContainer.class );
    }

    @Provides
    @Singleton
    StatisticDataRepository provideStatisticDataRepository() {
        StatisticDataRepository statisticsRepository = null;
        try {
            statisticsRepository = new StatisticDataRepositoryMapDB(repositoryFilename);
            statisticsRepository.openForRead();
        } catch (Exception e) {
            LOG.error("Problems opening repository for read: " + repositoryFilename);
            LOG.error(e.getMessage(), e);
        }
        return statisticsRepository;
    }

    @Provides
    @Singleton
    EventRepository provideEventRepository() {
        SessionFactory sessionFactory;

        if ("h2".equalsIgnoreCase(eventRepositoryType)) {
            sessionFactory = JpaSessionFactoryFactory.newH2SessionFactory(new File(pathToEventDatabase));
        } else if ("pgsql".equalsIgnoreCase(eventRepositoryType)) {
            sessionFactory = JpaSessionFactoryFactory.newPostgresSessionFactory(eventDataDbHost, eventDataDbPort, eventDataDbName, eventDataDbUsername, eventDataDbPassword);
        } else {
            throw new IllegalArgumentException("eventRepositoryType: " + eventRepositoryType);
        }

        return new JpaEventRepository(sessionFactory, true);
    }

}
