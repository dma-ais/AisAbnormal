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
import com.google.inject.Inject;
import dk.dma.ais.abnormal.analyzer.AppStatisticsService;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.abnormal.tracker.events.TimeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * This analysis manages events where two vessels have a close encounter and therefore
 * are in risk of collision.
 *
 * This analysis is rather extensive, and we can therefore now allow to block the EventBus
 * for the duration of a complete analysis. Instead the worked is spawned to a separate worker
 * thread.
 */
public class CloseEncounterAnalysis extends Analysis {
    private static final Logger LOG = LoggerFactory.getLogger(CloseEncounterAnalysis.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final AppStatisticsService statisticsService;
    private final String analysisName;

    /**
     * Minimum no. of msecs between runs of this analysis.
     */
    private final static int ANALYS_PERIOD_MILLIS = 5 * 60 * 1000;

    /**
     * The time when the analysis should next be run.
     */
    private Date nextRunTime;

    /**
     * Executor to perform the actual work.
     */
    private Executor executor = Executors.newSingleThreadExecutor();

    @Inject
    public CloseEncounterAnalysis(AppStatisticsService statisticsService, TrackingService trackingService, EventRepository eventRepository) {
        super(eventRepository, trackingService, null);
        this.statisticsService = statisticsService;
        this.analysisName = this.getClass().getSimpleName();
        this.nextRunTime = new Date(0L);
    }

    @Subscribe
    public void onMark(TimeEvent timeEvent) {
        LOG.debug(timeEvent.toString());

        Date now = new Date(timeEvent.getTimestamp());

        if (nextRunTime.before(now)) {
            executor.execute(() -> performAnalysis());
            nextRunTime = new Date(now.getTime() + ANALYS_PERIOD_MILLIS);
            LOG.debug("nextRunTime: " + nextRunTime);
        }
    }

    private void performAnalysis() {
        LOG.debug("Starting " + analysisName);

        Set<Track> tracks = getTrackingService().cloneTracks();
        tracks.forEach(t -> performAnalysis(tracks, t));

        statisticsService.incAnalysisStatistics(analysisName, "Analyses performed");
        LOG.debug("Finished " + analysisName);
    }

    private void performAnalysis(Set<Track> tracks, Track track) {
    }

    @Override
    protected Event buildEvent(Track track) {
        return null;
    }

}
