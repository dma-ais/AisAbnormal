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
import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import dk.dma.ais.abnormal.stat.db.data.FeatureData;
import dk.dma.ais.abnormal.stat.db.data.FeatureData2Key;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.abnormal.tracker.events.CellIdChangedEvent;
import dk.dma.ais.abnormal.util.Categorizer;
import dk.dma.enav.model.geometry.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;

import static dk.dma.ais.abnormal.event.db.domain.builders.AbnormalShipSizeOrTypeEventBuilder.AbnormalShipSizeOrTypeEvent;

public class ShipTypeAndSizeAnalysis extends StatisticalAnalysis {
    private static final Logger LOG = LoggerFactory.getLogger(ShipTypeAndSizeAnalysis.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final AppStatisticsService statisticsService;
    private final TrackingService trackingService;
    private static final int TOTAL_COUNT_THRESHOLD = 1; // TODO 1000
    private final EventRepository eventRepository;

    @Inject
    public ShipTypeAndSizeAnalysis(AppStatisticsService statisticsService, FeatureDataRepository featureDataRepository, TrackingService trackingService, EventRepository eventRepository) {
        super(featureDataRepository);

        this.statisticsService = statisticsService;
        this.trackingService = trackingService;
        this.eventRepository = eventRepository;

        trackingService.registerSubscriber(this);
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onCellIdChanged(CellIdChangedEvent trackEvent) {
        statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Events received");

        //LOG.debug("Received " + event.toString());

        Track track = trackEvent.getTrack();

        Date timestamp = new Date((Long) track.getProperty(Track.TIMESTAMP));
        Integer mmsi = trackEvent.getTrack().getMmsi();
        Integer imo = (Integer) trackEvent.getTrack().getProperty(Track.IMO);
        String callsign = (String) trackEvent.getTrack().getProperty(Track.CALLSIGN);
        Position position = (Position) trackEvent.getTrack().getProperty(Track.POSITION);
        Long cellId = (Long) trackEvent.getTrack().getProperty(Track.CELL_ID);
        Integer shipType = (Integer) track.getProperty(Track.SHIP_TYPE);
        Integer shipLength = (Integer) track.getProperty(Track.VESSEL_LENGTH);
        String shipName = (String) track.getProperty(Track.SHIP_NAME);

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
            StringBuffer description = new StringBuffer(64);
            description.append("Vessel of type ");
            description.append(shipType);
            description.append(" and length ");
            description.append(shipLength);
            description.append(" is abnormal for cell ");
            description.append(cellId);

            Event event =
            AbnormalShipSizeOrTypeEvent()
                            .shipType(shipTypeBucket)
                            .shipLength(shipLengthBucket)
                            .description(description.toString())
                            .startTime(timestamp)
                            .behaviour()
                                .vessel()
                                    .mmsi(mmsi)
                                    .imo(imo)
                                    .callsign(callsign)
                                    .name(shipName)
                                .position()
                                    .timestamp(timestamp)
                                    .latitude(position.getLatitude())
                                    .longitude(position.getLongitude())
                            .buildEvent();

            LOG.debug("Abnormal event: " + event);

            raiseOrMaintainEvent(event);
        }

        statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Events processed");
    }

    private void raiseOrMaintainEvent(Event event) {
        Event ongoingEvent = eventRepository.findOngoingEventByVessel(event.getBehaviour().getVessel(), event.getClass());

        if (ongoingEvent != null) {
            ongoingEvent.getBehaviour().addPositions(event.getBehaviour().getPositions());
            event = ongoingEvent;
        }

        eventRepository.save(event);
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
    boolean isAbnormalCellForShipTypeAndSize(Long cellId, short shipTypeBucket, short shipSizeBucket) {
        float pd = 1.0f;

        FeatureData shipSizeAndTypeFeature = getFeatureDataRepository().getFeatureData("ShipTypeAndSizeFeature", cellId);

        if (shipSizeAndTypeFeature instanceof FeatureData2Key) {
            Integer totalCount  = ((FeatureData2Key) shipSizeAndTypeFeature).getSumFor("shipCount");
            if (totalCount > TOTAL_COUNT_THRESHOLD) {
                Integer shipCount = 0;
                HashMap<String,Object> statistics = ((FeatureData2Key) shipSizeAndTypeFeature).getStatistics(shipTypeBucket, shipSizeBucket);
                if (statistics != null) {
                    Object shipCountAsObject = statistics.get("shipCount");
                    if (shipCountAsObject instanceof Integer) {
                        shipCount = (Integer) shipCountAsObject;
                    }
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
