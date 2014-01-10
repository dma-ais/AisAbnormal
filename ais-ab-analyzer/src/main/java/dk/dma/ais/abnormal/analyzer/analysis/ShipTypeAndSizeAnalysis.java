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
import dk.dma.ais.abnormal.event.db.domain.AbnormalShipSizeOrTypeEvent;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.VesselId;
import dk.dma.ais.abnormal.event.db.domain.builders.TrackingPointBuilder;
import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import dk.dma.ais.abnormal.stat.db.data.FeatureData;
import dk.dma.ais.abnormal.stat.db.data.ShipTypeAndSizeData;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.abnormal.tracker.events.CellIdChangedEvent;
import dk.dma.ais.abnormal.util.Categorizer;
import dk.dma.enav.model.geometry.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static dk.dma.ais.abnormal.event.db.domain.builders.AbnormalShipSizeOrTypeEventBuilder.AbnormalShipSizeOrTypeEvent;

public class ShipTypeAndSizeAnalysis extends StatisticalAnalysis {
    private static final Logger LOG = LoggerFactory.getLogger(ShipTypeAndSizeAnalysis.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final AppStatisticsService statisticsService;
    private final EventRepository eventRepository;

    private static final int TOTAL_COUNT_THRESHOLD = 1; // TODO 1000

    @Inject
    public ShipTypeAndSizeAnalysis(AppStatisticsService statisticsService, FeatureDataRepository featureDataRepository, TrackingService trackingService, EventRepository eventRepository) {
        super(featureDataRepository);

        this.statisticsService = statisticsService;
        this.eventRepository = eventRepository;

        trackingService.registerSubscriber(this);
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onCellIdChanged(CellIdChangedEvent trackEvent) {
        statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Events received");

        Track track = trackEvent.getTrack();
        Long cellId = (Long) track.getProperty(Track.CELL_ID);
        Integer shipType = (Integer) track.getProperty(Track.SHIP_TYPE);
        Integer shipLength = (Integer) track.getProperty(Track.VESSEL_LENGTH);

        if (cellId == null) {
            // LOG.warn("cellId is unexpectedly null (mmsi " + mmsi + ")");
            statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Unknown mmsi");
            return;
        }

        if (shipType == null) {
            // LOG.debug("shipType is null - probably no static data received yet (mmsi " + mmsi + ")");
            statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Unknown ship type");
            return;
        }

        if (shipLength == null) {
            // LOG.debug("shipLength is null - probably no static data received yet (mmsi " + mmsi + ")");
            statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Unknown ship length");
            return;
        }

        short shipTypeBucket = Categorizer.mapShipTypeToCategory(shipType);
        short shipLengthBucket = Categorizer.mapShipLengthToCategory(shipLength);

        if (isAbnormalCellForShipTypeAndSize(cellId, shipTypeBucket, shipLengthBucket)) {
            raiseOrMaintainAbnormalEvent(trackEvent);
        } else {
            lowerExistingAbnormalEventIfExists(trackEvent);
        }

        statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Events processed");
    }

    private void lowerExistingAbnormalEventIfExists(CellIdChangedEvent trackEvent) {
        Track track = trackEvent.getTrack();

        Integer mmsi = track.getMmsi();
        Integer imo = (Integer) track.getProperty(Track.IMO);
        String callsign = (String) track.getProperty(Track.CALLSIGN);
        String shipName = (String) track.getProperty(Track.SHIP_NAME);

        VesselId vesselId = new VesselId();
        vesselId.setImo(imo);
        vesselId.setMmsi(mmsi);
        vesselId.setCallsign(callsign);
        vesselId.setName(shipName);

        Event ongoingEvent = eventRepository.findOngoingEventByVessel(vesselId, AbnormalShipSizeOrTypeEvent.class);
        if (ongoingEvent != null) {
            Date timestamp = new Date((Long) track.getProperty(Track.TIMESTAMP));
            ongoingEvent.setState(Event.State.PAST);
            ongoingEvent.setEndTime(timestamp);
            eventRepository.save(ongoingEvent);
        }
    }

    private void raiseOrMaintainAbnormalEvent(CellIdChangedEvent trackEvent) {
        Track track = trackEvent.getTrack();

        Date timestamp = new Date((Long) track.getProperty(Track.TIMESTAMP));
        Integer mmsi = track.getMmsi();
        Integer imo = (Integer) track.getProperty(Track.IMO);
        String callsign = (String) track.getProperty(Track.CALLSIGN);
        String shipName = (String) track.getProperty(Track.SHIP_NAME);
        Position position = (Position) track.getProperty(Track.POSITION);
        Float cog = (Float) track.getProperty(Track.COURSE_OVER_GROUND);
        Float sog = (Float) track.getProperty(Track.SPEED_OVER_GROUND);
        Boolean interpolated = (Boolean) track.getProperty(Track.POSITION_IS_INTERPOLATED);

        VesselId vesselId = new VesselId();
        vesselId.setImo(imo);
        vesselId.setMmsi(mmsi);
        vesselId.setCallsign(callsign);
        vesselId.setName(shipName);

        Event ongoingEvent = eventRepository.findOngoingEventByVessel(vesselId, AbnormalShipSizeOrTypeEvent.class);

        if (ongoingEvent != null) {
            ongoingEvent.getBehaviour().addTrackingPoint(
                    TrackingPointBuilder.TrackingPoint()
                            .timestamp(timestamp)
                            .positionInterpolated(interpolated)
                            .speedOverGround(sog)
                            .courseOverGround(cog)
                            .latitude(position.getLatitude())
                            .longitude(position.getLongitude())
                    .getTrackingPoint()
            );

            eventRepository.save(ongoingEvent);
        } else {
            Integer shipType = (Integer) track.getProperty(Track.SHIP_TYPE);
            Integer shipLength = (Integer) track.getProperty(Track.VESSEL_LENGTH);

            short shipTypeBucket = Categorizer.mapShipTypeToCategory(shipType);
            short shipLengthBucket = Categorizer.mapShipLengthToCategory(shipLength);

            Event event =
                    AbnormalShipSizeOrTypeEvent()
                            .shipType(shipTypeBucket)
                            .shipLength(shipLengthBucket)
                            .description("It is abnormal to see this vessel in this area.")
                            .startTime(timestamp)
                            .behaviour()
                                .vessel()
                                    .mmsi(mmsi)
                                    .imo(imo)
                                    .callsign(callsign)
                                    .name(shipName)
                                .trackingPoint()
                                    .timestamp(timestamp)
                                    .speedOverGround(sog)
                                    .courseOverGround(cog)
                                    .latitude(position.getLatitude())
                                    .longitude(position.getLongitude())
                            .getEvent();

            eventRepository.save(event);
        }

    }

    /**
     * If the probability p(d)<0.001 and total count>1000 then abnormal. p(d)=sum(count)/count for all sog_intervals for
     * that shiptype and size.
     *
     * @param cellId
     * @param shipTypeBucket
     * @param shipSizeBucket
     * @return true if the presence of size/type in this cell is abnormal. False otherwise.
     */
    boolean isAbnormalCellForShipTypeAndSize(Long cellId, int shipTypeBucket, int shipSizeBucket) {
        float pd = 1.0f;

        FeatureData shipSizeAndTypeData = getFeatureDataRepository().getFeatureData("ShipTypeAndSizeFeature", cellId);

        if (shipSizeAndTypeData instanceof ShipTypeAndSizeData) {
            Integer totalCount  = ((ShipTypeAndSizeData) shipSizeAndTypeData).getSumFor("shipCount");
            if (totalCount > TOTAL_COUNT_THRESHOLD) {
                Integer shipCount = ((ShipTypeAndSizeData) shipSizeAndTypeData).getStatistic(shipTypeBucket, shipSizeBucket, ShipTypeAndSizeData.STAT_SHIP_COUNT);
                if (shipCount == null) {
                    shipCount = 0;
                }
                pd = (float) shipCount / (float) totalCount;
                LOG.debug("cellId=" + cellId + ", shipType=" + shipTypeBucket + ", shipSize=" + shipSizeBucket + ", shipCount=" + shipCount + ", totalCount=" + totalCount + ", pd=" + pd);
            } else {
                LOG.debug("totalCount of " + totalCount + " is not enough statistical data for cell " + cellId);
            }
        }

        LOG.debug("pd = " + pd);

        boolean isAbnormalCellForShipTypeAndSize = pd < 0.001;
        if (isAbnormalCellForShipTypeAndSize) {
            LOG.debug("Abnormal event detected.");
        } else {
            LOG.debug("Normal or inconclusive event detected.");
        }

        statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Analyses performed");

        return isAbnormalCellForShipTypeAndSize;
    }

}
