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

package dk.dma.ais.abnormal.analyzer.analysis;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.MoreExecutors;
import dk.dma.ais.abnormal.analyzer.behaviour.BehaviourManager;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.tracker.eventEmittingTracker.EventEmittingTracker;
import dk.dma.ais.tracker.eventEmittingTracker.events.TimeEvent;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.Executor;

/**
 * An Analysis is a class which is known to the ais-ab-analyzer application and possesses certain public
 * methods which can be called to analyze and detect events.
 *
 * The Analysis class is an abstract class which all analyses must inherit from.
 *
 * The Analysis class provides basic methods to its subclasses, so they can reuse the code to raise and
 * lower events.
 *
 * @author Thomas Borg Salling <tbsalling@tbsalling.dk>
 *
 */
@NotThreadSafe
public abstract class PeriodicAnalysis extends Analysis {

    private static final Logger LOG = LoggerFactory.getLogger(PeriodicAnalysis.class);

    /** Minimum no. of msecs between runs of this analysis. */
    private int analysisPeriodMillis = Integer.MAX_VALUE;

    /** The time when the next analysis should be run. */
    private long currentRunTime = -1L;

    /** The time when the next analysis should be run. */
    private long nextRunTime = 0L;

    /** Executor to perform the actual work. */
    private final Executor executor = MoreExecutors.directExecutor();

    public PeriodicAnalysis(EventRepository eventRepository, EventEmittingTracker trackingService, BehaviourManager behaviourManager) {
        super(eventRepository, trackingService, behaviourManager);
    }

    @Override
    public String toString() {
        return "PeriodicAnalysis{" +
                "analysisPeriodMillis=" + analysisPeriodMillis +
                "} " + super.toString();
    }

    /** Perform the actual analysis */
    protected abstract void performAnalysis();

    protected void setAnalysisPeriodMillis(int analysisPeriodMillis) {
        this.analysisPeriodMillis = analysisPeriodMillis;
    }

    @Subscribe
    public void onMark(TimeEvent timeEvent) {
        final long now = timeEvent.getTimestamp();
        if (nextRunTime <= now) {
            currentRunTime = now;
            if (LOG.isDebugEnabled()) {
                LOG.debug("currentRunTime: " + new Date(currentRunTime) + " " + currentRunTime + " (nextRunTime was " + new Date(nextRunTime) + " " + nextRunTime + ")");
            }
            executor.execute(() -> performAnalysis());
            currentRunTime = -1L;
            nextRunTime = now + analysisPeriodMillis;
            if (LOG.isDebugEnabled()) {
                LOG.debug("nextRunTime: " + new Date(nextRunTime) + " " + nextRunTime);
            }
        }
    }

    public long getCurrentRunTime() {
        return currentRunTime;
    }

    public long getNextRunTime() {
        return nextRunTime;
    }
}