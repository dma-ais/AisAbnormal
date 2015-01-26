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

import com.google.inject.Inject;
import dk.dma.ais.abnormal.analyzer.AppStatisticsService;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.domain.CloseEncounterEvent;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.TrackingPoint;
import dk.dma.ais.abnormal.event.db.domain.builders.CloseEncounterEventBuilder;
import dk.dma.ais.abnormal.tracker.InterpolatedTrackingReport;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.Tracker;
import dk.dma.ais.abnormal.tracker.TrackingReport;
import dk.dma.ais.abnormal.util.Categorizer;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Ellipse;
import dk.dma.enav.util.CoordinateConverter;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_CLOSEENCOUNTER_PREDICTIONTIME_MAX;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_CLOSEENCOUNTER_RUN_PERIOD;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_CLOSEENCOUNTER_SOG_MIN;
import static dk.dma.ais.abnormal.util.AisDataHelper.nameOrMmsi;
import static dk.dma.ais.abnormal.util.TrackPredicates.isEngagedInFishing;
import static dk.dma.ais.abnormal.util.TrackPredicates.isEngagedInTowing;
import static dk.dma.ais.abnormal.util.TrackPredicates.isFishingVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isSlowVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isSmallVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isSupportVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isUndefinedVessel;
import static dk.dma.enav.safety.SafetyZones.safetyZone;
import static dk.dma.enav.safety.SafetyZones.vesselExtent;
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
 *
 * @author Thomas Borg Salling <tbsalling@tbsalling.dk>
 */
@NotThreadSafe
public class CloseEncounterAnalysis extends PeriodicAnalysis {
    private static final Logger LOG = LoggerFactory.getLogger(CloseEncounterAnalysis.class);

    private final AppStatisticsService statisticsService;
    private final String analysisName;

    /** Minimum speed over ground to consider close encounter (in knots) */
    private final float sogMin;

    /** Maximum time a track may be predicted and still be included in analysis (in minutes) */
    private final int predictionTimeMax;

