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

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import dk.dma.ais.abnormal.stat.AppStatisticsService;
import dk.dma.ais.abnormal.stat.tracker.Track;
import dk.dma.ais.abnormal.stat.tracker.TrackingService;
import dk.dma.ais.abnormal.stat.tracker.events.CellIdChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShipTypeAndSizeFeature implements Feature {

    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger(ShipTypeAndSizeFeature.class);

    private AppStatisticsService appStatisticsService;
    private TrackingService trackingService;

    FeatureStatistics featureStatistics = new FeatureStatistics();

    @Inject
    public ShipTypeAndSizeFeature(AppStatisticsService appStatisticsService, TrackingService trackingService) {
        this.appStatisticsService = appStatisticsService;
        this.trackingService = trackingService;
        this.trackingService.registerSubscriber(this);
    }

    /*
     * This feature is only updated once per cell; i.e. when a cell is entered.
     */
    @Subscribe
    public void cellIdChangedEvent(CellIdChangedEvent event) {
        appStatisticsService.incFeatureStatistics(this.getClass().getSimpleName(), "Events processed");

        LOG.debug("Received " + event.toString());

        Track track = event.getTrack();

        Integer cellId = (Integer) track.getProperty(Track.CELL_ID);
        Integer shipType = (Integer) track.getProperty(Track.SHIP_TYPE);
        Integer shipLength = (Integer) track.getProperty(Track.VESSEL_LENGTH);

        if (cellId == null) {
            LOG.error("cellId is unexpectedly null (mmsi " + track.getMmsi() + ")");
            return;
        }

        if (shipType == null) {
            LOG.debug("shipType is null - probably no static data received yet (mmsi " + track.getMmsi() + ")");
            return;
        }

        if (shipLength == null) {
            LOG.debug("shipLength is null - probably no static data received yet (mmsi " + track.getMmsi() + ")");
            return;
        }

        Integer shipTypeBucket = mapShipTypeToBucket(shipType);
        Integer shipSizeBucket = mapShipLengthToBucket(shipLength);

        featureStatistics.incrementStatistic(cellId, shipTypeBucket, shipSizeBucket, "shipCount");

        appStatisticsService.setFeatureStatistics(this.getClass().getSimpleName(), "Cell count", Long.valueOf(featureStatistics.getNumberOfLevel1Entries()));
        appStatisticsService.incFeatureStatistics(this.getClass().getSimpleName(), "Events processed ok");
    }

    private static Integer mapShipTypeToBucket(Integer shipType) {
        Integer bucket = 8;
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

    private static Integer mapShipLengthToBucket(Integer shipLength) {
        Integer bucket;
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
}
