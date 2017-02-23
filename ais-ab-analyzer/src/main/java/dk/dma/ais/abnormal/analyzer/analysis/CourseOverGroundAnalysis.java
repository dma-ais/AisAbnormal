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
import dk.dma.ais.abnormal.event.db.domain.CourseOverGroundEvent;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.TrackingPoint;
import dk.dma.ais.abnormal.stat.db.StatisticDataRepository;
import dk.dma.ais.abnormal.stat.db.data.CourseOverGroundStatisticData;
import dk.dma.ais.abnormal.stat.db.data.StatisticData;
import dk.dma.ais.abnormal.util.Categorizer;
import dk.dma.ais.tracker.eventEmittingTracker.EventEmittingTracker;
import dk.dma.ais.tracker.eventEmittingTracker.InterpolatedTrackingReport;
import dk.dma.ais.tracker.eventEmittingTracker.Track;
import dk.dma.ais.tracker.eventEmittingTracker.events.CellChangedEvent;
import dk.dma.ais.tracker.eventEmittingTracker.events.TrackStaleEvent;
import dk.dma.enav.model.geometry.Position;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_COG_CELL_SHIPCOUNT_MIN;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_COG_PD;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_COG_PREDICTIONTIME_MAX;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_COG_SHIPLENGTH_MIN;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_COG_USE_AGGREGATED_STATS;
import static dk.dma.ais.abnormal.event.db.domain.builders.CourseOverGroundEventBuilder.CourseOverGroundEvent;
import static dk.dma.ais.abnormal.util.AisDataHelper.nameOrMmsi;
import static dk.dma.ais.abnormal.util.TrackPredicates.isClassB;
import static dk.dma.ais.abnormal.util.TrackPredicates.isEngagedInTowing;
import static dk.dma.ais.abnormal.util.TrackPredicates.isFishingVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isSlowVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isSmallVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isSpecialCraft;
import static dk.dma.ais.abnormal.util.TrackPredicates.isUnknownTypeOrSize;

/**
 * This analysis manages events where a vessel has an "abnormal" course over ground
 * relative to the previous observations for vessels in the same grid cell. Statistics
 * for previous observations are stored in the StatisticDataRepository.
 *
 * @author Thomas Borg Salling <tbsalling@tbsalling.dk>
 */
public class CourseOverGroundAnalysis extends StatisticBasedAnalysis {
    private static final Logger LOG = LoggerFactory.getLogger(CourseOverGroundAnalysis.class);

    private final AppStatisticsService statisticsService;

    private final int TOTAL_SHIP_COUNT_THRESHOLD;
    private final float PD;
    private final int SHIP_LENGTH_MIN;
    private final boolean USE_AGGREGATED_STATS;

    @Inject
    public CourseOverGroundAnalysis(Configuration configuration, AppStatisticsService statisticsService, StatisticDataRepository statisticsRepository, EventEmittingTracker trackingService, EventRepository eventRepository, BehaviourManager behaviourManager) {
        super(eventRepository, statisticsRepository, trackingService, behaviourManager);
        this.statisticsService = statisticsService;
        setTrackPredictionTimeMax(configuration.getInteger(CONFKEY_ANALYSIS_COG_PREDICTIONTIME_MAX, -1));

        TOTAL_SHIP_COUNT_THRESHOLD = configuration.getInt(CONFKEY_ANALYSIS_COG_CELL_SHIPCOUNT_MIN, 1000);
        PD = configuration.getFloat(CONFKEY_ANALYSIS_COG_PD, 0.001f);
        SHIP_LENGTH_MIN = configuration.getInt(CONFKEY_ANALYSIS_COG_SHIPLENGTH_MIN, 50);
        USE_AGGREGATED_STATS = configuration.getBoolean(CONFKEY_ANALYSIS_COG_USE_AGGREGATED_STATS, false);

        LOG.info(getAnalysisName() + " created (" + this + ").");
    }

