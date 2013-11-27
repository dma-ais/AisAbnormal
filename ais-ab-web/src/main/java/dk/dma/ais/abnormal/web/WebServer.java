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
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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

    public WebServer(int port, String repositoryName) {
        server = new Server(port);
        this.context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        this.repositoryName = repositoryName;
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

        // Setup Guice-Jersey integration
        context.addEventListener(new GuiceServletContextListener() {
            @Override
            protected Injector getInjector() {
                return Guice.createInjector(new RestModule(repositoryName));
            }
        });
        context.addFilter(com.google.inject.servlet.GuiceFilter.class, "/feature/*", EnumSet.allOf(DispatcherType.class));

        // Setup diagnostic logging
        HandlerWrapper hw = new HandlerWrapper() {
            /** {@inheritDoc} */
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request,
                               HttpServletResponse response) throws IOException, ServletException {
                long start = System.nanoTime();
                String queryString = request.getQueryString() == null ? "" : "?" + request.getQueryString();
                LOG.info("Received connection from " + request.getRemoteHost() + " (" + request.getRemoteAddr() + ":"
                        + request.getRemotePort() + ") request = " + request.getRequestURI() + queryString);
                super.handle(target, baseRequest, request, response);
                LOG.info("Connection closed from " + request.getRemoteHost() + " (" + request.getRemoteAddr() + ":"
                        + request.getRemotePort() + ") request = " + request.getRequestURI() + queryString
                        + ", Duration = " + (System.nanoTime() - start) / 1000000 + " ms");
            }
        };
        hw.setHandler(context);
        server.setHandler(hw);

        // Start the server
        server.start();
    }
}
