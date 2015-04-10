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
import dk.dma.ais.abnormal.stat.statistics.CourseOverGroundStatistic;
import dk.dma.ais.abnormal.stat.statistics.ShipTypeAndSizeStatistic;
import dk.dma.ais.abnormal.stat.statistics.SpeedOverGroundStatistic;
import dk.dma.ais.abnormal.stat.statistics.TrackingEventListener;
import dk.dma.ais.concurrency.stripedexecutor.StripedExecutorService;
import dk.dma.ais.filter.ReplayDownSampleFilter;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.IPositionMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.tracker.Tracker;
import eu.javaspecialists.tjsn.concurrency.StripedRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;

/**
 * Handler for read AIS packets
 */
public class PacketHandlerImpl implements PacketHandler {

    static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);
    
    private AppStatisticsService statisticsService; // = new AppStatisticsServiceImpl(1, TimeUnit.MINUTES);
    private Tracker trackingService;
    private ReplayDownSampleFilter downSampleFilter;
    private StripedExecutorService workerThreads;
    private final boolean multiThreaded;

    private volatile boolean cancel;

    private Set<TrackingEventListener> statistics;

    private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();

    @Inject
    public PacketHandlerImpl(AppStatisticsService statisticsService, Tracker trackingService, ReplayDownSampleFilter downSampleFilter, StripedExecutorService executorService, @Assisted boolean multiThreaded) {
        LOG.debug("Detected " + NUMBER_OF_CORES + " CPU cores.");
        LOG.info("Creating " + (multiThreaded ? "multi threaded ":"single threaded ")+ "AIS packet handler.");

        this.statisticsService = statisticsService;
        this.trackingService = trackingService;
        this.downSampleFilter = downSampleFilter;
        this.workerThreads = executorService;
        this.multiThreaded = multiThreaded;

        initStatistics();
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

        if (multiThreaded) {
            Object stripe = assignStripe(message);
            workerThreads.submit(new Task(packet, stripe));
        } else {
            doWork(packet);
        }
    }

    private static int hash(int a) {
        // https://gist.github.com/badboy/6267743
        a = ~a + (a << 15); // key = (key << 15) - key - 1;
        a = a ^ (a >>> 12);
        a = a + (a << 2);
        a = a ^ (a >>> 4);
        a = a * 2057; // key = (key + (key << 3)) + (key << 11);
        a = a ^ (a >>> 16);
        return a;
    }

    private static Object assignStripe(AisMessage message) {
        return Integer.valueOf(Math.abs(hash(message.getUserId())) % NUMBER_OF_CORES);
    }

    private void doWork(AisPacket p) {
        trackingService.update(p);
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

    private void initStatistics() {
        Injector injector = AbnormalStatBuilderApp.getInjector();

        this.statistics = new ImmutableSet.Builder<TrackingEventListener>()
            .add(injector.getInstance(ShipTypeAndSizeStatistic.class))
            .add(injector.getInstance(CourseOverGroundStatistic.class))
            .add(injector.getInstance(SpeedOverGroundStatistic.class))
            .build();

        Iterator<TrackingEventListener> statisticIterator = this.statistics.iterator();
        while (statisticIterator.hasNext()) {
            statisticIterator.next().start();
        }
    }

    private final class Task implements StripedRunnable {
        final AisPacket packet;
        final Object stripe;

        public Task(AisPacket packet, Object stripe) {
            this.packet = packet;
            this.stripe = stripe;
        }

        @Override
        public void run() {
            try {
                doWork(packet);
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
