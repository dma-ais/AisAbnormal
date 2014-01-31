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
import dk.dma.ais.abnormal.event.db.domain.SpeedOverGroundEvent;
import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import dk.dma.ais.abnormal.stat.db.data.FeatureData;
import dk.dma.ais.abnormal.stat.db.data.SpeedOverGroundFeatureData;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.abnormal.tracker.events.CellChangedEvent;
import dk.dma.ais.abnormal.tracker.events.TrackStaleEvent;
import dk.dma.ais.abnormal.util.Categorizer;
import dk.dma.enav.model.geometry.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static dk.dma.ais.abnormal.event.db.domain.builders.SpeedOverGroundEventBuilder.SpeedOverGroundEvent;

/**
 * This analysis manages events where a vessel has an "abnormal" speed over ground
 * relative to the previous observations for vessels in the same grid cell. Statistics
 * for previous observations are stored in the FeatureDataRepository.
 */
public class SpeedOverGroundAnalysis extends FeatureDataBasedAnalysis {
    private static final Logger LOG = LoggerFactory.getLogger(SpeedOverGroundAnalysis.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final AppStatisticsService statisticsService;

    private static final int TOTAL_COUNT_THRESHOLD = 1000;

    @Inject
    public SpeedOverGroundAnalysis(AppStatisticsService statisticsService, FeatureDataRepository featureDataRepository, TrackingService trackingService, EventRepository eventRepository) {
        super(eventRepository, featureDataRepository, trackingService);
        this.statisticsService = statisticsService;
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onCellIdChanged(CellChangedEvent trackEvent) {
        statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Events received");

        Track track = trackEvent.getTrack();
        Long cellId = (Long) track.getProperty(Track.CELL_ID);
        Integer shipType = (Integer) track.getProperty(Track.SHIP_TYPE);
        Integer shipLength = (Integer) track.getProperty(Track.VESSEL_LENGTH);
        Float speedOverGround = (Float) track.getProperty(Track.SPEED_OVER_GROUND);

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

        short shipTypeBucket = Categorizer.mapShipTypeToCategory(shipType);
        short shipLengthBucket = Categorizer.mapShipLengthToCategory(shipLength);
        short speedOverGroundBucket = Categorizer.mapSpeedOverGroundToCategory(speedOverGround);

        if (isAbnormalSpeedOverGround(cellId, shipTypeBucket, shipLengthBucket, speedOverGroundBucket)) {
            raiseOrMaintainAbnormalEvent(SpeedOverGroundEvent.class, track);
        } else {
            lowerExistingAbnormalEventIfExists(SpeedOverGroundEvent.class, track);
        }

        statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Events processed");
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onTrackStale(TrackStaleEvent trackEvent) {
        lowerExistingAbnormalEventIfExists(SpeedOverGroundEvent.class, trackEvent.getTrack());
    }

    /**
     * If the probability p(d)<0.001 and total count>1000 then abnormal. p(d)=sum(count)/count for all sog_intervals for
     * that shiptype and size.
     *
     * @param cellId
     * @param shipTypeBucket
     * @param shipSizeBucket
     * @return true if the presence of size/type with this sog in this cell is abnormal. False otherwise.
     */
    boolean isAbnormalSpeedOverGround(Long cellId, int shipTypeBucket, int shipSizeBucket, int speedOverGroundBucket) {
        float pd = 1.0f;

        FeatureData speedOverGroundFeatureData = getFeatureDataRepository().getFeatureData("SpeedOverGroundFeature", cellId);

        if (speedOverGroundFeatureData instanceof SpeedOverGroundFeatureData) {
            Integer totalCount  = ((SpeedOverGroundFeatureData) speedOverGroundFeatureData).getSumFor(SpeedOverGroundFeatureData.STAT_SHIP_COUNT);
            if (totalCount > TOTAL_COUNT_THRESHOLD) {
                Integer shipCount = ((SpeedOverGroundFeatureData) speedOverGroundFeatureData).getValue(shipTypeBucket, shipSizeBucket, speedOverGroundBucket, SpeedOverGroundFeatureData.STAT_SHIP_COUNT);
                if (shipCount == null) {
                    shipCount = 0;
                }
                pd = (float) shipCount / (float) totalCount;
                LOG.debug("cellId=" + cellId + ", shipType=" + shipTypeBucket + ", shipSize=" + shipSizeBucket + ", sog=" + speedOverGroundBucket + ", shipCount=" + shipCount + ", totalCount=" + totalCount + ", pd=" + pd);
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
    protected Event buildEvent(Track track) {
        Integer mmsi = track.getMmsi();
        Integer imo = (Integer) track.getProperty(Track.IMO);
        String callsign = (String) track.getProperty(Track.CALLSIGN);
        String name = (String) track.getProperty(Track.SHIP_NAME);
        Integer shipType = (Integer) track.getProperty(Track.SHIP_TYPE);
        Integer shipLength = (Integer) track.getProperty(Track.VESSEL_LENGTH);
        Date positionTimestamp = new Date((Long) track.getProperty(Track.TIMESTAMP_POSITION_UPDATE));
        Position position = (Position) track.getProperty(Track.POSITION);
        Float cog = (Float) track.getProperty(Track.COURSE_OVER_GROUND);
        Float sog = (Float) track.getProperty(Track.SPEED_OVER_GROUND);
        Boolean interpolated = (Boolean) track.getProperty(Track.POSITION_IS_INTERPOLATED);

        short shipTypeBucket = Categorizer.mapShipTypeToCategory(shipType);
        short shipLengthBucket = Categorizer.mapShipLengthToCategory(shipLength);
        short speedOverGroundBucket = Categorizer.mapSpeedOverGroundToCategory(sog);

        String desc = String.format("sog:%.0f(%d) type:%d(%d) size:%d(%d)", sog, speedOverGroundBucket+1, shipType, shipTypeBucket+1, shipLength, shipLengthBucket+1);
        LOG.info(positionTimestamp + ": Detected SpeedOverGroundEvent for mmsi " + mmsi + ": "+ desc + "." );

        Event event =
                SpeedOverGroundEvent()
                        .shipType(shipTypeBucket)
                        .shipLength(shipLengthBucket)
                        .speedOverGround(speedOverGroundBucket)
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
                                .speedOverGround(sog)
                                .courseOverGround(cog)
                                .latitude(position.getLatitude())
                                .longitude(position.getLongitude())
                .getEvent();

        return event;
    }
}
