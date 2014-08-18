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
import dk.dma.ais.abnormal.event.db.domain.TrackingPoint;
import dk.dma.ais.abnormal.event.db.domain.builders.SuddenSpeedChangeEventBuilder;
import dk.dma.ais.abnormal.event.db.domain.builders.TrackingPointBuilder;
import dk.dma.ais.abnormal.tracker.InterpolatedTrackingReport;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.Tracker;
import dk.dma.ais.abnormal.tracker.TrackingReport;
import dk.dma.ais.abnormal.tracker.events.PositionChangedEvent;
import dk.dma.ais.abnormal.tracker.events.TrackStaleEvent;
import dk.dma.ais.abnormal.util.Categorizer;
import dk.dma.enav.model.geometry.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

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
 * This analysis manages events representing sudden decrease in speed over ground.
 *
 * Such a decrease is an indicator for groundings.
 *
 * A sudden decreasing speed is defined as a speed change going from more
 * than 7 knots to less than 1 knot in less than 30 seconds. To avoid false positives,
 * the speed must stay below 1 knot for a least 1 minute before an event is raised.
 *
 * This analysis is not based on previous observations (statistic data).
 */

public class SuddenSpeedChangeAnalysis extends Analysis {
    private static final Logger LOG = LoggerFactory.getLogger(SuddenSpeedChangeAnalysis.class);

    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final AppStatisticsService statisticsService;

    /** Track must come from SOG above this value to cause sudden speed change event */
    static final float SPEED_HIGH_MARK = 7.0f;

    /** Track must drop to SOG below this value to cause sudden speed change event */
    static final float SPEED_LOW_MARK = 1.0f;

    /** Track must drop from above SPEED_HIGH_MARK to below SPEED_LOW_MARK in less than this amount of seconds to cause sudden speed change event */
    static final int SUDDEN_TIME_SECS = 30;

    /** No. of secs to sustain low speed before raising sudden speed change event */
    static final long SUSTAIN_LOW_SPEED_SECS = 60;

    static final float MAX_VALID_SPEED = (float) 102.2;

    private TreeSet<Integer> tracksWithSuddenSpeedDecrease = new TreeSet<>();

    private final String analysisName;

