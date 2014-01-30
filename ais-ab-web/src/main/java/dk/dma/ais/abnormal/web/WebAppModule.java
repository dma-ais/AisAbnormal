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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import dk.dma.commons.web.rest.AbstractResource;


public final class WebAppModule extends AbstractModule {

    static final Logger LOG = LoggerFactory.getLogger(WebAppModule.class);

    private final int    port;
    private final String repositoryName;
    private final String pathToEventDatabase;
    private final String eventRepositoryType;
    private final String eventDataDbHost;
    private final Integer eventDataDbPort;
    private final String eventDataDbName;
    private final String eventDataDbUsername;
    private final String eventDataDbPassword;

    public WebAppModule(int port, String repositoryName, String pathToEventDatabase, String eventRepositoryType, String eventDataDbHost, Integer eventDataDbPort, String eventDataDbName, String eventDataDbUsername, String eventDataDbPassword) {
        this.port = port;
        this.repositoryName = repositoryName;
        this.pathToEventDatabase = pathToEventDatabase;
        this.eventRepositoryType = eventRepositoryType;
        this.eventDataDbHost = eventDataDbHost;
        this.eventDataDbPort = eventDataDbPort;
        this.eventDataDbName = eventDataDbName;
        this.eventDataDbUsername = eventDataDbUsername;
        this.eventDataDbPassword = eventDataDbPassword;
    }

    @Override
    public void configure() {
    }

    @Provides
    @Singleton
    WebServer provideWebServer() {
        WebServer webServer = null;
        try {
            webServer = new WebServer(
                    port,
                    repositoryName,
                    pathToEventDatabase,
                    eventRepositoryType,
                    eventDataDbHost,
                    eventDataDbPort,
                    eventDataDbName,
                    eventDataDbUsername,
                    eventDataDbPassword);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return webServer;
    }

}
