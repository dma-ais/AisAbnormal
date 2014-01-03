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
import dk.dma.ais.abnormal.event.db.h2.H2EventRepository;
import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import dk.dma.ais.abnormal.stat.db.mapdb.FeatureDataRepositoryMapDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


public final class RestModule extends ServletModule {

    static final Logger LOG = LoggerFactory.getLogger(RestModule.class);

    private final String repositoryFilename;
    private final String pathToEventDatabase;

    public RestModule(String repositoryFilename, String pathToEventDatabase) {
        this.repositoryFilename = repositoryFilename;
        this.pathToEventDatabase = pathToEventDatabase;
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
    FeatureDataRepository provideFeatureDataRepository() {
        FeatureDataRepository featureDataRepository = null;
        try {
            featureDataRepository = new FeatureDataRepositoryMapDB(repositoryFilename);
            featureDataRepository.openForRead();
        } catch (Exception e) {
            LOG.error("Problems opening repository for read: " + repositoryFilename);
            LOG.error(e.getMessage(), e);
        }
        return featureDataRepository;
    }

    @Provides
    @Singleton
    EventRepository provideEventRepository() {
        return new H2EventRepository(new File(pathToEventDatabase), false);
    }

}

