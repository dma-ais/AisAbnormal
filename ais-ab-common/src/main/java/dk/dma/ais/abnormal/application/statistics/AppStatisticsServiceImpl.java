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
package dk.dma.ais.abnormal.application.statistics;

import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class for holding information on the file processing process
 */
@Singleton
public class AppStatisticsServiceImpl implements AppStatisticsService {

    private static final Logger LOG = LoggerFactory.getLogger(AppStatisticsServiceImpl.class);

    private static final long DEFAULT_LOG_INTERVAL_SECONDS = 3600;
    private final long logInterval;
    private final AtomicLong lastLog = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong lastMessageCount = new AtomicLong(0);

    private final AtomicLong unfilteredPacketCount = new AtomicLong(0);
    private final AtomicLong filteredPacketCount = new AtomicLong(0);
    private final AtomicLong messageCount = new AtomicLong(0);
    private final AtomicLong messagesOutOfSequence = new AtomicLong(0);
    private final AtomicLong posMsgCount = new AtomicLong(0);
    private final AtomicLong statMsgCount = new AtomicLong(0);
    private final AtomicInteger trackCount = new AtomicInteger(0);

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    public AppStatisticsServiceImpl() {
        this.logInterval = DEFAULT_LOG_INTERVAL_SECONDS;
    }

    @Override
    public void start() {
        LOG.debug("Starting statistics service.");
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
            dumpStatistics();
            }
        }, 0 /* logInterval */, logInterval, TimeUnit.SECONDS);
        LOG.info("Statistics service started.");
    }

    @Override
    public void stop() {
        LOG.debug("Stopping statistics service.");
        scheduledExecutorService.shutdown();
        try {
            scheduledExecutorService.awaitTermination(2*logInterval, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
        }
        LOG.info("Statistics service stopped.");
    }

    @Override
    public final long getFilteredPacketCount() {
        return filteredPacketCount.get();
    }

    @Override
    public final long getMessageCount() {
        return messageCount.get();
    }

    @Override
    public final long getPosMsgCount() {
        return posMsgCount.get();
    }

    @Override
    public final long getStatMsgCount() {
        return statMsgCount.get();
    }

    @Override
    public final void incFilteredPacketCount() {
        filteredPacketCount.incrementAndGet();
    }

    @Override
    public final void incUnfilteredPacketCount() {
        this.unfilteredPacketCount.incrementAndGet();
    }

    @Override
    public final void incMessageCount() {
        messageCount.incrementAndGet();
    }

    @Override
    public final void incPosMsgCount() {
        posMsgCount.incrementAndGet();
    }

    @Override
    public final void incStatMsgCount() {
        statMsgCount.incrementAndGet();
    }

    @Override
    public final void incOutOfSequenceMessages() {
       messagesOutOfSequence.incrementAndGet();
    }

    @Override
    public void setTrackCount(int trackCount) {
        this.trackCount.set(trackCount);
    }

    protected double getMessageRate() {
        double secs = (double)(System.currentTimeMillis() - lastLog.get()) / 1000.0;
        long msgs = messageCount.get() - lastMessageCount.get();

        lastLog.set(System.currentTimeMillis());
        lastMessageCount.set(messageCount.get());

        return (double) msgs / secs;
    }

    @Override
    public void dumpStatistics() {
        LOG.info("==== AIS Abnormal Behavior application statistics ====");

        LOG.info("==== Application statistics ====");
        LOG.info(String.format("%-30s %9d", "Unfiltered packet count", unfilteredPacketCount.get()));
        LOG.info(String.format("%-30s %9d", "Filtered packet count", filteredPacketCount.get()));
        LOG.info(String.format("%-30s %9d", "Message count", messageCount.get()));
        LOG.info(String.format("%-30s %9d", "Messages out of sequence", messagesOutOfSequence.get()));
        LOG.info(String.format("%-30s %9d", "Pos message count", posMsgCount.get()));
        LOG.info(String.format("%-30s %9d", "Stat message count", statMsgCount.get()));
        LOG.info(String.format("%-30s %9d", "Track count", trackCount.get()));
        LOG.info(String.format("%-30s %9.0f msg/sec", "Message rate", getMessageRate()));
        LOG.info("==== Application statistics ====");
    }
}
