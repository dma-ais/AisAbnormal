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

package dk.dma.ais.abnormal.stat.features;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import dk.dma.ais.abnormal.stat.AppStatisticsService;
import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import dk.dma.ais.abnormal.stat.db.data.FeatureData;
import dk.dma.ais.abnormal.stat.db.data.FeatureData2Key;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.abnormal.tracker.events.CellIdChangedEvent;
import dk.dma.ais.abnormal.util.Categorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShipTypeAndSizeFeature implements Feature {

    /** The logger */
    private static final transient Logger LOG = LoggerFactory.getLogger(ShipTypeAndSizeFeature.class);

    private final transient AppStatisticsService appStatisticsService;
    private final transient FeatureDataRepository featureDataRepository;
    private final transient TrackingService trackingService;

    static final String STATISTICS_KEY_1 = "shipType";
    static final String STATISTICS_KEY_2 = "shipSize";
    static final String STATISTICS_NAME = "shipCount";

    private transient boolean started;

    static final String FEATURE_NAME = ShipTypeAndSizeFeature.class.getSimpleName();

    @Inject
    public ShipTypeAndSizeFeature(AppStatisticsService appStatisticsService, TrackingService trackingService, FeatureDataRepository featureDataRepository) {
        this.appStatisticsService = appStatisticsService;
        this.trackingService = trackingService;
        this.featureDataRepository = featureDataRepository;
    }

    /**
     * Start listening to tracking events.
     */
    public void start() {
        if (! started) {
            trackingService.registerSubscriber(this);
            started = true;
        }
    }

    /*
     * This feature is only updated once per cell; i.e. when a cell is entered.
     */
    @AllowConcurrentEvents
    @Subscribe
    public void onCellIdChanged(CellIdChangedEvent event) {
        appStatisticsService.incFeatureStatistics(this.getClass().getSimpleName(), "Events processed");

        LOG.debug("Received " + event.toString());

        Track track = event.getTrack();

        Long cellId = (Long) track.getProperty(Track.CELL_ID);
        Integer shipType = (Integer) track.getProperty(Track.SHIP_TYPE);
        Integer shipLength = (Integer) track.getProperty(Track.VESSEL_LENGTH);

        if (cellId == null) {
            LOG.debug("cellId is null - position is likely not valid (mmsi " + track.getMmsi() + ")");
            appStatisticsService.incFeatureStatistics(this.getClass().getSimpleName(), "Unknown mmsi");
            return;
        }

        if (shipType == null) {
            LOG.debug("shipType is null - probably no static data received yet (mmsi " + track.getMmsi() + ")");
            appStatisticsService.incFeatureStatistics(this.getClass().getSimpleName(), "Unknown ship type");
            return;
        }

        if (shipLength == null) {
            LOG.debug("shipLength is null - probably no static data received yet (mmsi " + track.getMmsi() + ")");
            appStatisticsService.incFeatureStatistics(this.getClass().getSimpleName(), "Unknown ship length");
            return;
        }

        short shipTypeBucket = Categorizer.mapShipTypeToCategory(shipType);
        short shipSizeBucket = Categorizer.mapShipLengthToCategory(shipLength);

        FeatureData featureDataTmp = featureDataRepository.getFeatureData(FEATURE_NAME, cellId);
        if (! (featureDataTmp instanceof FeatureData2Key)) {
            LOG.debug("No suitable feature data for cell id " + cellId + " found in repo. Creating new.");
            featureDataTmp = new FeatureData2Key(this.getClass(), STATISTICS_KEY_1, STATISTICS_KEY_2);
        }
        FeatureData2Key featureData = (FeatureData2Key) featureDataTmp;

        featureData.incrementStatistic(shipTypeBucket, shipSizeBucket, STATISTICS_NAME);

        LOG.debug("Storing feature data for cellId " + cellId + ", featureName " + FEATURE_NAME);
        featureDataRepository.putFeatureData(FEATURE_NAME, cellId, featureData);
        LOG.debug("Feature data for cellId " + cellId + ", featureName " + FEATURE_NAME + " stored.");

        // TODO expensive: appStatisticsService.setFeatureStatistics(this.getClass().getSimpleName(), "Cell count", featureDataRepository.getNumberOfCells(FEATURE_NAME));
        appStatisticsService.incFeatureStatistics(this.getClass().getSimpleName(), "Events processed ok");
    }

}
