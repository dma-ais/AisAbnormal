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
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import dk.dma.ais.abnormal.stat.db.mapdb.FeatureDataRepositoryMapDB;
//import dk.dma.commons.web.rest.AbstractResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.handler.MessageContext;
import java.security.Signature;


public final class WebAppModule extends AbstractModule {

    static final Logger LOG = LoggerFactory.getLogger(WebAppModule.class);

    private final int    port;
    private final String repositoryName;

    public WebAppModule(int port, String repositoryName) {
        this.port = port;
        this.repositoryName = repositoryName;
    }

    @Override
    public void configure() {
    }

    @Provides @Singleton
    WebServer provideWebServer() {
        WebServer webServer = null;
        try {
            webServer = new WebServer(port, repositoryName);
            //webServer.getContext().setAttribute(AbstractResource.CONFIG, AbstractResource.create());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return webServer;
    }

}
