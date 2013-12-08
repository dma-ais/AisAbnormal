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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import dk.dma.ais.abnormal.stat.features.Feature;
import dk.dma.ais.abnormal.stat.features.ShipTypeAndSizeFeature;
import dk.dma.ais.abnormal.stat.tracker.TrackingService;
import dk.dma.ais.concurrency.stripedexecutor.StripedExecutorService;
import dk.dma.ais.filter.ReplayDownSampleFilter;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.IPositionMessage;
import eu.javaspecialists.tjsn.concurrency.StripedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.ais.message.AisMessage;
import dk.dma.ais.packet.AisPacket;

import java.io.PrintStream;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Handler for read AIS packets
 */
public class PacketHandlerImpl implements PacketHandler {

    static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);
    
    private AppStatisticsService statisticsService; // = new AppStatisticsServiceImpl(1, TimeUnit.MINUTES);
    private TrackingService trackingService;
    private ReplayDownSampleFilter downSampleFilter;
    private StripedExecutorService workerThreads;
    private final boolean multiThreaded;

    private volatile boolean cancel;

    private Set<Feature> features;

    // Concurrency bookkeeping
    private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private Long nextStripe = Long.valueOf(0);
    private final Map<Integer,Long> userIdToStripe = new TreeMap<>();

    @Inject
    public PacketHandlerImpl(AppStatisticsService statisticsService, TrackingService trackingService, ReplayDownSampleFilter downSampleFilter, StripedExecutorService executorService, @Assisted boolean multiThreaded) {
        LOG.debug("Detected " + NUMBER_OF_CORES + " CPU cores.");
        LOG.info("Creating " + (multiThreaded ? "multi threaded ":"single threaded ")+ "AIS packet handler.");

        this.statisticsService = statisticsService;
        this.trackingService = trackingService;
        this.downSampleFilter = downSampleFilter;
        this.workerThreads = executorService;
        this.multiThreaded = multiThreaded;

        initFeatures();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    public void accept(final AisPacket packet) {
        if (cancel) {
            return;
        }

        statisticsService.incUnfilteredPacketCount();
        if (downSampleFilter.rejectedByFilter(packet)) {
            return;
        }
        statisticsService.incFilteredPacketCount();

        long n = statisticsService.getFilteredPacketCount();
        if (n % 100000L == 0) {
            LOG.debug(n + " packets through filter.");
        }

        // Get AisMessage from packet or drop
        AisMessage message = packet.tryGetAisMessage();
        if (message == null) {
            return;
        }
        statisticsService.incMessageCount();

        if (message instanceof IPositionMessage) {
            statisticsService.incPosMsgCount();
        } else if (message instanceof AisMessage5) {
            statisticsService.incStatMsgCount();
        }

        Date timestamp = packet.getTimestamp();

        if (multiThreaded) {
            Object stripe = assignStripe(message);
            workerThreads.submit(new Task(message, timestamp, stripe));
        } else {
            doWork(timestamp, message);
        }
    }

    private Object assignStripe(AisMessage message) {
        //return Integer.valueOf(String.valueOf(message.getUserId())).hashCode() % 8;
        Integer userId = message.getUserId();
        Long stripe = userIdToStripe.get(userId);
        if (stripe == null) {
            stripe = (nextStripe++) % NUMBER_OF_CORES;
            userIdToStripe.put(userId, stripe);
        }
        return stripe;
    }

    private void doWork(Date timestamp, AisMessage message) {
        trackingService.update(timestamp, message);
        statisticsService.setTrackCount(trackingService.getNumberOfTracks());
    }

    @Override
    public void cancel() {
        cancel = true;
        // TODO close down and clean up
    }

    @Override
    public AppStatisticsService getBuildStats() {
        return statisticsService;
    }

    @Override
    public void printAllFeatureStatistics(PrintStream stream) {
        stream.println("Collected statistics for all features:");
        stream.println();
        for (Feature feature : features) {
            feature.printStatistics(stream);
        }
    }

    private void initFeatures() {
        Injector injector = AbnormalStatBuilderApp.getInjector();

        this.features = new ImmutableSet.Builder<Feature>()
            .add(injector.getInstance(ShipTypeAndSizeFeature.class))
            .build();
    }

    private final class Task implements StripedRunnable {
        final Date timestamp;
        final AisMessage message;
        final Object stripe;

        public Task(AisMessage message, Date timestamp, Object stripe) {
            this.message = message;
            this.timestamp = timestamp;
            this.stripe = stripe;
        }

        @Override
        public void run() {
            try {
                doWork(timestamp, message);
            } catch(Throwable t) {
                LOG.error(t.getMessage(), t);
            }
        }

        @Override
        public Object getStripe() {
            return stripe;
        }
    }
}
