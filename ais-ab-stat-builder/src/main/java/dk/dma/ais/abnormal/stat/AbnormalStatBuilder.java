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

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.Parameter;
import com.google.inject.Injector;

import dk.dma.ais.reader.AisReader;
import dk.dma.ais.reader.AisReaders;
import dk.dma.commons.app.AbstractDaemon;

/**
 * AIS Abnormal Behavior statistics builder
 */
public class AbnormalStatBuilder extends AbstractDaemon {

    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger(AbnormalStatBuilder.class);

    @Parameter(names = "-file", description = "Historical AIS data file (plain text or .gz)", required = true)
    String file;

    @Parameter(names = "-db", description = "Output database")
    String db = "ais-db-stat_db";

    private volatile PacketHandler handler;
    private volatile AisReader reader;

    @Override
    protected void runDaemon(Injector injector) throws Exception {
        LOG.info("StatBuilder starting using file " + file);
        handler = new PacketHandler(db);

        // Open input stream
        InputStream input = new FileInputStream(file);
        if (file.endsWith(".gz")) {
            LOG.info("Using GZip decompression");
            input = new GZIPInputStream(input);
        }
        
        // Create reader
        reader = AisReaders.createReaderFromInputStream(input);
        reader.registerPacketHandler(handler);

        reader.start();
        reader.join();
    }

    @Override
    public void shutdown() {
        LOG.info("StatBuilder shutting down");
        AisReader r = reader;
        PacketHandler h = handler;
        if (r != null) {
            r.stopReader();
        }
        if (h != null) {
            h.getBuildStats().log(true);
            h.cancel();
        }
        super.shutdown();
    }

    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                LOG.error("Uncaught exception in thread " + t.getClass().getCanonicalName() + ": " + e.getMessage(), e);
                System.exit(-1);
            }
        });
        new AbnormalStatBuilder().execute(args);
    }}
