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
import dk.dma.ais.abnormal.event.db.domain.SuddenSpeedChangeEvent;
import dk.dma.ais.abnormal.event.db.domain.builders.SuddenSpeedChangeEventBuilder;
import dk.dma.ais.abnormal.event.db.domain.builders.TrackingPointBuilder;
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

/**
 * This analysis manages events where the a sudden decreasing speed change occurs.
 * A sudden decreasing speed change is defined as a a speed change going from more
 * than 8 knots to less than 1 knot in less than 15 seconds. This analysis is not
 * based on previous observations (feature data).
 */
public class SuddenSpeedChangeAnalysis extends Analysis {
    private static final Logger LOG = LoggerFactory.getLogger(SuddenSpeedChangeAnalysis.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final AppStatisticsService statisticsService;

    private final Map<Integer,TrackingPointData> tracks;
    private final String analysisName;
    private int counter;

    @Inject
    public SuddenSpeedChangeAnalysis(AppStatisticsService statisticsService, TrackingService trackingService, EventRepository eventRepository) {
        super(eventRepository, trackingService);
        this.statisticsService = statisticsService;
        this.tracks = new HashMap<>();
        analysisName = this.getClass().getSimpleName();
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onSpeedOverGroundUpdated(PositionChangedEvent trackEvent) {
        final Float sogAsFloat = (Float) trackEvent.getTrack().getProperty(Track.SPEED_OVER_GROUND);
        if (sogAsFloat == null) {
            return;
        }
        final float sog = sogAsFloat;
        if (sog >= 102.3 /* ~1024 invalid sog */ ) {
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
        TrackingPointData trackingPointData = tracks.get(mmsi);

        if (sog >= 8.0) {
            final long timestamp = (long) track.getProperty(Track.TIMESTAMP_POSITION_UPDATE);
            if (trackingPointData == null) {
                trackingPointData = new TrackingPointData(timestamp, sog, (Float) track.getProperty(Track.COURSE_OVER_GROUND), (Boolean) track.getProperty(Track.POSITION_IS_INTERPOLATED), (Position) track.getProperty(Track.POSITION));
                tracks.put(mmsi, trackingPointData);
            }
        } else if (sog <= 1.0) {
            final long timestamp = (long) track.getProperty(Track.TIMESTAMP_POSITION_UPDATE);
            if (trackingPointData != null) {
                long prevTimestamp = trackingPointData.getTimestamp();

                int deltaSecs = (int) ((timestamp - prevTimestamp) / 1000);
                if (deltaSecs <= 15 ) {
                    raiseAndLowerSuddenSpeedChangeEvent(track);
                }
                tracks.remove(mmsi);
            }
        } else {
            if (trackingPointData != null) {
                tracks.remove(mmsi);
            }
        }
    }

    @Override
    protected Event buildEvent(Track track) {
        Date timestamp = new Date((Long) track.getProperty(Track.TIMESTAMP_POSITION_UPDATE));
        Integer mmsi = track.getMmsi();
        Integer imo = (Integer) track.getProperty(Track.IMO);
        String callsign = (String) track.getProperty(Track.CALLSIGN);
        String name = (String) track.getProperty(Track.SHIP_NAME);
        Position position = (Position) track.getProperty(Track.POSITION);
        Float cog = (Float) track.getProperty(Track.COURSE_OVER_GROUND);
        Float sog = (Float) track.getProperty(Track.SPEED_OVER_GROUND);
        Boolean interpolated = (Boolean) track.getProperty(Track.POSITION_IS_INTERPOLATED);

        TrackingPointData prevTrackingPoint = tracks.get(mmsi);
        Date prevTimestamp = new Date(prevTrackingPoint.getTimestamp());
        float prevSog = prevTrackingPoint.getSog();
        Float prevCog = prevTrackingPoint.getCog();
        Position prevPosition = prevTrackingPoint.getPosition();
        Boolean prevInterpolated = prevTrackingPoint.getPositionInterpolated();

        float deltaSecs = (float) ((timestamp.getTime() - prevTimestamp.getTime()) / 1000.0);

        String desc = String.format("From %.1f kts to %.1f kts in %.1f secs", prevSog, sog, deltaSecs);
        LOG.info(timestamp + ": Detected SuddenSpeedChangeEvent for mmsi " + mmsi + ": "+ desc + "." );

        Event event =
            SuddenSpeedChangeEventBuilder.SuddenSpeedChangeEvent()
                .description(desc)
                .state(Event.State.PAST)
                .startTime(prevTimestamp)
                .endTime(timestamp)
                .behaviour()
                    .vessel()
                        .mmsi(mmsi)
                        .imo(imo)
                        .callsign(callsign)
                        .name(name)
                    .trackingPoint()
                        .timestamp(prevTimestamp)
                        .positionInterpolated(prevInterpolated)
                        .speedOverGround(prevSog)
                        .courseOverGround(prevCog)
                        .latitude(prevPosition.getLatitude())
                        .longitude(prevPosition.getLongitude())
            .getEvent();

        event.getBehaviour().addTrackingPoint(
            TrackingPointBuilder.TrackingPoint()
                .timestamp(timestamp)
                .positionInterpolated(interpolated)
                .speedOverGround(sog)
                .courseOverGround(cog)
                .latitude(position.getLatitude())
                .longitude(position.getLongitude())
            .getTrackingPoint());

        return event;
    }

    private void raiseAndLowerSuddenSpeedChangeEvent(Track track) {
        statisticsService.incAnalysisStatistics(analysisName, "Total speed change evts");
        raiseOrMaintainAbnormalEvent(SuddenSpeedChangeEvent.class, track);
        lowerExistingAbnormalEventIfExists(SuddenSpeedChangeEvent.class, track);
    }

    private final class TrackingPointData {

        private final long timestamp; // Null not allowed
        private final float sog;      // Null not allowed
        private final Float cog;
        private final Boolean positionInterpolated;
        private final Position position;

        private TrackingPointData(Long timestamp, Float sog, Float cog, Boolean positionInterpolated, Position position) {
            this.timestamp = timestamp;
            this.sog = sog;
            this.cog = cog;
            this.positionInterpolated = positionInterpolated;
            this.position = position;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public float getSog() {
            return sog;
        }

        public Float getCog() {
            return cog;
        }

        public Boolean getPositionInterpolated() {
            return positionInterpolated;
        }

        public Position getPosition() {
            return position;
        }
    }

}
