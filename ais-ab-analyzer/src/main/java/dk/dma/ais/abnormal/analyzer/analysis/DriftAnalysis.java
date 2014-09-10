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
import dk.dma.ais.abnormal.event.db.domain.DriftEvent;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.TrackingPoint;
import dk.dma.ais.abnormal.event.db.domain.builders.DriftEventBuilder;
import dk.dma.ais.abnormal.tracker.InterpolatedTrackingReport;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.Tracker;
import dk.dma.ais.abnormal.tracker.events.PositionChangedEvent;
import dk.dma.ais.abnormal.tracker.events.TrackStaleEvent;
import dk.dma.enav.model.geometry.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.TreeSet;

import static dk.dma.ais.abnormal.util.AisDataHelper.isCourseOverGroundAvailable;
import static dk.dma.ais.abnormal.util.AisDataHelper.isSpeedOverGroundAvailable;
import static dk.dma.ais.abnormal.util.AisDataHelper.isTrueHeadingAvailable;
import static dk.dma.ais.abnormal.util.AisDataHelper.nameOrMmsi;
import static dk.dma.ais.abnormal.util.TrackPredicates.isCargoVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isClassB;
import static dk.dma.ais.abnormal.util.TrackPredicates.isEngagedInTowing;
import static dk.dma.ais.abnormal.util.TrackPredicates.isFishingVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isLongVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isPassengerVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isSmallVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isSpecialCraft;
import static dk.dma.ais.abnormal.util.TrackPredicates.isTankerVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isUnknownTypeOrSize;
import static dk.dma.ais.abnormal.util.TrackPredicates.isVeryLongVessel;

/**
 * This analysis detects vessels with indication for drift.
 *
 * The analysis is based on deviation between course over ground and heading. For vessels
 * with low speed over ground: If a large part of reports received over an interval
 * show a significant deviation between cog and hdg then an event is raised.
 *
 * This analysis is not based on previous observations (statistic data).
 *
 * @author Thomas Borg Salling <tbsalling@tbsalling.dk>
 */

public class DriftAnalysis extends Analysis {
    private static final Logger LOG = LoggerFactory.getLogger(DriftAnalysis.class);

    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final AppStatisticsService statisticsService;

    /** Track must have sustained sog below this mark to a cause drift event */
    static final float SPEED_HIGH_MARK = 5.0f;

    /** Track must have sustained sog above this mark to a cause drift event */
    static final float SPEED_LOW_MARK = 1.0f;

    /** Minimum no. of degrees to consider heading/course deviation significant */
    static final long MIN_HDG_COG_DEVIATION_DEGREES = 45;

    /** Tracks must have low speed and cog/hdg deviation for this period of time before a drift event is raised */
    static final int OBSERVATION_PERIOD_MINUTES = 10;

    private TreeSet<Integer> tracksPossiblyDrifting = new TreeSet<>();

    private final String analysisName;

    private int statCount = 0;

