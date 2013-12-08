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
import dk.dma.ais.abnormal.stat.tracker.Track;
import dk.dma.ais.abnormal.stat.tracker.TrackingService;
import dk.dma.ais.abnormal.stat.tracker.events.CellIdChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;

public class ShipTypeAndSizeFeature implements Feature {

    /** The logger */
    private static final transient Logger LOG = LoggerFactory.getLogger(ShipTypeAndSizeFeature.class);

    private final transient AppStatisticsService appStatisticsService;
    private final transient TrackingService trackingService;
    private final transient FeatureDataRepository featureDataRepository;

    private static final String FEATURE_NAME = ShipTypeAndSizeFeature.class.getSimpleName();

    private static final String STATISTICS_KEY_1 = "shipType";
    private static final String STATISTICS_KEY_2 = "shipSize";

    @Inject
    public ShipTypeAndSizeFeature(AppStatisticsService appStatisticsService, TrackingService trackingService, FeatureDataRepository featureDataRepository) {
        this.appStatisticsService = appStatisticsService;
        this.featureDataRepository = featureDataRepository;
        this.trackingService = trackingService;
        this.trackingService.registerSubscriber(this);
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
            LOG.warn("cellId is unexpectedly null (mmsi " + track.getMmsi() + ")");
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

        short shipTypeBucket = mapShipTypeToBucket(shipType);
        short shipSizeBucket = mapShipLengthToBucket(shipLength);

        FeatureData featureDataTmp = featureDataRepository.getFeatureData(FEATURE_NAME, cellId);
        if (! (featureDataTmp instanceof FeatureData2Key)) {
            LOG.debug("No suitable feature data for cell id " + cellId + " found in repo. Creating new.");
            featureDataTmp = new FeatureData2Key(this.getClass(), STATISTICS_KEY_1, STATISTICS_KEY_2);
        }
        FeatureData2Key featureData = (FeatureData2Key) featureDataTmp;

        featureData.incrementStatistic(shipTypeBucket, shipSizeBucket, "shipCount");

        LOG.debug("Storing feature data for cellId " + cellId + ", featureName " + FEATURE_NAME);
        featureDataRepository.putFeatureData(FEATURE_NAME, cellId, featureData);
        LOG.debug("Feature data for cellId " + cellId + ", featureName " + FEATURE_NAME + " stored.");

        // TODO expensive: appStatisticsService.setFeatureStatistics(this.getClass().getSimpleName(), "Cell count", featureDataRepository.getNumberOfCells(FEATURE_NAME));
        appStatisticsService.incFeatureStatistics(this.getClass().getSimpleName(), "Events processed ok");
    }

    private static short mapShipTypeToBucket(Integer shipType) {
        short bucket = 8;
        if (shipType>79 && shipType<90) {
            bucket = 1;
        } else if (shipType>69 && shipType<80) {
            bucket = 2;
        } else if ((shipType>39 && shipType<50) || (shipType>59 && shipType<70)) {
            bucket = 3;
        } else if ((shipType>30 && shipType<36) || (shipType>49 && shipType<56)) {
            bucket = 4;
        } else if (shipType == 30) {
            bucket = 5;
        } else if (shipType == 36 || shipType == 37) {    // TODO Class B
            bucket = 6;
        } else if ((shipType>0 && shipType<30) || (shipType>89 && shipType<100)) {
            bucket = 7;
        } else if (shipType == 0) {
            bucket = 8;
        }

        return bucket;
    }

    private static short mapShipLengthToBucket(Integer shipLength) {
        short bucket;
        if (shipLength >= 0 && shipLength < 1) {
            bucket = 1;
        } else if (shipLength >= 1 && shipLength < 50) {
            bucket = 2;
        } else if (shipLength >= 50 && shipLength < 100) {
            bucket = 3;
        } else if (shipLength >= 100 && shipLength < 200) {
            bucket = 4;
        } else if (shipLength >= 200 && shipLength < 999) {
            bucket = 5;
        } else {
            bucket = 6;
        }

        return bucket;
    }

    @Override
    public void printStatistics(PrintStream stream) {
     //   this.featureData.printStatistics(stream);
    }
}
