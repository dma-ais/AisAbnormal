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
import dk.dma.ais.reader.AisReader;
import dk.dma.commons.app.AbstractDaemon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Thread.UncaughtExceptionHandler;

/**
 * AIS Abnormal event analyzer
 */
public class AbnormalAnalyzerApp extends AbstractDaemon {

    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger(AbnormalAnalyzerApp.class);
    {
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
        reader.start();
        reader.join();


        /*
        Grid grid = getInjector().getInstance(Grid.class);

        packetHandler = packetHandlerFactory.create(userArguments.isMultiThreaded());
        // Write dataset metadata before we start
        DatasetMetaData metadata = new DatasetMetaData(grid.getResolution(), userArguments.getDownSampling());
        featureDataRepository.putMetaData(metadata);

        reader.registerPacketHandler(packetHandler);
        reader.start();
        reader.join();

        executorService.shutdown();
        boolean shutdown;
        do {
            LOG.debug("Waiting for worker tasks to complete.");
            shutdown = executorService.awaitTermination(1, TimeUnit.MINUTES);
        } while(!shutdown);
        LOG.info("All worker tasks completed.");

        statisticsService.dumpStatistics();

        featureDataRepository.close();
        statisticsService.stop();
        */
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
                System.exit(-1);
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

        if (userArguments.isHelp()) {
            jCommander = new JCommander(userArguments, "-help");
            jCommander.setProgramName("AbnormalAnalyzerApp");
            jCommander.usage();
        } else {
            Injector injector = Guice.createInjector(new AbnormalAnalyzerAppModule(userArguments.getInputDirectory(), userArguments.getInputFilenamePattern(), userArguments.isRecursive(), userArguments.getFeatureData(), userArguments.getPathToEventDatabase()));
            AbnormalAnalyzerApp.setInjector(injector);
            AbnormalAnalyzerApp app = injector.getInstance(AbnormalAnalyzerApp.class);
            app.execute(new String[]{} /* no cmd args - we handled them already */ );
        }
    }
}
