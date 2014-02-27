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
package dk.dma.ais.abnormal.analyzer;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import dk.dma.ais.abnormal.application.ApplicationSupport;
import dk.dma.ais.reader.AisReader;
import dk.dma.commons.app.AbstractDaemon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Thread.UncaughtExceptionHandler;

import static java.lang.System.exit;

/**
 * AIS Abnormal event analyzer
 */
public class AbnormalAnalyzerApp extends AbstractDaemon {

    /**
     * The logger
     */
    static final Logger LOG = LoggerFactory.getLogger(AbnormalAnalyzerApp.class);

    {
        ApplicationSupport.logJavaSystemProperties(LOG);
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }


    // TODO find a way to share injector stored in AbstractDmaApplication
    private static Injector injector;

    static UserArguments userArguments;

    @Inject
    private AisReader reader;

    @Inject
    private PacketHandler packetHandler;

    @Inject
    private AppStatisticsService statisticsService;

    @Override
    protected void runDaemon(Injector injector) throws Exception {
        LOG.info("Starting AbnormalAnalyzerApp");

        statisticsService.start();

        reader.registerPacketHandler(packetHandler);
        LOG.info("Starting AisDirectoryReader thread.");
        reader.start();
        LOG.info("Joining AisDirectoryReader thread.");
        reader.join();
        LOG.info("AisDirectoryReader thread finished.");

    }

    @Override
    public void execute(String[] args) throws Exception {
        super.execute(args);
    }

    private static void setInjector(Injector injector) {
        AbnormalAnalyzerApp.injector = injector;
    }

    public static Injector getInjector() {
        return injector;
    }

    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                LOG.error("Uncaught exception in thread " + t.getClass().getCanonicalName() + ": " + e.getMessage(), e);
                exit(-1);
            }
        });

        // TODO find a way to integrate with app.addModule
        // AbnormalAnalyzerApp app = new AbnormalAnalyzerApp();
        // AbstractModule module = new AbnormalAnalyzerModule(app);
        // app.addModule(module);

        // TODO find a way to share DMA AbtractCommandLineTool parameters with Guice module/userArguments
        userArguments = new UserArguments();
        JCommander jCommander;

        try {
            new JCommander(userArguments, args);
        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            userArguments.setHelp(true);
        }

        if (userArguments.isHelp() || (!userArguments.paramsValidForH2() && !userArguments.paramsValidForPgsql())) {
            jCommander = new JCommander(userArguments, "-help");
            jCommander.setProgramName("AbnormalAnalyzerApp");
            jCommander.usage();
        } else {
            Injector injector = Guice.createInjector(
                    new AbnormalAnalyzerAppModule(
                            userArguments.getInputDirectory(),
                            userArguments.getInputFilenamePattern(),
                            userArguments.isRecursive(),
                            userArguments.getFeatureData(),
                            userArguments.getEventDataDbFile(),
                            userArguments.getDownSampling(),
                            userArguments.getEventDataRepositoryType(),
                            userArguments.getEventDataDbHost(),
                            userArguments.getEventDataDbPort(),
                            userArguments.getEventDataDbName(),
                            userArguments.getEventDataDbUsername(),
                            userArguments.getEventDataDbPassword()
                    )
            );
            AbnormalAnalyzerApp.setInjector(injector);
            try {
                AbnormalAnalyzerApp app = injector.getInstance(AbnormalAnalyzerApp.class);
                LOG.info("Executing application.");
                app.execute(new String[]{} /* no cmd args - we handled them already */);
                LOG.info("Completed executing application.");
            } catch (Exception e) {
                e.printStackTrace(System.err);
                LOG.error(e.getMessage());
            }
        }

        exit(0);
    }
}
