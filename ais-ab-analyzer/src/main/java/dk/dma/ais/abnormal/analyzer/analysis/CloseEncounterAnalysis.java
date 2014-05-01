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
import dk.dma.ais.abnormal.analyzer.geometry.Point;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.domain.CloseEncounterEvent;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.TrackingPoint;
import dk.dma.ais.abnormal.event.db.domain.builders.CloseEncounterEventBuilder;
import dk.dma.ais.abnormal.tracker.InterpolatedTrackingReport;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.Tracker;
import dk.dma.ais.abnormal.tracker.TrackingReport;
import dk.dma.ais.abnormal.tracker.events.TimeEvent;
import dk.dma.ais.abnormal.util.AisDataHelper;
import dk.dma.ais.abnormal.util.Categorizer;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Ellipse;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.model.geometry.util.CoordinateConverter;
import net.jcip.annotations.NotThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toSet;

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
    private final static int ANALYSIS_PERIOD_MILLIS = 30 * 1000;

    /**
     * The time when the analysis should next be run.
     */
    private Date nextRunTime;

    /**
     * Executor to perform the actual work.
     */
    private Executor executor = MoreExecutors.sameThreadExecutor(); // Executors.newSingleThreadExecutor();

    @Inject
    public CloseEncounterAnalysis(AppStatisticsService statisticsService, Tracker trackingService, EventRepository eventRepository) {
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
            nextRunTime = new Date(now.getTime() + ANALYSIS_PERIOD_MILLIS);
            LOG.debug("nextRunTime: " + nextRunTime);
        }
    }

    private void performAnalysis() {
        LOG.debug("Starting " + analysisName);
        final long systemTimeMillisBeforeAnalysis = System.currentTimeMillis();

        Set<Track> tracks = getTrackingService().cloneTracks();
        tracks.forEach(
                t -> analyseCloseEncounters(tracks, t)
        );

        final long systemTimeMillisAfterAnalysis = System.currentTimeMillis();
        statisticsService.incAnalysisStatistics(analysisName, "Analyses performed");
        LOG.debug(analysisName + " of " + tracks.size() + " tracks completed in " + (systemTimeMillisAfterAnalysis - systemTimeMillisBeforeAnalysis) + " msecs.");
    }

    private void analyseCloseEncounters(Set<Track> allTracks, Track track) {
        clearTrackPairsAnalyzed();
        if (! isSupportVessel(track)) {
            Set<Track> nearByTracks = findNearByTracks(allTracks, track, 60000, 1852);
            nearByTracks.forEach(nearByTrack -> {
                if (isFishingVessel(track) && isFishingVessel(nearByTrack)) { return; }
                if (isUndefinedVessel(track) && isUndefinedVessel(nearByTrack)) { return; }
                if (isSupportVessel(nearByTrack)) { return; };
                if (isSlowVessel(track) && isSlowVessel(nearByTrack)) { return; };

                analyseCloseEncounter(track, nearByTrack);
            });
        }
    }

    void analyseCloseEncounter(Track track1, Track track2) {
        if (track1.getSpeedOverGround() > 5.0 && ! isTrackPairAnalyzed(track1, track2)) {

            if (track1.getTimeOfLastPositionReport() < track2.getTimeOfLastPositionReport()) {
                track1.predict(track2.getTimeOfLastPositionReport());
            } else if (track2.getTimeOfLastPositionReport() < track1.getTimeOfLastPositionReport()) {
                track2.predict(track1.getTimeOfLastPositionReport());
            }

            Ellipse safetyEllipseTrack1 = computeSafetyZone(track1.getPosition(), track1, track2);
            Ellipse extentTrack2 = computeVesselExtent(track1.getPosition(), track2);

            if (safetyEllipseTrack1 != null && extentTrack2 != null && safetyEllipseTrack1.intersects(extentTrack2)) {
                track1.setProperty(Track.SAFETY_ZONE, safetyEllipseTrack1);
                track2.setProperty(Track.EXTENT, extentTrack2);
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
     * Compute the an elliptic zone which roughly corresponds to the vessel's physical extent.
     * @param cartesianCenter
     * @param track
     * @return
     */
    Ellipse computeVesselExtent(Position cartesianCenter, Track track) {
        return computeZone(cartesianCenter, track, 1.0, 1.0, 0.5);
    }

    /**
     * Compute the safety zone of track. This is roughly equivalent to the elliptic area around the vessel
     * which its navigator would observe for safety reasons to avoid imminent collisions.
     *
     * @param cartesianCenter
     * @param track
     * @param otherTrack
     * @return
     */
    Ellipse computeSafetyZone(Position cartesianCenter, Track track, Track otherTrack) {
        final double safetyEllipseLength = 4;
        final double safetyEllipseBreadth = 5;
        final double safetyEllipseBehind = 0.5;   // = behindLength ???
        final double v = 1.0;
        final double l1 = max(safetyEllipseLength * v, 1.0 + safetyEllipseBehind*v*2.0);
        final double b1 = max(safetyEllipseBreadth * v, 1.5);
        final double xc = -safetyEllipseBehind * v + 0.5*l1;

        return computeZone(cartesianCenter, track, l1, b1, xc);
    }

    /**
     * Compute an elliptic zone around a track.
     * @param geodeticReference
     * @param track
     * @param l1
     * @param b1
     * @param xc
     * @return The safety zone for the track.
     */
    private Ellipse computeZone(Position geodeticReference, Track track, double l1, double b1, double xc) {
        // Retrieve and validate required input variables
        Float   cogBoxed = track.getCourseOverGround();
        Integer loaBoxed = track.getVesselLength();
        Integer beamBoxed = track.getVesselBeam();
        Integer dimSternBoxed = track.getShipDimensionStern();
        Integer dimStarboardBoxed = track.getShipDimensionStarboard();

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

        // Compute direction of half axis alpha
        final double thetaDeg = CoordinateConverter.compass2cartesian(cog);

        // Transform latitude/longitude to cartesian coordinates
        final double centerLatitude = geodeticReference.getLatitude();
        final double centerLongitude = geodeticReference.getLongitude();
        final CoordinateConverter CoordinateConverter = new CoordinateConverter(centerLongitude, centerLatitude);

        final double trackLatitude = track.getPosition().getLatitude();
        final double trackLongitude = track.getPosition().getLongitude();
        final double x = CoordinateConverter.lon2x(trackLongitude, trackLatitude);
        final double y = CoordinateConverter.lat2y(trackLongitude, trackLatitude);

        // Compute center of safety zone
        final Point pt0 = new Point(x, y);
        Point pt1 = new Point(pt0.getX() - dimStern + loa*xc, pt0.getY() + dimStarboard - beam/2.0);
        pt1 = pt1.rotate(pt0, thetaDeg);

        // Compute length of half axis alpha
        final double alpha = loa * l1 / 2.0;

        // Compute length of half axis beta
        final double beta = beam * b1 / 2.0;

        return new Ellipse(geodeticReference, pt1.getX(), pt1.getY(), alpha, beta, thetaDeg, CoordinateSystem.CARTESIAN);
    }

    /**
     * In the set of candidateTracks: find the candidateTracks which are near to the nearToTrack - with 'near'
     * defined as
     *
     * - last reported position timestamp within +/- 1 minute of nearToTrack's
     * - last reported position within 1 nm of nearToTrack
     *
     * @param candidateTracks the set of candidate candidateTracks to search among.
     * @param nearToTrack the nearToTrack to find other near-by candidateTracks for.
     * @return the set of nearby candidateTracks
     */
    Set<Track> findNearByTracks(Set<Track> candidateTracks, Track nearToTrack, int maxTimestampDeviationMillis, int maxDistanceDeviationMeters) {
        Set<Track> nearbyTracks = Collections.EMPTY_SET;

        TrackingReport positionReport = nearToTrack.getNewestTrackingReport();

        if (positionReport != null) {
            final long timestamp = positionReport.getTimestamp();

            nearbyTracks = candidateTracks.stream().filter(candidateTrack ->
                    candidateTrack.getMmsi() != nearToTrack.getMmsi() &&
                    candidateTrack.getTimeOfLastPositionReport() > 0L &&
                    candidateTrack.getTimeOfLastPositionReport() > timestamp - maxTimestampDeviationMillis &&
                    candidateTrack.getTimeOfLastPositionReport() < timestamp + maxTimestampDeviationMillis &&
                    candidateTrack.getPosition().distanceTo(nearToTrack.getPosition(), CoordinateSystem.CARTESIAN) < maxDistanceDeviationMeters
            ).collect(toSet());
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

        String primaryShipName = primaryTrack.getShipName();
        primaryShipName = AisDataHelper.trimAisString(primaryShipName);

        String secondaryShipName = secondaryTrack.getShipName();
        secondaryShipName = AisDataHelper.trimAisString(secondaryShipName);

        String primaryShipType = "?";
        Integer primaryShipTypeBoxed = primaryTrack.getShipType();
        if (primaryShipTypeBoxed != null) {
            short primaryShipTypeCategory = Categorizer.mapShipTypeToCategory(primaryShipTypeBoxed);
            primaryShipType = Categorizer.mapShipTypeCategoryToString(primaryShipTypeCategory);
        }

        String secondaryShipType = "?";
        Integer secondaryShipTypeBoxed = secondaryTrack.getShipType();
        if (secondaryShipTypeBoxed != null) {
            short secondaryShipTypeCategory = Categorizer.mapShipTypeToCategory(secondaryShipTypeBoxed);
            secondaryShipType = Categorizer.mapShipTypeCategoryToString(secondaryShipTypeCategory);
        }

        StringBuffer description = new StringBuffer();
        description.append("Close encounter between ");
        description.append(primaryShipName);
        description.append(" (" + primaryShipType + ") and ");
        description.append(secondaryShipName);
        description.append(" (" + secondaryShipType + "). ");

        Ellipse primaryTracksafetyEllipse = (Ellipse) primaryTrack.getProperty(Track.SAFETY_ZONE);
        Ellipse secondaryTrackExtent = (Ellipse) secondaryTrack.getProperty(Track.EXTENT);

        CoordinateConverter CoordinateConverter = new CoordinateConverter(primaryTracksafetyEllipse.getGeodeticReference().getLongitude(), primaryTracksafetyEllipse.getGeodeticReference().getLatitude());
        double primaryTrackLatitude = CoordinateConverter.y2Lat(primaryTracksafetyEllipse.getX(), primaryTracksafetyEllipse.getY());
        double primaryTrackLongitude = CoordinateConverter.x2Lon(primaryTracksafetyEllipse.getX(), primaryTracksafetyEllipse.getY());
        double secondaryTrackLatitude = CoordinateConverter.y2Lat(secondaryTrackExtent.getX(), secondaryTrackExtent.getY());
        double secondaryTrackLongitude = CoordinateConverter.x2Lon(secondaryTrackExtent.getX(), secondaryTrackExtent.getY());

        statisticsService.incAnalysisStatistics(analysisName, "Events raised");

        LOG.info(new Date(primaryTrack.getTimeOfLastPositionReport()) + ": Detected CloseEncounterEvent: " + description.toString());


        Event event =
            CloseEncounterEventBuilder.CloseEncounterEvent()
                    .safetyZoneOfPrimaryVessel()
                        .targetTimestamp(new Date(primaryTrack.getTimeOfLastPositionReport()))
                        .centerLatitude(primaryTrackLatitude)
                        .centerLongitude(primaryTrackLongitude)
                        .majorAxisHeading(primaryTracksafetyEllipse.getMajorAxisGeodeticHeading())
                        .majorSemiAxisLength(primaryTracksafetyEllipse.getAlpha())
                        .minorSemiAxisLength(primaryTracksafetyEllipse.getBeta())
                    .extentOfSecondaryVessel()
                        .targetTimestamp(new Date(secondaryTrack.getTimeOfLastPositionReport()))
                        .centerLatitude(secondaryTrackLatitude)
                        .centerLongitude(secondaryTrackLongitude)
                        .majorAxisHeading(secondaryTrackExtent.getMajorAxisGeodeticHeading())
                        .majorSemiAxisLength(secondaryTrackExtent.getAlpha())
                        .minorSemiAxisLength(secondaryTrackExtent.getBeta())
                    .description(description.toString())
                    .state(Event.State.ONGOING)
                    .startTime(new Date(primaryTrack.getTimeOfLastPositionReport()))
                    .behaviour()
                        .vessel()
                            .mmsi(primaryTrack.getMmsi())
                            .imo(primaryTrack.getIMO())
                            .callsign(primaryTrack.getCallsign())
                            .name(primaryTrack.getShipName())
                        .trackingPoint()
                            .timestamp(new Date(primaryTrack.getTimeOfLastPositionReport()))
                            .positionInterpolated(primaryTrack.getNewestTrackingReport() instanceof InterpolatedTrackingReport)
                            .eventCertainty(TrackingPoint.EventCertainty.RAISED)
                            .speedOverGround(primaryTrack.getSpeedOverGround())
                            .courseOverGround(primaryTrack.getCourseOverGround())
                            .latitude(primaryTrack.getPosition().getLatitude())
                            .longitude(primaryTrack.getPosition().getLongitude())
                    .behaviour()
                        .vessel()
                            .mmsi(secondaryTrack.getMmsi())
                            .imo(secondaryTrack.getIMO())
                            .callsign(secondaryTrack.getCallsign())
                            .name(secondaryTrack.getShipName())
                        .trackingPoint()
                            .timestamp(new Date(secondaryTrack.getTimeOfLastPositionReport()))
                            .positionInterpolated(secondaryTrack.getNewestTrackingReport() instanceof InterpolatedTrackingReport)
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

    private static boolean isSupportVessel(Track track) {
        Integer shipType = track.getShipType();
        return shipType != null && Categorizer.mapShipTypeToCategory(shipType) == 4;
    }

    private static boolean isFishingVessel(Track track) {
        Integer shipType = track.getShipType();
        return shipType != null && Categorizer.mapShipTypeToCategory(shipType) == 5;
    }

    private static boolean isUndefinedVessel(Track track) {
        Integer shipType = track.getShipType();
        return shipType != null && Categorizer.mapShipTypeToCategory(shipType) == 8;
    }

    private static boolean isSlowVessel(Track track) {
        return track.getSpeedOverGround() < 3.0;
    }

}
