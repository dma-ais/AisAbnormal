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
import dk.dma.ais.abnormal.stat.db.data.StatisticData;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.abnormal.tracker.events.CellChangedEvent;
import dk.dma.ais.abnormal.util.Categorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShipTypeAndSizeStatistic implements TrackingEventListener {

    /** The logger */
    private static final transient Logger LOG = LoggerFactory.getLogger(ShipTypeAndSizeStatistic.class);

    private final transient AppStatisticsService appStatisticsService;
    private final transient StatisticDataRepository statisticsRepository;
    private final transient TrackingService trackingService;

    private transient boolean started;

    static final String STATISTIC_NAME = ShipTypeAndSizeStatistic.class.getSimpleName();

    @Inject
    public ShipTypeAndSizeStatistic(AppStatisticsService appStatisticsService, TrackingService trackingService, StatisticDataRepository statisticsRepository) {
        this.appStatisticsService = appStatisticsService;
        this.trackingService = trackingService;
        this.statisticsRepository = statisticsRepository;
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
     * This statistic is only updated once per cell; i.e. when a cell is entered.
     */
    @AllowConcurrentEvents
    @Subscribe
    public void onCellIdChanged(CellChangedEvent event) {
        appStatisticsService.incStatisticStatistics(this.getClass().getSimpleName(), "Events processed");

        LOG.debug("Received " + event.toString());

        Track track = event.getTrack();

        Float sog = track.getSpeedOverGround();
        if (sog != null && sog < 2.0) {
            return; // If track has a sog and it is < 2 - don't run this statistic
        }

        Long cellId = (Long) track.getProperty(Track.CELL_ID);
        Integer shipType = (Integer) track.getProperty(Track.SHIP_TYPE);
        Integer shipLength = (Integer) track.getProperty(Track.VESSEL_LENGTH);

        if (cellId == null) {
            LOG.debug("cellId is null - position is likely not valid (mmsi " + track.getMmsi() + ")");
            appStatisticsService.incStatisticStatistics(this.getClass().getSimpleName(), "Unknown mmsi");
            return;
        }

        if (shipType == null) {
            LOG.debug("shipType is null - probably no static data received yet (mmsi " + track.getMmsi() + ")");
            appStatisticsService.incStatisticStatistics(this.getClass().getSimpleName(), "Unknown ship type");
            return;
        }

        if (shipLength == null) {
            LOG.debug("shipLength is null - probably no static data received yet (mmsi " + track.getMmsi() + ")");
            appStatisticsService.incStatisticStatistics(this.getClass().getSimpleName(), "Unknown ship length");
            return;
        }

        short shipTypeBucket = Categorizer.mapShipTypeToCategory(shipType);
        short shipSizeBucket = Categorizer.mapShipLengthToCategory(shipLength);

        StatisticData statisticsTmp = statisticsRepository.getStatisticData(STATISTIC_NAME, cellId);
        if (! (statisticsTmp instanceof ShipTypeAndSizeStatisticData)) {
            LOG.debug("No suitable statistic data for cell id " + cellId + " found in repo. Creating new.");
            statisticsTmp = ShipTypeAndSizeStatisticData.create();
        }
        ShipTypeAndSizeStatisticData statistics = (ShipTypeAndSizeStatisticData) statisticsTmp;

        statistics.incrementValue(shipTypeBucket-1, shipSizeBucket-1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT);

        LOG.debug("Storing statistic data for cellId " + cellId + ", statisticName " + STATISTIC_NAME);
        statisticsRepository.putStatisticData(STATISTIC_NAME, cellId, statistics);
        LOG.debug("TrackingEventListener data for cellId " + cellId + ", statisticName " + STATISTIC_NAME + " stored.");

        // TODO expensive: appStatisticsService.setStatisticStatistics(this.getClass().getSimpleName(), "Cell count", statisticsRepository.getNumberOfCells(STATISTIC_NAME));
        appStatisticsService.incStatisticStatistics(this.getClass().getSimpleName(), "Events processed ok");
    }

}
