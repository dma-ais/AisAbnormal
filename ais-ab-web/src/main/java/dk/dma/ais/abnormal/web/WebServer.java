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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.rewrite.handler.RewritePatternRule;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

/**
 * 
 * @author Kasper Nielsen
 */
public class WebServer {

    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger(WebServer.class);

    private final ServletContextHandler context;
    private final Server server;

    private final String repositoryName;
    private final String pathToEventDatabase;
    private final String eventRepositoryType;
    private final String eventDataDbHost;
    private final Integer eventDataDbPort;
    private final String eventDataDbName;
    private final String eventDataDbUsername;
    private final String eventDataDbPassword;

    public WebServer(
            int port,
            String repositoryName,
            String pathToEventDatabase,
            String eventRepositoryType,
            String eventDataDbHost,
            Integer eventDataDbPort,
            String eventDataDbName,
            String eventDataDbUsername,
            String eventDataDbPassword
        ) {
        server = new Server(port);
        this.context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        this.repositoryName = repositoryName;
        this.pathToEventDatabase = pathToEventDatabase;
        this.eventRepositoryType = eventRepositoryType;
        this.eventDataDbHost = eventDataDbHost;
        this.eventDataDbPort = eventDataDbPort;
        this.eventDataDbName = eventDataDbName;
        this.eventDataDbUsername = eventDataDbUsername;
        this.eventDataDbPassword = eventDataDbPassword;
    }

    /**
     * @return the context
     */
    public ServletContextHandler getContext() {
        return context;
    }

    public void join() throws InterruptedException {
        server.join();
    }

    public void start() throws Exception {
        ((ServerConnector) server.getConnectors()[0]).setReuseAddress(true);

        // Root context
        context.setContextPath("/abnormal");

        // Setup static content
        context.setResourceBase("src/main/webapp/");
        context.addServlet(DefaultServlet.class, "/");

        // Enable Jersey debug output
        context.setInitParameter("com.sun.jersey.config.feature.Trace", "true");

        // Little hack to satisfy OpenLayers URLs in DMA context
        RewritePatternRule openlayersRewriteRule = new RewritePatternRule();
        openlayersRewriteRule.setPattern("/abnormal/theme/*");
        openlayersRewriteRule.setReplacement("/abnormal/js/theme/");

        RewriteHandler rewrite = new RewriteHandler();
        rewrite.setRewriteRequestURI(true);
        rewrite.setRewritePathInfo(false);
        rewrite.setOriginalPathAttribute("requestedPath");
        rewrite.addRule(openlayersRewriteRule);
        rewrite.setHandler(context);
        server.setHandler(rewrite);

        // Setup Guice-Jersey integration
        context.addEventListener(new GuiceServletContextListener() {
            @Override
            protected Injector getInjector() {
                return Guice.createInjector(new RestModule(
                        repositoryName,
                        pathToEventDatabase,
                        eventRepositoryType,
                        eventDataDbHost,
                        eventDataDbPort,
                        eventDataDbName,
                        eventDataDbUsername,
                        eventDataDbPassword
                ));
            }
        });
        context.addFilter(com.google.inject.servlet.GuiceFilter.class, "/rest/*", EnumSet.allOf(DispatcherType.class));

        // Start the server
        server.start();
    }
}
