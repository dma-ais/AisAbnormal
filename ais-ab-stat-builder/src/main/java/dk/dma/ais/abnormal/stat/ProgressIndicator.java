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

import com.google.inject.Inject;
import dk.dma.ais.reader.AisDirectoryReader;
import dk.dma.ais.reader.AisReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ProgressIndicator {

    private static final Logger LOG = LoggerFactory.getLogger(ProgressIndicator.class);

    private final AisReader reader;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private long startTime;
    private static final int PROGRESS_INDICATION_PERIOD_MINUTES = 10;

    @Inject
    public ProgressIndicator(AisReader reader) {
        this.reader = reader;
        LOG.info("ProgressIndicator created (" + this + ").");
    }

    public void init() {
        LOG.debug("reader: " + reader);
        if (reader instanceof AisDirectoryReader) {
            LOG.info("Scanning input files...");
            ((AisDirectoryReader) reader).getEstimatedFractionOfPacketsRead();
            LOG.debug("Scanning input files... done.");
        }
    }

    public void start() {
        LOG.debug("Starting progress indicator.");

        startTime = System.currentTimeMillis();

        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                showProgress();
            }

            private void showProgress() {
                if (reader instanceof AisDirectoryReader) {
                    final float fractionCompleted = ((AisDirectoryReader) reader).getEstimatedFractionOfPacketsRead();
                    final NumberFormat pctFormatter = NumberFormat.getPercentInstance();
                    final String pctComplete = pctFormatter.format(fractionCompleted);
                    final long elapsedMillis = System.currentTimeMillis() - startTime;
                    final float pace = fractionCompleted / elapsedMillis;  // Pace since start (should use pace over last hour?)
                    final float fractionRemaining = 1 - fractionCompleted;
                    final long remainingMillis = (long) (fractionRemaining / pace);
                    LOG.debug("elapsedMillis: " + elapsedMillis + ", pace: " + pace + ", fractionRemaining: " + fractionRemaining + ", remainingMillis: " + remainingMillis);
                    final long estimatedTimeOfCompletion = startTime + elapsedMillis + remainingMillis;
                    LOG.info("Training is " + pctComplete + " complete. Estimated time of completion is " + new Date(estimatedTimeOfCompletion) + ".");
                }
            }
        }, 1 /* early output */, PROGRESS_INDICATION_PERIOD_MINUTES, TimeUnit.MINUTES);
    }

    public void shutdown() {
        LOG.debug("Stopping progress indicator.");
        scheduledExecutorService.shutdownNow();
        try {
            scheduledExecutorService.awaitTermination(PROGRESS_INDICATION_PERIOD_MINUTES * 2, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
