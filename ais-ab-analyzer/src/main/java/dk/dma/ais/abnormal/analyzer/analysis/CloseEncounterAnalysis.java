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

import com.google.common.eventbus.AllowConcurrentEvents;
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

/**
 * This analysis manages events where two vessels have a close encounter and therefore
 * are in risk of collision.
 */
public class CloseEncounterAnalysis extends Analysis {
    private static final Logger LOG = LoggerFactory.getLogger(CloseEncounterAnalysis.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final AppStatisticsService statisticsService;

    private final String analysisName;

    @Inject
    public CloseEncounterAnalysis(AppStatisticsService statisticsService, TrackingService trackingService, EventRepository eventRepository) {
        super(eventRepository, trackingService);
        this.statisticsService = statisticsService;
        this.analysisName = this.getClass().getSimpleName();
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onMark(TimeEvent timeEvent) {
        LOG.debug(timeEvent.toString());
        statisticsService.incAnalysisStatistics(analysisName, "Analyses performed");
    }

    @Override
    protected Event buildEvent(Track track) {
        return null;
    }

}
