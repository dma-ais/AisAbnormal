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
import dk.dma.ais.abnormal.analyzer.behaviour.BehaviourManager;
import dk.dma.ais.abnormal.analyzer.behaviour.EventCertainty;
import dk.dma.ais.abnormal.analyzer.behaviour.events.AbnormalEventLower;
import dk.dma.ais.abnormal.analyzer.behaviour.events.AbnormalEventMaintain;
import dk.dma.ais.abnormal.analyzer.behaviour.events.AbnormalEventRaise;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.SpeedOverGroundEvent;
import dk.dma.ais.abnormal.event.db.domain.TrackingPoint;
import dk.dma.ais.abnormal.stat.db.StatisticDataRepository;
import dk.dma.ais.abnormal.stat.db.data.SpeedOverGroundStatisticData;
import dk.dma.ais.abnormal.stat.db.data.StatisticData;
import dk.dma.ais.abnormal.tracker.InterpolatedTrackingReport;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.Tracker;
import dk.dma.ais.abnormal.tracker.events.CellChangedEvent;
import dk.dma.ais.abnormal.tracker.events.TrackStaleEvent;
import dk.dma.ais.abnormal.util.Categorizer;
import dk.dma.enav.model.geometry.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static dk.dma.ais.abnormal.event.db.domain.builders.SpeedOverGroundEventBuilder.SpeedOverGroundEvent;
import static dk.dma.ais.abnormal.util.AisDataHelper.nameOrMmsi;
import static dk.dma.ais.abnormal.util.TrackPredicates.isClassB;
import static dk.dma.ais.abnormal.util.TrackPredicates.isEngagedInTowing;
import static dk.dma.ais.abnormal.util.TrackPredicates.isFishingVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isSmallVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isSpecialCraft;
import static dk.dma.ais.abnormal.util.TrackPredicates.isUnknownTypeOrSize;

/**
 * This analysis manages events where a vessel has an "abnormal" speed over ground
 * relative to the previous observations for vessels in the same grid cell. Statistics
 * for previous observations are stored in the StatisticDataRepository.
 */
