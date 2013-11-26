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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.google.inject.Injector;

import dk.dma.commons.app.AbstractDaemon;
import dk.dma.commons.web.rest.AbstractResource;

public class Main extends AbstractDaemon {

    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger(Main.class);

    @Parameter(names = "-port", description = "The port to run AisView at")
    int port = 8090;

    /** {@inheritDoc} */
    @Override
    protected void runDaemon(Injector injector) throws Exception {

        WebServer ws = new WebServer(port);
        ws.getContext().setAttribute(AbstractResource.CONFIG,
                AbstractResource.create(/*g, con, targetTracker, jobManager*/));

        ws.start();
        LOG.info("AisAbnormal REST service started");
        ws.join();
    }

    public static void main(String[] args) throws Exception {
        // args = AisReaders.getDefaultSources();
        new Main().execute(args);
    }
}
