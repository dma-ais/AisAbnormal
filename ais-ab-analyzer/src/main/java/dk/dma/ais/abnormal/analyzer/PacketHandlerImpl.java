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
import com.google.inject.name.Named;
import dk.dma.ais.abnormal.analyzer.analysis.Analysis;
import dk.dma.ais.abnormal.analyzer.analysis.CloseEncounterAnalysis;
import dk.dma.ais.abnormal.analyzer.analysis.CourseOverGroundAnalysis;
import dk.dma.ais.abnormal.analyzer.analysis.DriftAnalysis;
import dk.dma.ais.abnormal.analyzer.analysis.FreeFlowAnalysis;
import dk.dma.ais.abnormal.analyzer.analysis.ShipTypeAndSizeAnalysis;
import dk.dma.ais.abnormal.analyzer.analysis.SpeedOverGroundAnalysis;
import dk.dma.ais.abnormal.analyzer.analysis.SuddenSpeedChangeAnalysis;
import dk.dma.ais.filter.IPacketFilter;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.IPositionMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.tracker.Tracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Handler for read AIS packets
 */
public class PacketHandlerImpl implements PacketHandler {

    static final Logger LOG = LoggerFactory.getLogger(PacketHandlerImpl.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final AppStatisticsService statisticsService;
    private final Tracker tracker;

    private final Set<IPacketFilter> filters;
    private final Predicate<AisPacket> shipNameFilter;

    private final Set<Analysis> analyses;

    @Inject
    public PacketHandlerImpl(AppStatisticsService statisticsService,
                             Tracker tracker,
                             Set<IPacketFilter> filters,
                             @Named("shipNameFilter") Predicate<AisPacket> shipNameFilter) {
        this.statisticsService = statisticsService;
        this.tracker = tracker;
        this.filters = filters;
        this.shipNameFilter = shipNameFilter;
        this.analyses = initAnalyses();

        this.analyses.forEach(analysis -> analysis.start());
    }

    /**
     * Receive and process one AisPacket.
     *
     * @param packet The AisPacket to process.
     */
    public void accept(final AisPacket packet) {
        statisticsService.incUnfilteredPacketCount();
        if (filterPacket(packet)) {
            statisticsService.incFilteredPacketCount();

            AisMessage message = packet.tryGetAisMessage();
            if (message == null) {
                LOG.warn("Invalid packet: " + packet.getStringMessage());
                return;
            }
            updateApplicationStatistics(message);

            doWork(packet);
        }
    }

    /**
     * Returns true if packet passes all packet filters
     * @param packet
     * @return
     */
    private boolean filterPacket(AisPacket packet) {
        final boolean[] rejected = {false}; // TODO Use Groovy or Scala...
        filters.forEach(f -> {
            if (!rejected[0] && f.rejectedByFilter(packet)) {
                rejected[0] = true;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Packet dropped due to " + f.getClass().getSimpleName());
                }
            }
        });

        boolean shipNameFilterPassed = ! shipNameFilter.test(packet);
        if (!shipNameFilterPassed && LOG.isDebugEnabled()) {
            LOG.debug("Packet dropped due to shipNameFilter");
        }

        boolean filterPassed = !rejected[0] && shipNameFilterPassed;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Packet " + (filterPassed ? "passed":"dropped") + ": " + packet.getStringMessage());
        }

        return filterPassed;
    }

    private void updateApplicationStatistics(AisMessage message) {
        statisticsService.incMessageCount();

        if (message instanceof IPositionMessage) {
            statisticsService.incPosMsgCount();
        } else if (message instanceof AisMessage5) {
            statisticsService.incStatMsgCount();
        }
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
            .add(injector.getInstance(FreeFlowAnalysis.class))
            .build();

        return analyses;
    }

}