public class SpeedOverGroundAnalysis extends StatisticBasedAnalysis {
    private static final Logger LOG = LoggerFactory.getLogger(SpeedOverGroundAnalysis.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final AppStatisticsService statisticsService;

    static final int TOTAL_SHIP_COUNT_THRESHOLD = 1000;

    @Inject
    public SpeedOverGroundAnalysis(AppStatisticsService statisticsService, StatisticDataRepository statisticsRepository, Tracker trackingService, EventRepository eventRepository, BehaviourManager behaviourManager) {
        super(eventRepository, statisticsRepository, trackingService, behaviourManager);
        this.statisticsService = statisticsService;
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onCellIdChanged(CellChangedEvent trackEvent) {
        statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Events received");

        Track track = trackEvent.getTrack();

        if (isClassB.test(track) || isUnknownTypeOrSize.test(track) || isFishingVessel.test(track) || isSmallVessel.test(track) || isSpecialCraft.test(track) || isEngagedInTowing.test(track)) {
            return;
        }

        Long cellId = (Long) track.getProperty(Track.CELL_ID);
        Integer shipType = track.getShipType();
        Integer shipLength = track.getVesselLength();
        Float speedOverGround = track.getSpeedOverGround();

        if (cellId == null) {
            statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Unknown mmsi");
            return;
        }

        if (shipType == null) {
            statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Unknown ship type");
            return;
        }

        if (shipLength == null) {
            statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Unknown ship length");
            return;
        }

        if (speedOverGround == null) {
            statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Unknown course over ground");
            return;
        }

        int shipTypeKey = Categorizer.mapShipTypeToCategory(shipType) - 1;
        int shipLengthKey = Categorizer.mapShipLengthToCategory(shipLength) - 1;
        int speedOverGroundKey = Categorizer.mapSpeedOverGroundToCategory(speedOverGround) - 1;

        if (isAbnormalSpeedOverGround(cellId, shipTypeKey, shipLengthKey, speedOverGroundKey)) {
            getBehaviourManager().abnormalBehaviourDetected(SpeedOverGroundEvent.class, track);
        } else {
            getBehaviourManager().normalBehaviourDetected(SpeedOverGroundEvent.class, track);
        }
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onTrackStale(TrackStaleEvent trackEvent) {
        getBehaviourManager().trackStaleDetected(SpeedOverGroundEvent.class, trackEvent.getTrack());
        lowerExistingAbnormalEventIfExists(SpeedOverGroundEvent.class, trackEvent.getTrack());
    }

    @Subscribe
    public void onAbnormalEventRaise(AbnormalEventRaise behaviourEvent) {
        LOG.debug("onAbnormalEventRaise " + behaviourEvent.getTrack().getMmsi());
        if (behaviourEvent.getEventClass().equals(SpeedOverGroundEvent.class)) {
            raiseOrMaintainAbnormalEvent(SpeedOverGroundEvent.class, behaviourEvent.getTrack());
        }
    }
    @Subscribe
    public void onAbnormalEventMaintain(AbnormalEventMaintain behaviourEvent) {
        LOG.debug("onAbnormalEventMaintain " + behaviourEvent.getTrack().getMmsi());
        if (behaviourEvent.getEventClass().equals(SpeedOverGroundEvent.class)) {
            raiseOrMaintainAbnormalEvent(SpeedOverGroundEvent.class, behaviourEvent.getTrack());
        }
    }

    @Subscribe
    public void onAbnormalEventLower(AbnormalEventLower behaviourEvent) {
        LOG.debug("onAbnormalEventLower " + behaviourEvent.getTrack().getMmsi());
        if (behaviourEvent.getEventClass().equals(SpeedOverGroundEvent.class)) {
            lowerExistingAbnormalEventIfExists(SpeedOverGroundEvent.class, behaviourEvent.getTrack());
        }
    }

    /**
     * If the probability p(d)<0.001 and total count>1000 then abnormal. p(d)=sum(count)/count for all sog_intervals for
     * that shiptype and size.
     *
     * @param cellId
     * @param shipTypeKey
     * @param shipSizeKey
     * @return true if the presence of size/type with this sog in this cell is abnormal. False otherwise.
     */
    boolean isAbnormalSpeedOverGround(Long cellId, int shipTypeKey, int shipSizeKey, int speedOverGroundKey) {
        float pd = 1.0f;

        StatisticData speedOverGroundStatisticData = getStatisticDataRepository().getStatisticData("SpeedOverGroundStatistic", cellId);

        if (speedOverGroundStatisticData instanceof SpeedOverGroundStatisticData) {
            Integer totalCount  = ((SpeedOverGroundStatisticData) speedOverGroundStatisticData).getSumFor(SpeedOverGroundStatisticData.STAT_SHIP_COUNT);
            if (totalCount > TOTAL_SHIP_COUNT_THRESHOLD) {
                Integer shipCount = ((SpeedOverGroundStatisticData) speedOverGroundStatisticData).getValue(shipTypeKey, shipSizeKey, speedOverGroundKey, SpeedOverGroundStatisticData.STAT_SHIP_COUNT);
                if (shipCount == null) {
                    shipCount = 0;
                }
                pd = (float) shipCount / (float) totalCount;
                LOG.debug("cellId=" + cellId + ", shipType=" + shipTypeKey + ", shipSize=" + shipSizeKey + ", sog=" + speedOverGroundKey + ", shipCount=" + shipCount + ", totalCount=" + totalCount + ", pd=" + pd);
            } else {
                LOG.debug("totalCount of " + totalCount + " is not enough statistical data for cell " + cellId);
            }
        }

        LOG.debug("pd = " + pd);

        boolean isAbnormalSpeedOverGround = pd < 0.001;
        if (isAbnormalSpeedOverGround) {
            LOG.debug("Abnormal event detected.");
        } else {
            LOG.debug("Normal or inconclusive event detected.");
        }

        statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Analyses performed");

        return isAbnormalSpeedOverGround;
    }

    @Override
    protected Event buildEvent(Track track, Track... otherTracks) {
        if (otherTracks != null && otherTracks.length > 0) {
            throw new IllegalArgumentException("otherTracks not supported.");
        }

        Integer mmsi = track.getMmsi();
        Integer imo = track.getIMO();
        String callsign = track.getCallsign();
        String name = nameOrMmsi(track.getShipName(), mmsi);
        Integer shipType = track.getShipType();
        Integer shipLength = track.getVesselLength();
        Date positionTimestamp = new Date(track.getTimeOfLastPositionReport());
        Position position = track.getPosition();
        Float cog = track.getCourseOverGround();
        Float sog = track.getSpeedOverGround();
        Boolean interpolated = track.getNewestTrackingReport() instanceof InterpolatedTrackingReport;

        TrackingPoint.EventCertainty certainty = TrackingPoint.EventCertainty.UNDEFINED;
        EventCertainty eventCertainty = getBehaviourManager().getEventCertaintyAtCurrentPosition(SpeedOverGroundEvent.class, track);
        if (eventCertainty != null) {
            certainty = TrackingPoint.EventCertainty.create(eventCertainty.getCertainty());
        }

        short shipTypeCategory = Categorizer.mapShipTypeToCategory(shipType);
        short shipLengthCategory = Categorizer.mapShipLengthToCategory(shipLength);
        short speedOverGroundCategory = Categorizer.mapSpeedOverGroundToCategory(sog);

        String shipTypeAsString = Categorizer.mapShipTypeCategoryToString(shipTypeCategory);
        String shipLengthAsString = Categorizer.mapShipSizeCategoryToString(shipLengthCategory);
        String speedOverGroundAsString = Categorizer.mapSpeedOverGroundCategoryToString(speedOverGroundCategory);

        String title = "Abnormal speed over ground";
        String description = String.format("Abnormal speed over ground of " + name + " (" + shipTypeAsString + ") on position " + position + " at " + DATE_FORMAT.format(positionTimestamp) + ": cog:%.0f(%s) sog:%.1f(%s) type:%d(%s) size:%d(%s).", cog, speedOverGroundAsString, sog, speedOverGroundAsString, shipType, shipTypeAsString, shipLength, shipLengthAsString);

        LOG.info(description);

        Event event =
                SpeedOverGroundEvent()
                        .shipType(shipTypeCategory)
                        .shipLength(shipLengthCategory)
                        .speedOverGround(speedOverGroundCategory)
                        .title(title)
                        .description(description)
                        .startTime(positionTimestamp)
                        .behaviour()
                            .isPrimary(true)
                            .vessel()
                                .mmsi(mmsi)
                                .imo(imo)
                                .callsign(callsign)
                                .name(name)
                            .trackingPoint()
                                .timestamp(positionTimestamp)
                                .positionInterpolated(interpolated)
                                .eventCertainty(certainty)
                                .speedOverGround(sog)
                                .courseOverGround(cog)
                                .latitude(position.getLatitude())
                                .longitude(position.getLongitude())
                .getEvent();

        addPreviousTrackingPoints(event, track);

        statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Events raised");

        return event;
    }
}