    @Inject
    public SuddenSpeedChangeAnalysis(AppStatisticsService statisticsService, Tracker trackingService, EventRepository eventRepository) {
        super(eventRepository, trackingService, null);
        this.statisticsService = statisticsService;
        this.analysisName = this.getClass().getSimpleName();
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onSpeedOverGroundUpdated(PositionChangedEvent trackEvent) {
        Track track = trackEvent.getTrack();

        /* Do not perform analysis if reported speed is invalid */
        final Float sogBoxed = track.getSpeedOverGround();
        if (sogBoxed == null) {
            return;
        }
        final float sog = sogBoxed.floatValue();
        if (sog >= 102.3 /* ~1024 invalid sog */) {
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
    }

    @AllowConcurrentEvents
    @Subscribe
    public void onTrackStale(TrackStaleEvent trackEvent) {
        final int mmsi = trackEvent.getTrack().getMmsi();
        if (tracksWithSuddenSpeedDecrease.contains(mmsi)) {
            LOG.debug("MMSI " + mmsi + " is now stale. Removed from observation list.");
            tracksWithSuddenSpeedDecrease.remove(mmsi);
        }
    }

    private void performAnalysis(Track track) {
        final int mmsi = track.getMmsi();
        final Float speedOverGround = track.getSpeedOverGround();

        if (speedOverGround != null && speedOverGround <= SPEED_LOW_MARK) {
            if (!tracksWithSuddenSpeedDecrease.contains(mmsi) && isSuddenSpeedDecrease(track)) {
                LOG.debug("MMSI " + mmsi + " experienced sudden speed decrease. Added to observation list.");
                tracksWithSuddenSpeedDecrease.add(mmsi);
            } else if (tracksWithSuddenSpeedDecrease.contains(mmsi) && isSustainedSpeedDecrease(track)) {
                LOG.debug("MMSI " + mmsi + " experienced sustained speed decrease. Event raised.");
                raiseAndLowerSuddenSpeedChangeEvent(track);
                tracksWithSuddenSpeedDecrease.remove(mmsi);
            }
        } else {
            if (tracksWithSuddenSpeedDecrease.contains(mmsi)) {
                LOG.debug("MMSI " + mmsi + " speed above low mark. Removed from observation list.");
                tracksWithSuddenSpeedDecrease.remove(mmsi);
            }
        }
    }

    /**
     * Evaluate whether this track has kept its speed below SPEED_LOW_MARK
     * for a sustained period of at least SUSTAIN_LOW_SPEED_SECS seconds.
     *
     * @param track
     * @return
     */
    private boolean isSustainedSpeedDecrease(Track track) {
        if (track.getSpeedOverGround() > SPEED_LOW_MARK) {
            throw new IllegalArgumentException("track.speedOverGround > SPEED_LOW_MARK");
        }

        Optional<Float> maxSog = track.getTrackingReports()
                .stream()
                .filter(tr -> tr.getTimestamp() >= track.getTimeOfLastPositionReport() - SUSTAIN_LOW_SPEED_SECS * 1000)
                .map(tr -> Float.valueOf(tr.getSpeedOverGround()))
                .max(Comparator.<Float>naturalOrder());

        return maxSog.isPresent() ? maxSog.get() <= SPEED_LOW_MARK : false;
    }

    /**
     * Evaluate whether this track has experienced a "sudden speed decrease" in relation
     * to the most recent speed report.
     *
     * A sudden speed decrease is a drop in SOG from above SPEED_HIGH_MARK to below
     * SPEED_LOW_MARK in less than SUDDEN_TIME_SECS seconds.
     *
     * @param track
     * @return
     */
    private boolean isSuddenSpeedDecrease(Track track) {
        if (track.getSpeedOverGround() > SPEED_LOW_MARK) {
            return false;
        }

        long t1 = timeOfLastTrackingReportAboveHighMark(track.getTrackingReports());
        long t2 = track.getTimeOfLastPositionReport();

        return t1 >= 0 && (t2 - t1) <= SUDDEN_TIME_SECS*1000;
    }

    private long timeOfLastTrackingReportAboveHighMark(List<TrackingReport> trackingReports) {
        Optional<Long> t = trackingReports
            .stream()
            .filter(tr -> tr.getSpeedOverGround() <= MAX_VALID_SPEED)
            .filter(tr -> tr.getSpeedOverGround() >= SPEED_HIGH_MARK)
            .map(tr -> tr.getTimestamp())
            .max(Comparator.<Long>naturalOrder());

        return t.isPresent() ? t.get() : -1;
    }

    private long timeOfFirstTrackingReportBelowLowMark(List<TrackingReport> trackingReports, long t1) {
        Optional<Long> t = trackingReports
            .stream()
            .filter(tr -> tr.getTimestamp() >= t1)
            .filter(tr -> tr.getSpeedOverGround() <= SPEED_LOW_MARK)
            .map(tr -> Long.valueOf(tr.getTimestamp()))
            .min(Comparator.<Long>naturalOrder());

        return t.isPresent() ? t.get() : -1;
    }

    @Override
    protected Event buildEvent(Track track, Track... otherTracks) {
        if (otherTracks != null && otherTracks.length > 0) {
            throw new IllegalArgumentException("otherTracks not supported.");
        }

        // Static
        Integer mmsi = track.getMmsi();
        Integer imo = track.getIMO();
        String callsign = track.getCallsign();
        String name = nameOrMmsi(track.getShipName(), track.getMmsi());
        Integer shipType = track.getShipType();
        Integer shipDimensionToBow = track.getShipDimensionBow();
        Integer shipDimensionToStern = track.getShipDimensionStern();
        Integer shipDimensionToPort = track.getShipDimensionPort();
        Integer shipDimensionToStarboard = track.getShipDimensionStarboard();

        String shipTypeAsString = "unknown type";
        short shipTypeCategory = Categorizer.mapShipTypeToCategory(shipType);
        if (shipType != null) {
            shipTypeAsString = Categorizer.mapShipTypeCategoryToString(shipTypeCategory);
            shipTypeAsString = shipTypeAsString.substring(0, 1).toUpperCase() + shipTypeAsString.substring(1);
        }

        List<TrackingReport> trackingReports = track.getTrackingReports();
        long t1 = timeOfLastTrackingReportAboveHighMark(trackingReports);
        long t2 = timeOfFirstTrackingReportBelowLowMark(trackingReports, t1);
        // long t3 = track.getTimeOfLastPositionReport();
        float deltaSecs = (float) ((t2 - t1) / 1000.0);

        TrackingReport trackingReportAtT1 = track.getTrackingReportAt(t1);
        Position position1 = trackingReportAtT1.getPosition();
        Float cog1 = trackingReportAtT1.getCourseOverGround();
        Float sog1 = trackingReportAtT1.getSpeedOverGround();
        Float hdg1 = trackingReportAtT1.getTrueHeading();
        Boolean interpolated1 = trackingReportAtT1 instanceof InterpolatedTrackingReport;

        TrackingReport trackingReportAtT2 = track.getTrackingReportAt(t2);
        Position position2 = trackingReportAtT2.getPosition();
        Float cog2 = trackingReportAtT2.getCourseOverGround();
        Float sog2 = trackingReportAtT2.getSpeedOverGround();
        Float hdg2 = trackingReportAtT2.getTrueHeading();
        Boolean interpolated2 = trackingReportAtT2 instanceof InterpolatedTrackingReport;

        String title = "Sudden speed change";
        String description = String.format("Sudden speed change of %s (%s) on position %s at %s: From %.1f kts to %.1f kts in %.1f secs.",
                name,
                shipTypeAsString,
                position1,
                DATE_FORMAT.format(t1),
                sog1, sog2, deltaSecs);

        LOG.info(description);

        Event event =
            SuddenSpeedChangeEventBuilder.SuddenSpeedChangeEvent()
                .title(title)
                .description(description)
                .state(Event.State.PAST)
                .startTime(new Date(t1))
                .endTime(new Date(t2))
                .behaviour()
                    .isPrimary(true)
                    .vessel()
                        .mmsi(mmsi)
                        .imo(imo)
                        .callsign(callsign)
                        .type(shipType)
                        .toBow(shipDimensionToBow)
                        .toStern(shipDimensionToStern)
                        .toPort(shipDimensionToPort)
                        .toStarboard(shipDimensionToStarboard)
                        .name(name)
                    .trackingPoint()
                        .timestamp(new Date(t1))
                        .positionInterpolated(interpolated1)
                        .eventCertainty(TrackingPoint.EventCertainty.RAISED)
                        .speedOverGround(sog1)
                        .courseOverGround(cog1)
                        .trueHeading(hdg1)
                        .latitude(position1.getLatitude())
                        .longitude(position1.getLongitude())
            .getEvent();

        event.getBehaviour(mmsi).addTrackingPoint(
            TrackingPointBuilder.TrackingPoint()
                .timestamp(new Date(t2))
                .positionInterpolated(interpolated2)
                .eventCertainty(TrackingPoint.EventCertainty.RAISED)
                .speedOverGround(sog2)
                .courseOverGround(cog2)
                .trueHeading(hdg2)
                .latitude(position2.getLatitude())
                .longitude(position2.getLongitude())
            .getTrackingPoint());

        addPreviousTrackingPoints(event, track);

        statisticsService.incAnalysisStatistics(analysisName, "Events raised");

        return event;
    }

    private void raiseAndLowerSuddenSpeedChangeEvent(Track track) {
        raiseOrMaintainAbnormalEvent(SuddenSpeedChangeEvent.class, track);
        lowerExistingAbnormalEventIfExists(SuddenSpeedChangeEvent.class, track);
    }

}