    @Inject
    public CloseEncounterAnalysis(Configuration configuration, AppStatisticsService statisticsService, Tracker trackingService, EventRepository eventRepository) {
        super(eventRepository, trackingService, null);
        this.statisticsService = statisticsService;
        this.analysisName = this.getClass().getSimpleName();
        this.sogMin = configuration.getFloat(CONFKEY_ANALYSIS_CLOSEENCOUNTER_SOG_MIN, 5.0f);
        this.predictionTimeMax = configuration.getInteger(CONFKEY_ANALYSIS_CLOSEENCOUNTER_PREDICTIONTIME_MAX, -1);
        setAnalysisPeriodMillis(configuration.getInt(CONFKEY_ANALYSIS_CLOSEENCOUNTER_RUN_PERIOD, 30000) * 1000);
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    @Override
    public String toString() {
        return "CloseEncounterAnalysis{" +
                "sogMin=" + sogMin +
                "} " + super.toString();
    }

    protected void performAnalysis() {
        LOG.debug("Starting " + analysisName);
        final long systemTimeMillisBeforeAnalysis = System.currentTimeMillis();

        Collection<Track> tracks = getTrackingService().getTracks();
        tracks.forEach(
            t -> analyseCloseEncounters(tracks, t)
        );

        final long systemTimeMillisAfterAnalysis = System.currentTimeMillis();
        statisticsService.incAnalysisStatistics(analysisName, "Analyses performed");
        LOG.debug(analysisName + " of " + tracks.size() + " tracks completed in " + (systemTimeMillisAfterAnalysis - systemTimeMillisBeforeAnalysis) + " msecs.");
    }

    private void analyseCloseEncounters(Collection<Track> allTracks, Track track) {
        clearTrackPairsAnalyzed();
        if (isSupportVessel.negate().test(track) && isEngagedInTowing.negate().test(track)) {
            findNearByTracks(allTracks, track, 60000, 1852)
                .stream()
                .filter(isSupportVessel.negate())
                .filter(isEngagedInTowing.negate())
                .forEach(nearByTrack -> {
                    if (isFishingVessel.test(track) && isFishingVessel.test(nearByTrack)) {
                        return;
                    }
                    if (isUndefinedVessel.test(track) && isUndefinedVessel.test(nearByTrack)) {
                        return;
                    }
                    if (isSlowVessel.test(track) && isSlowVessel.test(nearByTrack)) {
                        return;
                    }
                    if (isSmallVessel.test(track) && isSmallVessel.test(nearByTrack)) {
                        return;
                    }
                    if (isEngagedInFishing.test(track) && isEngagedInFishing.test(nearByTrack)) {
                        return;
                    }
                    if (! (track.getSpeedOverGround() > sogMin || nearByTrack.getSpeedOverGround() > sogMin)) {
                        return;
                    }
                    analyseCloseEncounter(track, nearByTrack);
                });
        }
    }

    void analyseCloseEncounter(Track track1, Track track2) {
        if (track1.getSpeedOverGround() > sogMin && ! isTrackPairAnalyzed(track1, track2)) {

            final long t = max(track1.getTimeOfLastPositionReport(), track2.getTimeOfLastPositionReport());

            if (t > track1.getTimeOfLastPositionReport()) {
                track1.predict(t);
            }
            if (t > track2.getTimeOfLastPositionReport()) {
                track2.predict(t);
            }

            if (isLastAisTrackingReportTooOld(track1, t)) {
                LOG.debug("Skipping analysis: MMSI " + track1.getMmsi() + " was predicted for too long.");
                return;
            }
            if (isLastAisTrackingReportTooOld(track2, t)) {
                LOG.debug("Skipping analysis: MMSI " + track2.getMmsi() + " was predicted for too long.");
                return;
            }

            boolean allValuesPresent = false;
            float track1Cog=Float.NaN, track1Sog=Float.NaN, track2Hdg=Float.NaN;
            int track1Loa=-1, track1Beam=-1, track1Stern=-1, track1Starboard=-1, track2Loa=-1, track2Beam=-1, track2Stern=-1, track2Starboard=-1;
            try {
                track1Cog = track1.getCourseOverGround();
                track1Sog = track1.getSpeedOverGround();
                track1Loa = track1.getVesselLength();
                track1Beam = track1.getVesselBeam();
                track1Stern = track1.getShipDimensionStern();
                track1Starboard = track1.getShipDimensionStarboard();
                track2Hdg = track2.getTrueHeading();
                track2Loa = track2.getVesselLength();
                track2Beam = track2.getVesselBeam();
                track2Stern = track2.getShipDimensionStern();
                track2Starboard = track2.getShipDimensionStarboard();
                allValuesPresent = true;
            } catch(NullPointerException e) {
            }

            if (allValuesPresent && !Float.isNaN(track1Cog) && !Float.isNaN(track2Hdg)) {
                Ellipse safetyEllipseTrack1 = safetyZone(track1.getPosition(), track1.getPosition(), track1Cog, track1Sog, track1Loa, track1Beam, track1Stern, track1Starboard);
                Ellipse extentTrack2 = vesselExtent(track1.getPosition(), track2.getPosition(), track2Hdg, track2Loa, track2Beam, track2Stern, track2Starboard);

                if (safetyEllipseTrack1 != null && extentTrack2 != null && safetyEllipseTrack1.intersects(extentTrack2)) {
                    track1.setProperty(Track.SAFETY_ZONE, safetyEllipseTrack1);
                    track2.setProperty(Track.EXTENT, extentTrack2);
                    raiseOrMaintainAbnormalEvent(CloseEncounterEvent.class, track1, track2);
                } else {
                    lowerExistingAbnormalEventIfExists(CloseEncounterEvent.class, track1);
                }
            }

            markTrackPairAnalyzed(track1, track2);
        } else {
            LOG.debug("PREVIOUSLY COMPARED " + track1.getMmsi() + " AGAINST " + track2.getMmsi());
        }
    }

    /** If a max. age of last AIS message is provided, then use it. */
    private boolean isLastAisTrackingReportTooOld(Track track, long now) {
        if (predictionTimeMax == -1) {
            return false;
        }
        final long timeOfLastAisTrackingReport = track.getTimeOfLastAisTrackingReport();
        return timeOfLastAisTrackingReport != -1 && now - timeOfLastAisTrackingReport > predictionTimeMax*60*1000;
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
    Set<Track> findNearByTracks(Collection<Track> candidateTracks, Track nearToTrack, int maxTimestampDeviationMillis, int maxDistanceDeviationMeters) {
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

        String primaryShipName = nameOrMmsi(primaryTrack.getShipName(), primaryTrack.getMmsi());
        String secondaryShipName = nameOrMmsi(secondaryTrack.getShipName(), secondaryTrack.getMmsi());

        String primaryShipType = "unknown type";
        Integer primaryShipTypeBoxed = primaryTrack.getShipType();
        short primaryShipTypeCategory = Categorizer.mapShipTypeToCategory(primaryShipTypeBoxed);
        if (primaryShipTypeBoxed != null) {
            primaryShipType = Categorizer.mapShipTypeCategoryToString(primaryShipTypeCategory);
        }
        short primaryShipLengthCategory = Categorizer.mapShipLengthToCategory(primaryTrack.getVesselLength());

        String secondaryShipType = "?";
        Integer secondaryShipTypeBoxed = secondaryTrack.getShipType();
        short secondaryShipTypeCategory = Categorizer.mapShipTypeToCategory(secondaryShipTypeBoxed);
        if (secondaryShipTypeBoxed != null) {
            secondaryShipType = Categorizer.mapShipTypeCategoryToString(secondaryShipTypeCategory);
        }
        short secondaryShipLengthCategory = Categorizer.mapShipLengthToCategory(secondaryTrack.getVesselLength());

        StringBuffer title = new StringBuffer();
        title.append("Close encounter");

        StringBuffer description = new StringBuffer();
        description.append("Close encounter between ");
        description.append(primaryShipName);
        description.append(" (" + primaryShipType + ") and ");
        description.append(secondaryShipName);
        description.append(" (" + secondaryShipType + ") on ");
        description.append(DATE_FORMAT.format(new Date(primaryTrack.getTimeOfLastPositionReport())));
        description.append(".");

        Ellipse primaryTrackSafetyEllipse = (Ellipse) primaryTrack.getProperty(Track.SAFETY_ZONE);
        Ellipse secondaryTrackExtent = (Ellipse) secondaryTrack.getProperty(Track.EXTENT);

        CoordinateConverter CoordinateConverter = new CoordinateConverter(primaryTrackSafetyEllipse.getGeodeticReference().getLongitude(), primaryTrackSafetyEllipse.getGeodeticReference().getLatitude());
        double primaryTrackLatitude = CoordinateConverter.y2Lat(primaryTrackSafetyEllipse.getX(), primaryTrackSafetyEllipse.getY());
        double primaryTrackLongitude = CoordinateConverter.x2Lon(primaryTrackSafetyEllipse.getX(), primaryTrackSafetyEllipse.getY());
        double secondaryTrackLatitude = CoordinateConverter.y2Lat(secondaryTrackExtent.getX(), secondaryTrackExtent.getY());
        double secondaryTrackLongitude = CoordinateConverter.x2Lon(secondaryTrackExtent.getX(), secondaryTrackExtent.getY());

        statisticsService.incAnalysisStatistics(analysisName, "Events raised");

        LOG.info(description.toString());

        Event event =
            CloseEncounterEventBuilder.CloseEncounterEvent()
                    .safetyZoneOfPrimaryVessel()
                        .targetTimestamp(new Date(primaryTrack.getTimeOfLastPositionReport()))
                        .centerLatitude(primaryTrackLatitude)
                        .centerLongitude(primaryTrackLongitude)
                        .majorAxisHeading(primaryTrackSafetyEllipse.getMajorAxisGeodeticHeading())
                        .majorSemiAxisLength(primaryTrackSafetyEllipse.getAlpha())
                        .minorSemiAxisLength(primaryTrackSafetyEllipse.getBeta())
                    .extentOfSecondaryVessel()
                        .targetTimestamp(new Date(secondaryTrack.getTimeOfLastPositionReport()))
                        .centerLatitude(secondaryTrackLatitude)
                        .centerLongitude(secondaryTrackLongitude)
                        .majorAxisHeading(secondaryTrackExtent.getMajorAxisGeodeticHeading())
                        .majorSemiAxisLength(secondaryTrackExtent.getAlpha())
                        .minorSemiAxisLength(secondaryTrackExtent.getBeta())
                    .title(title.toString())
                    .description(description.toString())
                    .state(Event.State.ONGOING)
                    .startTime(new Date(primaryTrack.getTimeOfLastPositionReport()))
                    .behaviour()
                        .isPrimary(true)
                        .vessel()
                            .mmsi(primaryTrack.getMmsi())
                            .imo(primaryTrack.getIMO())
                            .callsign(primaryTrack.getCallsign())
                            .type(primaryShipTypeBoxed /* primaryShipTypeCategory */)
                            .toBow(primaryTrack.getShipDimensionBow())
                            .toStern(primaryTrack.getShipDimensionStern())
                            .toPort(primaryTrack.getShipDimensionPort())
                            .toStarboard(primaryTrack.getShipDimensionStarboard())
                            .name(primaryTrack.getShipName())
                        .trackingPoint()
                            .timestamp(new Date(primaryTrack.getTimeOfLastPositionReport()))
                            .positionInterpolated(primaryTrack.getNewestTrackingReport() instanceof InterpolatedTrackingReport)
                            .eventCertainty(TrackingPoint.EventCertainty.RAISED)
                            .speedOverGround(primaryTrack.getSpeedOverGround())
                            .courseOverGround(primaryTrack.getCourseOverGround())
                            .trueHeading(primaryTrack.getTrueHeading())
                            .latitude(primaryTrack.getPosition().getLatitude())
                            .longitude(primaryTrack.getPosition().getLongitude())
                    .behaviour()
                        .isPrimary(false)
                        .vessel()
                            .mmsi(secondaryTrack.getMmsi())
                            .imo(secondaryTrack.getIMO())
                            .callsign(secondaryTrack.getCallsign())
                            .type(secondaryShipTypeBoxed /* secondaryShipTypeCategory */)
                            .toBow(secondaryTrack.getShipDimensionBow())
                            .toStern(secondaryTrack.getShipDimensionStern())
                            .toPort(secondaryTrack.getShipDimensionPort())
                            .toStarboard(secondaryTrack.getShipDimensionStarboard())
                            .name(secondaryTrack.getShipName())
                        .trackingPoint()
                            .timestamp(new Date(secondaryTrack.getTimeOfLastPositionReport()))
                            .positionInterpolated(secondaryTrack.getNewestTrackingReport() instanceof InterpolatedTrackingReport)
                            .eventCertainty(TrackingPoint.EventCertainty.RAISED)
                            .speedOverGround(secondaryTrack.getSpeedOverGround())
                            .courseOverGround(secondaryTrack.getCourseOverGround())
                            .trueHeading(secondaryTrack.getTrueHeading())
                            .latitude(secondaryTrack.getPosition().getLatitude())
                            .longitude(secondaryTrack.getPosition().getLongitude())
                .getEvent();

        addPreviousTrackingPoints(event, primaryTrack);
        addPreviousTrackingPoints(event, secondaryTrack);

        return event;
    }

}
