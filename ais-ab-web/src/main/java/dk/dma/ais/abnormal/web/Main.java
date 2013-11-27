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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.inject.Guice;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import dk.dma.commons.app.AbstractDaemon;

public class Main extends AbstractDaemon {

    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static UserArguments userArguments;

    @Inject
    private WebServer webServer;

    /** {@inheritDoc} */
    @Override
    protected void runDaemon(Injector injector) throws Exception {
        webServer.start();
        LOG.info("AisAbnormal Web and REST service started");
        webServer.join();
    }

    // TODO find a way to share injector stored in AbstractDmaApplication
    private static Injector injector;

    public static Injector getInjector() {
        return injector;
    }

    public static void main(String[] args) throws Exception {
        // TODO find a way to share DMA AbtractCommandLineTool parameters with Guice module/userArguments
        userArguments = new UserArguments();
        JCommander jCommander=null;

        try {
            jCommander = new JCommander(userArguments, args);
        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            userArguments.setHelp(true);
        }

        if (userArguments.isHelp()) {
            jCommander = new JCommander(userArguments, new String[] { "-help", "-featureDirectory", "" });
            jCommander.setProgramName("ais-ab-web");
            jCommander.usage();
        } else {
            WebAppModule module = new WebAppModule(userArguments.getPort(), userArguments.getFeatureDataName());
            injector = Guice.createInjector(module);
            Main app = injector.getInstance(Main.class);
            app.execute(new String[]{} /* no cmd args - we handled them already */ );
        }
    }
}
