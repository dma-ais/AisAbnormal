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
import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import dk.dma.ais.abnormal.stat.db.data.FeatureData;
import dk.dma.ais.abnormal.stat.db.data.FeatureData2Key;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.abnormal.tracker.events.CellIdChangedEvent;
import dk.dma.ais.abnormal.util.Categorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class ShipTypeAndSizeAnalysis implements Analysis {
    private static final Logger LOG = LoggerFactory.getLogger(ShipTypeAndSizeAnalysis.class);

    private final AppStatisticsService statisticsService;
    private final TrackingService trackingService;
    private final FeatureDataRepository featureDataRepository;

    @Inject
    public ShipTypeAndSizeAnalysis(AppStatisticsService statisticsService, FeatureDataRepository featureDataRepository, TrackingService trackingService) {
        this.statisticsService = statisticsService;
        this.featureDataRepository = featureDataRepository;
        this.trackingService = trackingService;

        trackingService.registerSubscriber(this);
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onCellIdChanged(CellIdChangedEvent event) {
        statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Events received");

        //LOG.debug("Received " + event.toString());

        Track track = event.getTrack();

        Integer mmsi = event.getTrack().getMmsi();
        Long cellId = (Long) event.getTrack().getProperty(Track.CELL_ID);
        Integer shipType = (Integer) track.getProperty(Track.SHIP_TYPE);
        Integer shipLength = (Integer) track.getProperty(Track.VESSEL_LENGTH);

        if (cellId == null) {
            LOG.warn("cellId is unexpectedly null (mmsi " + mmsi + ")");
            statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Unknown mmsi");
            return;
        }

        if (shipType == null) {
            LOG.debug("shipType is null - probably no static data received yet (mmsi " + mmsi + ")");
            statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Unknown ship type");
            return;
        }

        if (shipLength == null) {
            LOG.debug("shipLength is null - probably no static data received yet (mmsi " + mmsi + ")");
            statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Unknown ship length");
            return;
        }

        short shipTypeBucket = Categorizer.mapShipTypeToCategory(shipType);
        short shipSizeBucket = Categorizer.mapShipLengthToCategory(shipLength);

        if (isAbnormalCellForShipTypeAndSize(cellId, shipTypeBucket, shipSizeBucket)) {
            LOG.info("ALARM!");
        }

        statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Events processed");
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
    private boolean isAbnormalCellForShipTypeAndSize(Long cellId, short shipTypeBucket, short shipSizeBucket) {
        float pd = 1.0f;

        FeatureData shipSizeAndTypeFeature = featureDataRepository.getFeatureData("ShipTypeAndSizeFeature", cellId);

        if (shipSizeAndTypeFeature instanceof FeatureData2Key) {
            Integer totalCount  = ((FeatureData2Key) shipSizeAndTypeFeature).getSumFor("shipCount");

            if (totalCount > 1000) {
                HashMap<String,Object> statistics = ((FeatureData2Key) shipSizeAndTypeFeature).getStatistics(shipSizeBucket, shipSizeBucket);
                if (statistics != null) {
                    Object shipCountAsObject = statistics.get("shipCount");
                    if (shipCountAsObject instanceof Integer) {
                        Integer shipCount = (Integer) shipCountAsObject;
                        pd = shipCount / totalCount;
                    }
                }
            }

        }

        boolean isAbnormalCellForShipTypeAndSize = pd < 0.001;
        if (isAbnormalCellForShipTypeAndSize) {
            LOG.debug("Abnormal event detected.");
        } else {
            LOG.debug("Normal or indeterminate event detected.");
        }

        return isAbnormalCellForShipTypeAndSize;
    }

}