    @Inject
    public DriftAnalysis(AppStatisticsService statisticsService, Tracker trackingService, EventRepository eventRepository) {
        super(eventRepository, trackingService, null);
        this.statisticsService = statisticsService;
        this.analysisName = this.getClass().getSimpleName();
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onSpeedOverGroundUpdated(PositionChangedEvent trackEvent) {
        final Track track = trackEvent.getTrack();

        if (  !isSpeedOverGroundAvailable(track.getSpeedOverGround())
           || !isCourseOverGroundAvailable(track.getCourseOverGround())
           || !isTrueHeadingAvailable(track.getTrueHeading())) {
            return;
        }

        /* Do not perform analysis for vessels with these characteristics: */
        if (isClassB.test(track)
            || isUnknownTypeOrSize.test(track)
            || isFishingVessel.test(track)
            || isSmallVessel.test(track)
            || isSpecialCraft.test(track)
            || isEngagedInTowing.test(track)
        ) {
            return;
        }

        /* Perform analysis only for very long vessels and some long vessels: */
        if (isVeryLongVessel.test(track)
            || (isLongVessel.test(track) && (isCargoVessel.test(track) || isTankerVessel.test(track) || isPassengerVessel.test(track)))
        ) {
            performAnalysis(track);
            updateApplicationStatistics();
        }
    }

    private void updateApplicationStatistics() {
        statisticsService.incAnalysisStatistics(analysisName, "Analyses performed");
        if (statCount++ % 10000 == 0) {
            statisticsService.setAnalysisStatistics(analysisName, "# observation list", tracksPossiblyDrifting.size());
        }
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onTrackStale(TrackStaleEvent trackEvent) {
        final int mmsi = trackEvent.getTrack().getMmsi();
        if (tracksPossiblyDrifting.contains(mmsi)) {
            LOG.debug(nameOrMmsi(trackEvent.getTrack().getShipName(), mmsi) + " is now stale. Removed from observation list.");
            tracksPossiblyDrifting.remove(mmsi);

            // TODO lowerEventIfRaised();

        }
    }

    private void performAnalysis(Track track) {
        final int mmsi = track.getMmsi();
        final float sog = track.getSpeedOverGround();
        final float cog = track.getCourseOverGround();
        final float hdg = track.getTrueHeading();

        if (sog >= SPEED_LOW_MARK && sog <= SPEED_HIGH_MARK && isSignificantDeviation(cog, hdg)) {
            LOG.debug(nameOrMmsi(track.getShipName(), mmsi) + " exhibits possible drift. Added to observation list.");
            tracksPossiblyDrifting.add(mmsi);
            if (isSustainedDrift(track)) {
                LOG.debug(nameOrMmsi(track.getShipName(), mmsi) + " exhibits sustained drift. Event raised or maintained.");
                raiseOrMaintainAbnormalEvent(DriftEvent.class, track);
            }
        } else if (tracksPossiblyDrifting.contains(mmsi)) {
            LOG.debug(nameOrMmsi(track.getShipName(), mmsi) + " appears not be drifting anymore. Removed from observation list.");
            tracksPossiblyDrifting.remove(mmsi);
            lowerExistingAbnormalEventIfExists(DriftEvent.class, track);
        }
    }

    static boolean isSignificantDeviation(float cog, float hdg) {
        return absoluteDirectionalDifference(cog, hdg) > MIN_HDG_COG_DEVIATION_DEGREES;
    }

    private static boolean isTrackingPeriodLongEnough(Track track) {
        return track.getNewestTrackingReport().getTimestamp() - track.getOldestTrackingReport().getTimestamp() > OBSERVATION_PERIOD_MINUTES*60*1000;
    }

    static boolean isSustainedDrift(Track track) {
        if (!isTrackingPeriodLongEnough(track)) {
            LOG.debug(nameOrMmsi(track.getShipName(), track.getMmsi()) + " not observed for long enough to consider sustained drift.");
            return false;
        }

        final long t1 = track.getNewestTrackingReport().getTimestamp() - OBSERVATION_PERIOD_MINUTES*60*1000;

        return track.getTrackingReports()
            .stream()
            .filter(tr -> tr.getTimestamp() >= t1)
            .allMatch(tr -> tr.getSpeedOverGround() >= SPEED_LOW_MARK && tr.getSpeedOverGround() <= SPEED_HIGH_MARK && isSignificantDeviation(tr.getCourseOverGround(), tr.getTrueHeading()));
    }

    private static float absoluteDirectionalDifference(float dir1, float dir2)
    {
        dir1 = directionInCompassRange(dir1);
        dir2 = directionInCompassRange(dir2);

        float diff = dir1>dir2 ? dir1-dir2 : dir2-dir1;

        if (diff >= 180.0) {
            diff = 360.0f - diff;
        }

        return diff;
    }

    /**
     * Convert the input to a number of degrees lying inside the compass
     * circle. E.g. -45 -> 315 or 370 -> 10.
     *
     * @param degrees
     * @return
     */
    private static float directionInCompassRange(float degrees) {
        degrees = degrees % 360.0f;
        if (degrees < 0.0) {
            degrees += 360.0f;
        }
        return degrees;
    }

    @Override
    protected Event buildEvent(Track track, Track... otherTracks) {
        if (otherTracks != null && otherTracks.length > 0) {
            throw new IllegalArgumentException("otherTracks not supported.");
        }

        final Integer mmsi = track.getMmsi();
        final Integer imo = track.getIMO();
        final String callsign = track.getCallsign();
        final String name = nameOrMmsi(track.getShipName(), mmsi);
        final Integer shipType = track.getShipType();
        final Integer shipDimensionToBow = track.getShipDimensionBow();
        final Integer shipDimensionToStern = track.getShipDimensionStern();
        final Integer shipDimensionToPort = track.getShipDimensionPort();
        final Integer shipDimensionToStarboard = track.getShipDimensionStarboard();
        final Date positionTimestamp = new Date(track.getTimeOfLastPositionReport());
        final Position position = track.getPosition();
        final Float cog = track.getCourseOverGround();
        final Float sog = track.getSpeedOverGround();
        final Float hdg = track.getTrueHeading();
        final Boolean interpolated = track.getNewestTrackingReport() instanceof InterpolatedTrackingReport;
        final TrackingPoint.EventCertainty certainty = TrackingPoint.EventCertainty.RAISED;

        final String title = "Drift";
        final String description = String.format("%s is drifting on position %s at %s", name, position, DATE_FORMAT.format(positionTimestamp));

        LOG.info(description);

        Event event =
            DriftEventBuilder.DriftEvent()
                .title(title)
                .description(description)
                .startTime(positionTimestamp)
                .behaviour()
                    .isPrimary(true)
                    .vessel()
                    .mmsi(mmsi)
                    .imo(imo)
                    .callsign(callsign)
                    .type(shipType /* shipTypeCategory */)
                    .toBow(shipDimensionToBow)
                    .toStern(shipDimensionToStern)
                    .toPort(shipDimensionToPort)
                    .toStarboard(shipDimensionToStarboard)
                    .name(name)
                .trackingPoint()
                    .timestamp(positionTimestamp)
                    .positionInterpolated(interpolated)
                    .eventCertainty(certainty)
                    .speedOverGround(sog)
                    .courseOverGround(cog)
                    .trueHeading(hdg)
                    .latitude(position.getLatitude())
                    .longitude(position.getLongitude())
                .getEvent();

        addPreviousTrackingPoints(event, track);

        statisticsService.incAnalysisStatistics(this.getClass().getSimpleName(), "Events raised");

        return event;
    }

}
