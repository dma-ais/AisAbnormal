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
package dk.dma.ais.abnormal.stat;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import dk.dma.ais.abnormal.stat.db.data.DatasetMetaData;
import dk.dma.ais.concurrency.stripedexecutor.StripedExecutorService;
import dk.dma.ais.reader.AisReader;
import dk.dma.commons.app.AbstractDaemon;
import dk.dma.enav.model.geometry.grid.Grid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * AIS Abnormal Behavior statistics builder
 */
public final class AbnormalStatBuilderApp extends AbstractDaemon {

    /** The logger */
    //@Log
    private static Logger LOG = LoggerFactory.getLogger(AbnormalStatBuilderApp.class);

    // TODO find a way to share injector stored in AbstractDmaApplication
    private static Injector injector;

    private PacketHandler packetHandler;

    @Inject
    private PacketHandlerFactory packetHandlerFactory;

    @Inject
    private AisReader reader;

    @Inject
    private FeatureDataRepository featureDataRepository;

    @Inject
    private AppStatisticsService statisticsService;

    @Inject
    private StripedExecutorService executorService;

    static UserArguments userArguments;

    @Override
    protected void runDaemon(Injector injector) throws Exception {
        LOG.info("Application starting.");

        packetHandler = packetHandlerFactory.create(userArguments.isMultiThreaded());

        statisticsService.start();

        // Write dataset metadata before we start
        Grid grid = getInjector().getInstance(Grid.class);

        DatasetMetaData metadata = new DatasetMetaData(grid.getResolution(), userArguments.getDownSampling());
        featureDataRepository.putMetaData(metadata);

        reader.registerPacketHandler(packetHandler);
        reader.start();
        reader.join();

        executorService.shutdown();
        boolean shutdown = false;
        do {
            LOG.debug("Waiting for worker tasks to complete.");
            shutdown = executorService.awaitTermination(1, TimeUnit.MINUTES);
        } while(!shutdown);
        LOG.info("All worker tasks completed.");

        statisticsService.dumpStatistics();

        featureDataRepository.close();
        statisticsService.stop();
    }
    
    @Override
    protected void preShutdown() {
        LOG.info("AbnormalStatBuilderApp shutting down");
        if (reader != null) {
            reader.stopReader();
        }
        if (packetHandler != null) {
            packetHandler.cancel();
        }
        statisticsService.dumpStatistics();
        super.preShutdown();
    }

    @Override
    public void execute(String[] args) throws Exception {
        super.execute(args);
    }

    public static void setInjector(Injector injector) {
        AbnormalStatBuilderApp.injector = injector;
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
        // AbnormalStatBuilderApp app = new AbnormalStatBuilderApp();
        // AbstractModule module = new AbnormalStatBuilderAppModule(app);
        // app.addModule(module);

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
            jCommander = new JCommander(userArguments, new String[] { "-help", "-input", "-output" });
            jCommander.setProgramName("AbnormalStatBuilderApp");
            jCommander.usage();
        } else {
            Injector injector = Guice.createInjector(new AbnormalStatBuilderAppModule(userArguments.getOutputFilename(), userArguments.getInputDirectory(), userArguments.getInputFilenamePattern(), userArguments.isRecursive(), userArguments.isMultiThreaded(), userArguments.getGridSize(), userArguments.getDownSampling()));
            AbnormalStatBuilderApp.setInjector(injector);
            AbnormalStatBuilderApp app = injector.getInstance(AbnormalStatBuilderApp.class);
            app.execute(new String[]{} /* no cmd args - we handled them already */ );
            // app.handler.printAllFeatureStatistics(System.out);
        }
    }
}
