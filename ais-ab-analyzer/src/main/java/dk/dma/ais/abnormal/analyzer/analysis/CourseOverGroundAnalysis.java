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
import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import dk.dma.ais.abnormal.stat.db.data.CourseOverGroundFeatureData;
import dk.dma.ais.abnormal.stat.db.data.FeatureData;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.abnormal.tracker.events.CellChangedEvent;
import dk.dma.ais.abnormal.tracker.events.TrackStaleEvent;
import dk.dma.ais.abnormal.util.Categorizer;
import dk.dma.enav.model.geometry.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static dk.dma.ais.abnormal.event.db.domain.builders.CourseOverGroundEventBuilder.CourseOverGroundEvent;

/**
 * This analysis manages events where a vessel has an "abnormal" course over ground
 * relative to the previous observations for vessels in the same grid cell. Statistics
 * for previous observations are stored in the FeatureDataRepository.
 */
public class CourseOverGroundAnalysis extends FeatureDataBasedAnalysis {
    private static final Logger LOG = LoggerFactory.getLogger(CourseOverGroundAnalysis.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final AppStatisticsService statisticsService;

    static final int TOTAL_SHIP_COUNT_THRESHOLD = 1000;

    @Inject
    public CourseOverGroundAnalysis(AppStatisticsService statisticsService, FeatureDataRepository featureDataRepository, TrackingService trackingService, EventRepository eventRepository, BehaviourManager behaviourManager) {
        super(eventRepository, featureDataRepository, trackingService, behaviourManager);
        this.statisticsService = statisticsService;
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onCellIdChanged(CellChangedEvent trackEvent) {
        statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Events received");

        Track track = trackEvent.getTrack();

        Float sog = (Float) track.getProperty(Track.SPEED_OVER_GROUND);
        if (sog == null || sog < 2.0) {
            return;
        }

        Long cellId = (Long) track.getProperty(Track.CELL_ID);
        Integer shipType = (Integer) track.getProperty(Track.SHIP_TYPE);
        Integer shipLength = (Integer) track.getProperty(Track.VESSEL_LENGTH);
        Float courseOverGround = (Float) track.getProperty(Track.COURSE_OVER_GROUND);

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

        if (courseOverGround == null) {
            statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Unknown course over ground");
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
     * If the probability p(d)<0.001 and total count>1000 then abnormal. p(d)=sum(count)/count for all sog_intervals for
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

        FeatureData courseOverGroundFeatureData = getFeatureDataRepository().getFeatureData("CourseOverGroundFeature", cellId);

        if (courseOverGroundFeatureData instanceof CourseOverGroundFeatureData) {
            Integer totalCount  = ((CourseOverGroundFeatureData) courseOverGroundFeatureData).getSumFor(CourseOverGroundFeatureData.STAT_SHIP_COUNT);
            if (totalCount > TOTAL_SHIP_COUNT_THRESHOLD) {
                Integer shipCount = ((CourseOverGroundFeatureData) courseOverGroundFeatureData).getValue(shipTypeKey, shipSizeKey, courseOverGroundKey, CourseOverGroundFeatureData.STAT_SHIP_COUNT);
                if (shipCount == null) {
                    shipCount = 0;
                }
                pd = (float) shipCount / (float) totalCount;
                LOG.debug("cellId=" + cellId + ", shipType=" + shipTypeKey + ", shipSize=" + shipSizeKey + ", cog=" + courseOverGroundKey + ", shipCount=" + shipCount + ", totalCount=" + totalCount + ", pd=" + pd);
            } else {
                LOG.debug("totalCount of " + totalCount + " is not enough statistical data for cell " + cellId);
            }
        }

        LOG.debug("pd = " + pd);

        boolean isAbnormalCourseOverGround = pd < 0.001;
        if (isAbnormalCourseOverGround) {
            LOG.debug("Abnormal event detected.");
        } else {
            LOG.debug("Normal or inconclusive event detected.");
        }

        statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Analyses performed");

        return isAbnormalCourseOverGround;
    }

    @Override
    protected Event buildEvent(Track track) {
        Integer mmsi = track.getMmsi();
        Integer imo = (Integer) track.getProperty(Track.IMO);
        String callsign = (String) track.getProperty(Track.CALLSIGN);
        String name = (String) track.getProperty(Track.SHIP_NAME);
        Integer shipType = (Integer) track.getProperty(Track.SHIP_TYPE);
        Integer shipLength = (Integer) track.getProperty(Track.VESSEL_LENGTH);
        Date positionTimestamp = new Date(track.getPositionReportTimestamp());
        Position position = track.getPositionReportPosition();
        Float cog = (Float) track.getProperty(Track.COURSE_OVER_GROUND);
        Float sog = (Float) track.getProperty(Track.SPEED_OVER_GROUND);
        Boolean interpolated = track.getPositionReportIsInterpolated();

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

        String desc = String.format("cog:%.0f(%s) sog:%.1f(%s) type:%d(%s) size:%d(%s)", cog, courseOverGroundAsString, sog, speedOverGroundAsString, shipType, shipTypeAsString, shipLength, shipLengthAsString);
        LOG.info(positionTimestamp + ": Detected CourseOverGroundEvent for mmsi " + mmsi + ": "+ desc + "." );

        Event event =
                CourseOverGroundEvent()
                        .shipType(shipTypeCategory)
                        .shipLength(shipLengthCategory)
                        .courseOverGround(courseOverGroundCategory)
                        .description(desc)
                        .startTime(positionTimestamp)
                        .behaviour()
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
