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
    static final Logger LOG = LoggerFactory.getLogger(AbnormalStatBuilderApp.class);

    // Bootstrap Guice dependency injection
    private static Injector injector;

    @Parameter(names = "-dir", description = "Directory to scan for files to read")
    String dir = ".";
    
    @Parameter(names = "-r", description = "Recursive directory scan")
    boolean recursive;

    @Parameter(names = "-name", description = "Glob pattern for files to read. '.zip' and '.gz' files are decompressed automatically.", required = true)
    String name;

    @Inject
    private volatile PacketHandler handler;
    private volatile AisReader reader;

    @Inject
    private AppStatisticsService appStatisticsService;

    @Override
    protected void runDaemon(Injector injector) throws Exception {
        LOG.info("AbnormalStatBuilderApp starting using dir: " + dir + " name: " + name + (recursive ? "(recursive)" : ""));

        // Create and start reader
        reader = AisReaders.createDirectoryReader(dir, name, recursive);
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

        Injector injector = Guice.createInjector(new AbnormalStatBuilderAppInjector());
        AbnormalStatBuilderApp.setInjector(injector);
        AbnormalStatBuilderApp app = injector.getInstance(AbnormalStatBuilderApp.class);

        // Start application
        app.execute(args);

        // Round off
        app.handler.printAllFeatureStatistics(System.out);
    }
}
