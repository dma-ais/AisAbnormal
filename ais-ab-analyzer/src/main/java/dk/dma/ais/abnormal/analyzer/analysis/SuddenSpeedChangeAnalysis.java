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
import dk.dma.ais.abnormal.event.db.domain.builders.SuddenSpeedChangeEventBuilder;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.abnormal.tracker.events.PositionChangedEvent;
import dk.dma.ais.abnormal.tracker.events.TrackStaleEvent;
import dk.dma.enav.model.geometry.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SuddenSpeedChangeAnalysis implements Analysis {
    private static final Logger LOG = LoggerFactory.getLogger(SuddenSpeedChangeAnalysis.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final TrackingService trackingService;
    private final AppStatisticsService statisticsService;
    private final EventRepository eventRepository;

    private final Map<Integer,TimestampSogPair> tracks;
    private final String analysisName;
    private int counter;

    @Inject
    public SuddenSpeedChangeAnalysis(AppStatisticsService statisticsService, TrackingService trackingService, EventRepository eventRepository) {
        this.statisticsService = statisticsService;
        this.trackingService = trackingService;
        this.eventRepository = eventRepository;
        this.tracks = new HashMap<>();
        analysisName = this.getClass().getSimpleName();
    }

    @Override
    public void start() {
        LOG.info(analysisName + " starts to listen for tracking events.");
        trackingService.registerSubscriber(this);
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onSpeedOverGroundUpdated(PositionChangedEvent trackEvent) {
        final Float sogAsFloat = (Float) trackEvent.getTrack().getProperty(Track.SPEED_OVER_GROUND);
        if (sogAsFloat == null) {
            return;
        }
        final float sog = sogAsFloat;
        if (sog >= 102.3 /* invalid sog */ ) {
            return;
        }

        performAnalysis(trackEvent.getTrack(), sog);

        if (counter++ % 10000 == 0) {
            statisticsService.setAnalysisStatistics(analysisName, "Ships > 8 kts", tracks.size());
        }
        statisticsService.incAnalysisStatistics(analysisName, "Analyses performed");
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onTrackStale(TrackStaleEvent trackEvent) {
        tracks.remove(trackEvent.getTrack().getMmsi());
    }

    private void performAnalysis(Track track, float sog) {
        final int mmsi = track.getMmsi();
        TimestampSogPair timestampSogPair = tracks.get(mmsi);

        if (sog >= 8.0) {
            final long timestamp = (long) track.getProperty(Track.TIMESTAMP_POSITION_UPDATE);
            if (timestampSogPair == null) {
                timestampSogPair = new TimestampSogPair(timestamp, sog);
                tracks.put(mmsi, timestampSogPair);
            }
        } else if (sog <= 1.0) {
            final long timestamp = (long) track.getProperty(Track.TIMESTAMP_POSITION_UPDATE);
            if (timestampSogPair != null) {
                long prevTimestamp = timestampSogPair.getTimestamp();
                float prevSog = timestampSogPair.getSog();

                int deltaSecs = (int) ((timestamp - prevTimestamp) / 1000);
                if (deltaSecs <= 15 ) {
                    raiseAndLowerSuddenSpeedChangeEvent(track);
                }
                tracks.remove(mmsi);
            }
        } else {
            if (timestampSogPair != null) {
                tracks.remove(mmsi);
            }
        }
    }

    private void raiseAndLowerSuddenSpeedChangeEvent(Track track) {
        Date currTimestamp = new Date((Long) track.getProperty(Track.TIMESTAMP_POSITION_UPDATE));
        Integer mmsi = track.getMmsi();
        Integer imo = (Integer) track.getProperty(Track.IMO);
        String callsign = (String) track.getProperty(Track.CALLSIGN);
        String name = (String) track.getProperty(Track.SHIP_NAME);
        Position position = (Position) track.getProperty(Track.POSITION);
        Float cog = (Float) track.getProperty(Track.COURSE_OVER_GROUND);
        Float sog = (Float) track.getProperty(Track.SPEED_OVER_GROUND);
        Boolean interpolated = (Boolean) track.getProperty(Track.POSITION_IS_INTERPOLATED);

        Date prevTimestamp = new Date(tracks.get(mmsi).getTimestamp());
        float prevSog = tracks.get(mmsi).getSog();
        float deltaSecs = (float) ((currTimestamp.getTime() - prevTimestamp.getTime()) / 1000.0);

        String desc = String.format("Went from %.1f kts to %.1f kts in %.1f secs", prevSog, sog, deltaSecs);
        LOG.info("Detected sudden speed change for mmsi " + mmsi + ": "+ desc + "." );

        Event event =
                SuddenSpeedChangeEventBuilder.SuddenSpeedChangeEvent()
                    .description(desc)
                    .state(Event.State.PAST)
                    .startTime(prevTimestamp)
                    .endTime(currTimestamp)
                    .behaviour()
                        .vessel()
                        .mmsi(mmsi)
                        .imo(imo)
                        .callsign(callsign)
                        .name(name)
                    .trackingPoint()
                        .timestamp(currTimestamp)
                        .positionInterpolated(interpolated)
                        .speedOverGround(sog)
                        .courseOverGround(cog)
                        .latitude(position.getLatitude())
                        .longitude(position.getLongitude())
                .getEvent();

        statisticsService.incAnalysisStatistics(analysisName, "Total speed change evts");

        eventRepository.save(event);
    }

    private final class TimestampSogPair {
        private final long timestamp;
        private final float sog;

        private TimestampSogPair(long timestamp, float sog) {
            this.timestamp = timestamp;
            this.sog = sog;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public float getSog() {
            return sog;
        }
    }

}
