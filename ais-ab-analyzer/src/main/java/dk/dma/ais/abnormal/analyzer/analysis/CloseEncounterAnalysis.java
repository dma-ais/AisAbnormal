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

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import dk.dma.ais.abnormal.analyzer.AppStatisticsService;
import dk.dma.ais.abnormal.analyzer.helpers.CoordinateTransformer;
import dk.dma.ais.abnormal.analyzer.helpers.Point;
import dk.dma.ais.abnormal.analyzer.helpers.SafetyZone;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.domain.CloseEncounterEvent;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.TrackingPoint;
import dk.dma.ais.abnormal.event.db.domain.builders.CloseEncounterEventBuilder;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingReport;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.abnormal.tracker.events.TimeEvent;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * This analysis manages events where two vessels have a close encounter and therefore
 * are in risk of collision.
 *
 * The analysis works by calculating an ellipse around each ship. The ellipse's size and orientation depends on the
 * vessels loa, beam, speed, and course. The ellipse is translated forward from the vessel's center so that it covers
 * a larger area in front of the vessel. If two ellipses intersect there is a risk of collision and this is registered
 * as an abnormal event.
 *
 * This analysis is rather extensive, and we can therefore now allow to block the EventBus
 * for the duration of a complete analysis. Instead the worked is spawned to a separate worker
 * thread.
 */
