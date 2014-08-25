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
package dk.dma.ais.abnormal.analyzer;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Injector;
import dk.dma.ais.abnormal.analyzer.analysis.Analysis;
import dk.dma.ais.abnormal.analyzer.analysis.CloseEncounterAnalysis;
import dk.dma.ais.abnormal.analyzer.analysis.CourseOverGroundAnalysis;
import dk.dma.ais.abnormal.analyzer.analysis.DriftAnalysis;
import dk.dma.ais.abnormal.analyzer.analysis.ShipTypeAndSizeAnalysis;
import dk.dma.ais.abnormal.analyzer.analysis.SpeedOverGroundAnalysis;
import dk.dma.ais.abnormal.analyzer.analysis.SuddenSpeedChangeAnalysis;
import dk.dma.ais.abnormal.tracker.Tracker;
import dk.dma.ais.filter.GeoMaskFilter;
import dk.dma.ais.filter.LocationFilter;
import dk.dma.ais.filter.ReplayDownSampleFilter;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.IPositionMessage;
import dk.dma.ais.packet.AisPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Handler for read AIS packets
 */
public class PacketHandlerImpl implements PacketHandler {

    static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final AppStatisticsService statisticsService;
    private final Tracker tracker;
    private final ReplayDownSampleFilter downSampleFilter;
    private final GeoMaskFilter geoMaskFilter;
    private final LocationFilter locationFilter;
    private final Set<Analysis> analyses;

    @Inject
    public PacketHandlerImpl(AppStatisticsService statisticsService, Tracker tracker, ReplayDownSampleFilter downSampleFilter, GeoMaskFilter geoMaskFilter, LocationFilter locationFilter) {
        this.statisticsService = statisticsService;
        this.tracker = tracker;
        this.downSampleFilter = downSampleFilter;
        this.geoMaskFilter = geoMaskFilter;
        this.locationFilter = locationFilter;
        this.analyses = initAnalyses();

        this.analyses.forEach(analysis -> analysis.start());
    }

    public void accept(final AisPacket packet) {
        statisticsService.incUnfilteredPacketCount();

        if (downSampleFilter.rejectedByFilter(packet)) {
            return;
        }

        if (locationFilter.rejectedByFilter(packet)) {
            return;
        }

        if (geoMaskFilter.rejectedByFilter(packet)) {
            return;
        }

        statisticsService.incFilteredPacketCount();
        long n = statisticsService.getFilteredPacketCount();
        if (n % 100000L == 0) {
            LOG.debug(n + " packets passed through filter.");
        }

        // Unpack and validate AIS packet
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

        doWork(packet);
    }

    private void doWork(AisPacket packet) {
        tracker.update(packet);
    }

    private static Set<Analysis> initAnalyses() {
        Injector injector = AbnormalAnalyzerApp.getInjector();

        Set<Analysis> analyses = new ImmutableSet.Builder<Analysis>()
            .add(injector.getInstance(CourseOverGroundAnalysis.class))
            .add(injector.getInstance(SpeedOverGroundAnalysis.class))
            .add(injector.getInstance(ShipTypeAndSizeAnalysis.class))
            .add(injector.getInstance(SuddenSpeedChangeAnalysis.class))
            .add(injector.getInstance(DriftAnalysis.class))
            .add(injector.getInstance(CloseEncounterAnalysis.class))
            .build();

        return analyses;
    }

}
