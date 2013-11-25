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

import com.beust.jcommander.Parameter;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import dk.dma.ais.reader.AisReader;
import dk.dma.ais.reader.AisReaders;
import dk.dma.commons.app.AbstractDaemon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Thread.UncaughtExceptionHandler;

/**
 * AIS Abnormal Behavior statistics builder
 */
public final class AbnormalStatBuilderApp extends AbstractDaemon {

    /** The logger */
    //@Log
    private static Logger LOG = LoggerFactory.getLogger(AbnormalStatBuilderApp.class);

    // TODO find a way to share injector stored in AbstractDmaApplication
    private static Injector injector;

    @Parameter(names = "-dir", description = "Directory to scan for files to read")
    private String dir = ".";
    
    @Parameter(names = "-r", description = "Recursive directory scan")
    private boolean recursive;

    @Parameter(names = "-input", description = "Glob pattern for files to read. '.zip' and '.gz' files are decompressed automatically.", required = true)
    private String inputFilenamePattern;

    @Parameter(names = "-output", description = "Name of output file.", required = true)
    private String outputFilename;

    @Inject
    private volatile PacketHandler handler;

    @Inject
    private volatile AppStatisticsService appStatisticsService;

    private volatile AisReader reader;

    @Override
    protected void runDaemon(Injector injector) throws Exception {
        LOG.info("AbnormalStatBuilderApp starting using dir: " + dir + " inputFilenamePattern: " + inputFilenamePattern + (recursive ? "(recursive)" : ""));

        // Create and start reader
        reader = AisReaders.createDirectoryReader(dir, inputFilenamePattern, recursive);
        reader.registerPacketHandler(handler);
        reader.start();
        reader.join();
    }
    
    @Override
    protected void preShutdown() {
        LOG.info("AbnormalStatBuilderApp shutting down");
        if (reader != null) {
            reader.stopReader();
        }
        if (handler != null) {
            handler.getBuildStats().log(true);
            handler.cancel();
        }
        super.preShutdown();
    }

    @Override
    public void execute(String[] args) throws Exception {
        super.execute(args);
    }

    public AppStatisticsService getAppStatisticsService() {
        return appStatisticsService;
    }

    public String getOutputFilename() {
        return outputFilename;
    }

    public static void setInjector(Injector injector) {
        AbnormalStatBuilderApp.injector = injector;
    }

    public static Injector getInjector() {
        return injector;
    }

    private static String getDbFilename(String[] args) {
        // TODO find a way to share cmdline parameters with module
        String dbFilename = null;
        for (int i=0; i<args.length; i++) {
            if ("-output".equalsIgnoreCase(args[i])) {
                dbFilename = args[i+1];
                break;
            }
        }
        return dbFilename;
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

        String dbFilename = getDbFilename(args);
        if (dbFilename == null) {
            LOG.error("No -output parameter found in cmd line parameters.");
        } else {
            Injector injector = Guice.createInjector(new AbnormalStatBuilderAppModule(dbFilename));
            AbnormalStatBuilderApp.setInjector(injector);
            AbnormalStatBuilderApp app = injector.getInstance(AbnormalStatBuilderApp.class);
            app.execute(args);
            // app.handler.printAllFeatureStatistics(System.out);
        }
    }
}