@NotThreadSafe
public class CloseEncounterAnalysis extends Analysis {
    private static final Logger LOG = LoggerFactory.getLogger(CloseEncounterAnalysis.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final AppStatisticsService statisticsService;
    private final String analysisName;

    /**
     * Minimum no. of msecs between runs of this analysis.
     */
    private final static int ANALYS_PERIOD_MILLIS = 5 * 60 * 1000;

    /**
     * The time when the analysis should next be run.
     */
    private Date nextRunTime;

    /**
     * Executor to perform the actual work.
     */
    private Executor executor = MoreExecutors.sameThreadExecutor(); // Executors.newSingleThreadExecutor();

    @Inject
    public CloseEncounterAnalysis(AppStatisticsService statisticsService, TrackingService trackingService, EventRepository eventRepository) {
        super(eventRepository, trackingService, null);
        this.statisticsService = statisticsService;
        this.analysisName = this.getClass().getSimpleName();
        this.nextRunTime = new Date(0L);
    }

    @Subscribe
    public void onMark(TimeEvent timeEvent) {
        LOG.debug(timeEvent.toString());

        Date now = new Date(timeEvent.getTimestamp());

        if (nextRunTime.before(now)) {
            executor.execute(() -> performAnalysis());
            nextRunTime = new Date(now.getTime() + ANALYS_PERIOD_MILLIS);
            LOG.debug("nextRunTime: " + nextRunTime);
        }
    }

    private void performAnalysis() {
        LOG.debug("Starting " + analysisName);

        Set<Track> tracks = getTrackingService().cloneTracks();
        tracks.forEach(t -> analyseCloseEncounters(tracks, t));

        statisticsService.incAnalysisStatistics(analysisName, "Analyses performed");
        LOG.debug("Finished " + analysisName);
    }

    private void analyseCloseEncounters(Set<Track> allTracks, Track track) {
        clearTrackPairsAnalyzed();
        Set<Track> nearByTracks = findNearByTracks(allTracks, track, 60000, 1852);
        nearByTracks.forEach(nearByTrack -> analyseCloseEncounter(track, nearByTrack));
    }

    void analyseCloseEncounter(Track track1, Track track2) {
        //  filterOutPreviouslyCompared(track);
        if (track1.getSpeedOverGround() > 5.0 && ! isTrackPairAnalyzed(track1, track2)) {
            SafetyZone safetyZone1 = computeSafetyZone(track1.getPosition(), track1, track2);
            SafetyZone safetyZone2 = computeSafetyZone(track1.getPosition(), track2, track1);
            if (safetyZone1 != null && safetyZone2 != null && safetyZone1.intersects(safetyZone2)) {
                raiseOrMaintainAbnormalEvent(CloseEncounterEvent.class, track1, track2);
            } else {
                lowerExistingAbnormalEventIfExists(CloseEncounterEvent.class, track1);
            }
            markTrackPairAnalyzed(track1, track2);
        } else {
            LOG.debug("PREVIOUSLY COMPARED " + track1.getMmsi() + " AGAINST " + track2.getMmsi());
        }
    }

    private Set<String> trackPairsAnalyzed;

    void clearTrackPairsAnalyzed() {
        trackPairsAnalyzed = new TreeSet<>();
    }

    void markTrackPairAnalyzed(Track track1, Track track2) {
        String trackPairKey = calculateTrackPairKey(track1, track2);
        trackPairsAnalyzed.add(trackPairKey);
    }

    static String calculateTrackPairKey(Track track1, Track track2) {
        int mmsi1 = track1.getMmsi();
        int mmsi2 = track2.getMmsi();
        return min(mmsi1, mmsi2) + "-" + max(mmsi1, mmsi2);
    }

    boolean isTrackPairAnalyzed(Track track1, Track track2) {
        String trackPairKey = calculateTrackPairKey(track1, track2);
        return trackPairsAnalyzed.contains(trackPairKey);
    }

    /**
     * Compute the safety zone for track in the context of otherTrack.
     * @param track
     * @param otherTrack
     * @return The safety zone for the track.
     */
    SafetyZone computeSafetyZone(Position cartesianCenter, Track track, Track otherTrack) {
        // Retrieve and validate required input variables
        Float   cogBoxed = track.getCourseOverGround();
        Integer loaBoxed = (Integer) track.getProperty(Track.VESSEL_LENGTH);
        Integer beamBoxed = (Integer) track.getProperty(Track.VESSEL_BEAM);
        Integer dimSternBoxed = (Integer) track.getProperty(Track.VESSEL_DIM_STERN);
        Integer dimStarboardBoxed = (Integer) track.getProperty(Track.VESSEL_DIM_STARBOARD);

        if (cogBoxed == null) {
            LOG.debug("MMSI " + track.getMmsi() + ": Cannot compute safety zone: No cog.");
            return null;
        }

        if (loaBoxed == null) {
            LOG.debug("MMSI " + track.getMmsi() + ": Cannot compute safety zone: No loa.");
            return null;
        }

        if (beamBoxed == null) {
            LOG.debug("MMSI " + track.getMmsi() + ": Cannot compute safety zone: No beam.");
            return null;
        }

        if (dimSternBoxed == null) {
            LOG.debug("MMSI " + track.getMmsi() + ": Cannot compute safety zone: No stern dimension.");
            return null;
        }

        if (dimStarboardBoxed == null) {
            LOG.debug("MMSI " + track.getMmsi() + ": Cannot compute safety zone: No starboard dimension.");
            return null;
        }

        final double cog = cogBoxed;
        final double loa = loaBoxed;
        final double beam = beamBoxed;
        final double dimStern = dimSternBoxed;
        final double dimStarboard = dimStarboardBoxed;

        // Setup configurable input constants
        final double safetyEllipseLength = 4;
        final double safetyEllipseBreadth = 5;
        final double safetyEllipseBehind = 0.5;   // = behindLength ???
        final double v = 1.0;
        final double l1 = max(safetyEllipseLength * v, 1.0 + safetyEllipseBehind*v*2.0);
        final double b1 = max(safetyEllipseBreadth * v, 1.5);
        final double xc = -safetyEllipseBehind*v+0.5*l1;

        // Compute direction of half axis alpha
        final double thetaDeg = compass2cartesian(cog);

        // Transform latitude/longitude to cartesian coordinates
        final double centerLatitude = cartesianCenter.getLatitude();
        final double centerLongitude = cartesianCenter.getLongitude();
        final CoordinateTransformer coordinateTransformer = new CoordinateTransformer(centerLongitude, centerLatitude);

        final double trackLatitude = track.getPosition().getLatitude();
        final double trackLongitude = track.getPosition().getLongitude();
        final double x = coordinateTransformer.lon2x(trackLongitude, trackLatitude);
        final double y = coordinateTransformer.lat2y(trackLongitude, trackLatitude);

        // Compute center of safety zone
        final Point pt0 = new Point(x, y);
        Point pt1 = new Point(pt0.getX() - dimStern + loa*xc, pt0.getY() + dimStarboard - beam/2.0);
        pt1 = pt1.rotate(pt0, thetaDeg);

        // Compute length of half axis alpha
        final double alpha = loa * l1 / 2.0;

        // Compute length of half axis beta
        final double beta = beam * b1 / 2.0;

        return new SafetyZone(pt1.getX(), pt1.getY(), alpha, beta, thetaDeg);
    }

    /**
     * Converts a compass heading to a cartesian angle
     * @param a
     * @return
     */
    private static double compass2cartesian(double a) {
        double cartesianAngle;

        if ((a >= 0.0) && (a <= 90.0)) {
            cartesianAngle = 90.0 - a;
        } else {
            cartesianAngle = 450.0 - a;
        }
        return cartesianAngle;
    }

    /**
     * In the set of tracks: find the tracks which are near to the track - with 'near'
     * defined as
     *
     * - last reported position timestamp within +/- 1 minute of track's
     * - last reported position within 1 nm of track
     *
     * @param tracks the set of candidate tracks to search among.
     * @param track the track to find other near-by tracks for.
     * @return the set of nearby tracks
     */
    Set<Track> findNearByTracks(Set<Track> tracks, Track track, int maxTimestampDeviationMillis, int maxDistanceDeviationMeters) {
        Set<Track> nearbyTracks = Collections.EMPTY_SET;

        TrackingReport positionReport = track.getPositionReport();

        if (positionReport != null) {
            long timestamp = positionReport.getTimestamp();
            Position position = positionReport.getPosition();

            nearbyTracks = tracks.stream().filter(t ->
                    t.getMmsi() != track.getMmsi() &&
                    t.getPositionReportTimestamp() != null &&
                    t.getPositionReportTimestamp() > positionReport.getTimestamp() - maxTimestampDeviationMillis &&
                    t.getPositionReportTimestamp() < positionReport.getTimestamp() + maxTimestampDeviationMillis &&
                    t.getPosition().distanceTo(track.getPosition(), CoordinateSystem.CARTESIAN) < maxDistanceDeviationMeters

            ).collect(Collectors.toSet());
        }

        return nearbyTracks;
    }

    @Override
    protected Event buildEvent(Track primaryTrack, Track... otherTracks) {
        if (otherTracks == null) {
            throw new IllegalArgumentException("otherTracks cannot be null.");
        }
        if (otherTracks.length != 1) {
            throw new IllegalArgumentException("otherTracks.length must be exactly 1, not " + otherTracks.length + ".");
        }
        final Track secondaryTrack = otherTracks[0];

        statisticsService.incAnalysisStatistics(analysisName, "Events raised");
        LOG.info(new Date(primaryTrack.getPositionReportTimestamp()) + ": Detected CloseEncounterEvent involving mmsi " + primaryTrack.getMmsi());

        final String desc = String.format("Close encounter");

        Event event =
            CloseEncounterEventBuilder.CloseEncounterEvent()
                    .description(desc)
                    .state(Event.State.ONGOING)
                    .startTime(new Date(primaryTrack.getPositionReportTimestamp()))
                    .behaviour()
                        .vessel()
                            .mmsi(primaryTrack.getMmsi())
                            .imo((Integer) primaryTrack.getProperty(Track.IMO))
                            .callsign((String) primaryTrack.getProperty(Track.CALLSIGN))
                            .name((String) primaryTrack.getProperty(Track.SHIP_NAME))
                        .trackingPoint()
                            .timestamp(new Date(primaryTrack.getPositionReportTimestamp()))
                            .positionInterpolated(primaryTrack.getPositionReport().isInterpolated())
                            .eventCertainty(TrackingPoint.EventCertainty.RAISED)
                            .speedOverGround(primaryTrack.getSpeedOverGround())
                            .courseOverGround(primaryTrack.getCourseOverGround())
                            .latitude(primaryTrack.getPosition().getLatitude())
                            .longitude(primaryTrack.getPosition().getLongitude())
                    .behaviour()
                        .vessel()
                            .mmsi(secondaryTrack.getMmsi())
                            .imo((Integer) secondaryTrack.getProperty(Track.IMO))
                            .callsign((String) secondaryTrack.getProperty(Track.CALLSIGN))
                            .name((String) secondaryTrack.getProperty(Track.SHIP_NAME))
                        .trackingPoint()
                            .timestamp(new Date(secondaryTrack.getPositionReportTimestamp()))
                            .positionInterpolated(secondaryTrack.getPositionReport().isInterpolated())
                            .eventCertainty(TrackingPoint.EventCertainty.RAISED)
                            .speedOverGround(secondaryTrack.getSpeedOverGround())
                            .courseOverGround(secondaryTrack.getCourseOverGround())
                            .latitude(secondaryTrack.getPosition().getLatitude())
                            .longitude(secondaryTrack.getPosition().getLongitude())
                .getEvent();

        addPreviousTrackingPoints(event, primaryTrack);
        addPreviousTrackingPoints(event, secondaryTrack);

        return event;
    }

}