    @Override
    public String toString() {
        return "CourseOverGroundAnalysis{" +
                "TOTAL_SHIP_COUNT_THRESHOLD=" + TOTAL_SHIP_COUNT_THRESHOLD +
                ", PD=" + PD +
                ", SHIP_LENGTH_MIN=" + SHIP_LENGTH_MIN +
                ", USE_AGGREGATED_STATS=" + USE_AGGREGATED_STATS +
                "} " + super.toString();
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onCellIdChanged(CellChangedEvent trackEvent) {
        statisticsService.incAnalysisStatistics(getAnalysisName(), "Events received");

        Track track = trackEvent.getTrack();

        if (isClassB.test(track) || isUnknownTypeOrSize.test(track) || isFishingVessel.test(track) || isSlowVessel.test(track) || isSmallVessel.test(track) || isSpecialCraft.test(track) || isEngagedInTowing.test(track)) {
            return;
        }

        /* Skip analysis if track has been predicted forward for too long */
        if (isLastAisTrackingReportTooOld(track, track.getTimeOfLastPositionReport())) {
            LOG.debug("Skipping analysis: MMSI " + track.getMmsi() + " was predicted for too long.");
            return;
        }

        Long cellId = (Long) track.getProperty(Track.CELL_ID);
        Integer shipType = track.getShipType();
        Integer shipLength = track.getVesselLength();
        Float courseOverGround = track.getCourseOverGround();

        if (cellId == null) {
            statisticsService.incAnalysisStatistics(getAnalysisName(), "Unknown cell id");
            return;
        }

        if (shipType == null) {
            statisticsService.incAnalysisStatistics(getAnalysisName(), "Unknown ship type");
            return;
        }

        if (shipLength == null) {
            statisticsService.incAnalysisStatistics(getAnalysisName(), "Unknown ship length");
            return;
        }

        if (courseOverGround == null) {
            statisticsService.incAnalysisStatistics(getAnalysisName(), "Unknown course over ground");
            return;
        }

        if (shipLength < SHIP_LENGTH_MIN) {
            statisticsService.incAnalysisStatistics(getAnalysisName(), "LOA < " + SHIP_LENGTH_MIN);
            return;
        }

        int shipTypeKey = Categorizer.mapShipTypeToCategory(shipType) - 1;
        int shipLengthKey = Categorizer.mapShipLengthToCategory(shipLength) - 1;
        int courseOverGroundKey = Categorizer.mapCourseOverGroundToCategory(courseOverGround) - 1;

        if (isAbnormalCourseOverGround(cellId, shipTypeKey, shipLengthKey, courseOverGroundKey)) {
            getBehaviourManager().abnormalBehaviourDetected(CourseOverGroundEvent.class, track);
        } else {
            getBehaviourManager().normalBehaviourDetected(CourseOverGroundEvent.class, track);
        }
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onTrackStale(TrackStaleEvent trackEvent) {
        getBehaviourManager().trackStaleDetected(CourseOverGroundEvent.class, trackEvent.getTrack());
        lowerExistingAbnormalEventIfExists(CourseOverGroundEvent.class, trackEvent.getTrack());
    }

    @Subscribe
    public void onAbnormalEventRaise(AbnormalEventRaise behaviourEvent) {
        LOG.debug("onAbnormalEventRaise " + behaviourEvent.getTrack().getMmsi());
        if (behaviourEvent.getEventClass().equals(CourseOverGroundEvent.class)) {
            raiseOrMaintainAbnormalEvent(CourseOverGroundEvent.class, behaviourEvent.getTrack());
        }
    }
    @Subscribe
    public void onAbnormalEventMaintain(AbnormalEventMaintain behaviourEvent) {
        LOG.debug("onAbnormalEventMaintain " + behaviourEvent.getTrack().getMmsi());
        if (behaviourEvent.getEventClass().equals(CourseOverGroundEvent.class)) {
            raiseOrMaintainAbnormalEvent(CourseOverGroundEvent.class, behaviourEvent.getTrack());
        }
    }

    @Subscribe
    public void onAbnormalEventLower(AbnormalEventLower behaviourEvent) {
        LOG.debug("onAbnormalEventLower " + behaviourEvent.getTrack().getMmsi());
        if (behaviourEvent.getEventClass().equals(CourseOverGroundEvent.class)) {
            lowerExistingAbnormalEventIfExists(CourseOverGroundEvent.class, behaviourEvent.getTrack());
        }
    }

    /**
     * If the probability p(d)<PD and total count>TOTAL_SHIP_COUNT_THRESHOLD then abnormal. p(d)=sum(count)/count for all sog_intervals for
     * that shiptype and size.
     *
     * @param cellId
     * @param shipTypeKey
     * @param shipSizeKey
     * @param courseOverGroundKey
     * @return true if the presence of size/type with this cog in this cell is abnormal. False otherwise.
     */
    boolean isAbnormalCourseOverGround(Long cellId, int shipTypeKey, int shipSizeKey, int courseOverGroundKey) {
        float pd = 1.0f;

        StatisticData courseOverGroundStatisticData = getStatisticDataRepository().getStatisticData("CourseOverGroundStatistic", cellId);

        if (courseOverGroundStatisticData instanceof CourseOverGroundStatisticData) {
            Integer totalCount  = ((CourseOverGroundStatisticData) courseOverGroundStatisticData).getSumFor(CourseOverGroundStatisticData.STAT_SHIP_COUNT);
            if (totalCount > TOTAL_SHIP_COUNT_THRESHOLD) {
                int shipCount = calculateShipCount((CourseOverGroundStatisticData) courseOverGroundStatisticData, shipTypeKey, shipSizeKey, courseOverGroundKey);
                pd = (float) shipCount / (float) totalCount;
                LOG.debug("cellId=" + cellId + ", shipType=" + shipTypeKey + ", shipSize=" + shipSizeKey + ", cog=" + courseOverGroundKey + ", shipCount=" + shipCount + ", totalCount=" + totalCount + ", pd=" + pd);
            } else {
                LOG.debug("totalCount of " + totalCount + " is not enough statistical data for cell " + cellId);
            }
        }

        LOG.debug("pd = " + pd);

        boolean isAbnormalCourseOverGround = pd < PD;
        if (isAbnormalCourseOverGround) {
            LOG.debug("Abnormal event detected.");
        } else {
            LOG.debug("Normal or inconclusive event detected.");
        }

        statisticsService.incAnalysisStatistics(getAnalysisName(), "Analyses performed");

        return isAbnormalCourseOverGround;
    }

    private int calculateShipCount(CourseOverGroundStatisticData courseOverGroundStatisticData, int shipTypeKey, int shipSizeKey, int courseOverGroundKey) {
        if (USE_AGGREGATED_STATS) {
            return courseOverGroundStatisticData.aggregateSumOverKey1(shipSizeKey, courseOverGroundKey, CourseOverGroundStatisticData.STAT_SHIP_COUNT);
        } else {
            Integer value = courseOverGroundStatisticData.getValue(shipTypeKey, shipSizeKey, courseOverGroundKey, CourseOverGroundStatisticData.STAT_SHIP_COUNT);
            return value == null ? 0 : value;
        }
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
        Integer shipDimensionToBow = track.getShipDimensionBow();
        Integer shipDimensionToStern = track.getShipDimensionStern();
        Integer shipDimensionToPort = track.getShipDimensionPort();
        Integer shipDimensionToStarboard = track.getShipDimensionStarboard();
        LocalDateTime positionTimestamp = track.getTimeOfLastPositionReportTyped();
        Position position = track.getPosition();
        Float cog = track.getCourseOverGround();
        Float sog = track.getSpeedOverGround();
        Float hdg = track.getTrueHeading();
        Boolean interpolated = track.getNewestTrackingReport() instanceof InterpolatedTrackingReport;

        TrackingPoint.EventCertainty certainty = TrackingPoint.EventCertainty.UNDEFINED;
        EventCertainty eventCertainty = getBehaviourManager().getEventCertaintyAtCurrentPosition(CourseOverGroundEvent.class, track);
        if (eventCertainty != null) {
            certainty = TrackingPoint.EventCertainty.create(eventCertainty.getCertainty());
        }

        short shipTypeCategory = Categorizer.mapShipTypeToCategory(shipType);
        short shipLengthCategory = Categorizer.mapShipLengthToCategory(shipLength);
        short courseOverGroundCategory = Categorizer.mapCourseOverGroundToCategory(cog);
        short speedOverGroundCategory = Categorizer.mapSpeedOverGroundToCategory(sog);

        String shipTypeAsString = Categorizer.mapShipTypeCategoryToString(shipTypeCategory);
        String shipLengthAsString = Categorizer.mapShipSizeCategoryToString(shipLengthCategory);
        String courseOverGroundAsString = Categorizer.mapCourseOverGroundCategoryToString(courseOverGroundCategory);
        String speedOverGroundAsString = Categorizer.mapSpeedOverGroundCategoryToString(speedOverGroundCategory);

        String title = "Abnormal course over ground";
        String description = String.format("Abnormal course over ground of " + name + " (" + shipTypeAsString + ") on position " + position + " at " + DATE_FORMAT.format(positionTimestamp) + ": cog:%.0f(%s) sog:%.1f(%s) type:%d(%s) size:%d(%s).", cog, courseOverGroundAsString, sog, speedOverGroundAsString, shipType, shipTypeAsString, shipLength, shipLengthAsString);

        LOG.info(description);

        Event event =
            CourseOverGroundEvent()
                .shipType(shipTypeCategory)
                .shipLength(shipLengthCategory)
                .courseOverGround(courseOverGroundCategory)
                .title(title)
                .description(description)
                .startTime(positionTimestamp)
                .behaviour()
                    .isPrimary(true)
                    .vessel()
                        .mmsi(mmsi)
                        .imo(imo)
                        .callsign(callsign)
                        .type(shipType /* shipTypeCategory */)
                        .toBow(shipDimensionToBow)
                        .toStern(shipDimensionToStern)
                        .toPort(shipDimensionToPort)
                        .toStarboard(shipDimensionToStarboard)
                        .name(name)
                    .trackingPoint()
                        .timestamp(positionTimestamp)
                        .positionInterpolated(interpolated)
                        .eventCertainty(certainty)
                        .speedOverGround(sog)
                        .courseOverGround(cog)
                        .trueHeading(hdg)
                        .latitude(position.getLatitude())
                        .longitude(position.getLongitude())
            .getEvent();

        addPreviousTrackingPoints(event, track);

        statisticsService.incAnalysisStatistics(getAnalysisName(), "Events raised");

        return event;
    }
}
