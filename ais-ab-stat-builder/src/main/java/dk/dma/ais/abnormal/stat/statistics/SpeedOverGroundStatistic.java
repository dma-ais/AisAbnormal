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

package dk.dma.ais.abnormal.stat.statistics;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import dk.dma.ais.abnormal.stat.AppStatisticsService;
import dk.dma.ais.abnormal.stat.db.StatisticDataRepository;
import dk.dma.ais.abnormal.stat.db.data.ShipTypeAndSizeStatisticData;
import dk.dma.ais.abnormal.stat.db.data.SpeedOverGroundStatisticData;
import dk.dma.ais.abnormal.stat.db.data.StatisticData;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.abnormal.tracker.events.CellChangedEvent;
import dk.dma.ais.abnormal.util.Categorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class SpeedOverGroundStatistic implements TrackingEventListener {

    /**
     * The logger
     */
    private static final transient Logger LOG = LoggerFactory.getLogger(SpeedOverGroundStatistic.class);

    private final transient AppStatisticsService appStatisticsService;
    private final transient StatisticDataRepository statisticsRepository;
    private final transient TrackingService trackingService;

    private final transient AtomicBoolean started = new AtomicBoolean(false);

    static final String STATISTIC_NAME = SpeedOverGroundStatistic.class.getSimpleName();

    @Inject
    public SpeedOverGroundStatistic(AppStatisticsService appStatisticsService, TrackingService trackingService, StatisticDataRepository statisticsRepository) {
        this.appStatisticsService = appStatisticsService;
        this.trackingService = trackingService;
        this.statisticsRepository = statisticsRepository;
    }

    /**
     * Start listening to tracking events.
     */
    public void start() {
        if (!started.get()) {
            trackingService.registerSubscriber(this);
            started.getAndSet(true);
        }
    }

    /*
     * This statistic is only updated once per cell; i.e. when a cell is entered.
     */
    @AllowConcurrentEvents
    @Subscribe
    public void onCellIdChanged(CellChangedEvent event) {
        appStatisticsService.incStatisticStatistics(STATISTIC_NAME, "Events processed");

        Track track = event.getTrack();
        Float sog = track.getSpeedOverGround();

        if (sog != null) {
            Long cellId = (Long) track.getProperty(Track.CELL_ID);
            Integer shipType = (Integer) track.getProperty(Track.SHIP_TYPE);
            Integer shipLength = (Integer) track.getProperty(Track.VESSEL_LENGTH);

            if (isInputValid(cellId, shipType, shipLength, sog)) {
                short shipTypeBucket = Categorizer.mapShipTypeToCategory(shipType);
                short shipSizeBucket = Categorizer.mapShipLengthToCategory(shipLength);
                short sogBucket = Categorizer.mapSpeedOverGroundToCategory(sog);

                incrementStatisticStatistics(cellId, shipTypeBucket, shipSizeBucket, sogBucket);

                appStatisticsService.incStatisticStatistics(this.getClass().getSimpleName(), "Events processed ok");
            }
        }
    }

    private boolean isInputValid(Long cellId, Integer shipType, Integer shipLength, Float sog) {
        boolean valid = true;

        if (cellId == null) {
            appStatisticsService.incStatisticStatistics(STATISTIC_NAME, "Unknown mmsi");
            valid = false;
        }

        if (shipType == null) {
            appStatisticsService.incStatisticStatistics(STATISTIC_NAME, "Unknown ship type");
            valid = false;
        }

        if (shipLength == null) {
            appStatisticsService.incStatisticStatistics(STATISTIC_NAME, "Unknown ship length");
            valid = false;
        }

        if (sog == null) {
            appStatisticsService.incStatisticStatistics(STATISTIC_NAME, "Unknown speed over ground");
            valid = false;
        }

        return valid;
    }

    private void incrementStatisticStatistics(long cellId, int shipTypeBucket, int shipSizeBucket, int sogBucket) {
        StatisticData statisticsTmp = statisticsRepository.getStatisticData(STATISTIC_NAME, cellId);
        if (!(statisticsTmp instanceof SpeedOverGroundStatisticData)) {
            LOG.debug("No suitable statistic data for cell id " + cellId + " found in repo. Creating new.");
            statisticsTmp = SpeedOverGroundStatisticData.create();
        }
        SpeedOverGroundStatisticData statistics = (SpeedOverGroundStatisticData) statisticsTmp;

        // Increment ship count
        statistics.incrementValue(shipTypeBucket-1, shipSizeBucket-1, sogBucket-1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT);

        LOG.debug("Storing statistic data for cellId " + cellId + ", statisticName " + STATISTIC_NAME);
        statisticsRepository.putStatisticData(STATISTIC_NAME, cellId, statistics);
        LOG.debug("TrackingEventListener data for cellId " + cellId + ", statisticName " + STATISTIC_NAME + " stored.");
    }

}
