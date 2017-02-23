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

import dk.dma.ais.abnormal.analyzer.behaviour.BehaviourManager;
import dk.dma.ais.abnormal.analyzer.behaviour.BehaviourManagerImpl;
import dk.dma.ais.abnormal.analyzer.behaviour.EventCertainty;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.domain.CloseEncounterEvent;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.TrackingPoint;
import dk.dma.ais.abnormal.event.db.domain.builders.TrackingPointBuilder;
import dk.dma.ais.tracker.eventEmittingTracker.EventEmittingTracker;
import dk.dma.ais.tracker.eventEmittingTracker.EventEmittingTrackerImpl;
import dk.dma.ais.tracker.eventEmittingTracker.InterpolatedTrackingReport;
import dk.dma.ais.tracker.eventEmittingTracker.Track;
import dk.dma.ais.tracker.eventEmittingTracker.TrackingReport;
import dk.dma.commons.util.DateTimeUtil;
import dk.dma.enav.model.geometry.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;

/**
 * An Analysis is a class which is known to the ais-ab-analyzer application and possesses certain public
 * methods which can be called to analyze and detect events.
 *
 * The Analysis class is an abstract class which all analyses must inherit from.
 *
 * The Analysis class provides basic methods to its subclasses, so they can reuse the code to raise and
 * lower events.
 *
 * @author Thomas Borg Salling <tbsalling@tbsalling.dk>
 *
 */
public abstract class Analysis {

    private static final Logger LOG = LoggerFactory.getLogger(Analysis.class);

    private final EventRepository eventRepository;
    private final EventEmittingTracker trackingService;
    private final BehaviourManager behaviourManager;

    protected final static DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final String analysisName;

    protected Analysis(EventRepository eventRepository, EventEmittingTracker trackingService, BehaviourManager behaviourManager) {
        this.eventRepository = eventRepository;
        this.trackingService = trackingService;
        this.behaviourManager = behaviourManager;
        this.trackPredictionTimeMax = -1;
        this.analysisName = getClass().getSimpleName();
        init(behaviourManager);
    }

    private void init(BehaviourManager behaviourManager) {
        if (behaviourManager != null) {
            this.behaviourManager.registerSubscriber(this);
        }
    }

    @Override
    public String toString() {
        return "Analysis{" +
                "analysisName='" + analysisName + '\'' +
                ", trackPredictionTimeMax=" + trackPredictionTimeMax +
                '}';
    }

    protected final BehaviourManager getBehaviourManager() {
        return behaviourManager;
    }

    /**
     * The analysis will only start to receive trackEvents from the trackingService once this start() method
     * has been called.
     */
    public void start() {
        LOG.info(this.getClass().getSimpleName() + " starts to listen for tracking events.");
        trackingService.registerSubscriber(this);
    }

    /** Return the name of this analysis */
    public final String getAnalysisName() {
        return analysisName;
    }

    /**
     * This abstract method is intended to be implemented by subclasses, so that they can build
     * and return the proper Event entity when a new event is raised.
     *
     * @param primaryTrack the primary track for which the event is detected.
     * @param otherTracks other tracks involved in or related to the event.
     * @return the event.
     */
    protected abstract Event buildEvent(Track primaryTrack, Track... otherTracks);

    /**
     * If an event of the given type and involving the given track has already been raised, then lower it.
     * @param track
     */
    protected void lowerExistingAbnormalEventIfExists(Class<? extends Event> eventClass, Track track) {
        Integer mmsi = track.getMmsi();
        Event ongoingEvent = eventRepository.findOngoingEventByVessel(mmsi, eventClass);
        if (ongoingEvent != null) {
            LocalDateTime timestamp = track.getTimeOfLastUpdate();
            ongoingEvent.setState(Event.State.PAST);
            ongoingEvent.setEndTime(timestamp);
            eventRepository.save(ongoingEvent);
        }
    }

    /**
     * Raise a new event of type eventClass for the given primaryTrack. If such an event has already been raised then
     * maintain it and add the tracks newest behaviour to it.
     *
     * @param eventClass
     * @param primaryTrack
     * @param otherTracks
     */
    protected void raiseOrMaintainAbnormalEvent(Class<? extends Event> eventClass, Track primaryTrack, Track... otherTracks) {
        Integer mmsi = primaryTrack.getMmsi();
        Event event = eventRepository.findOngoingEventByVessel(mmsi, eventClass);

        if (event != null) {
            LocalDateTime positionTimestamp = primaryTrack.getTimeOfLastPositionReport();
            Position position = primaryTrack.getPosition();
            Float cog = primaryTrack.getCourseOverGround();
            Float sog = primaryTrack.getSpeedOverGround();
            Float hdg = primaryTrack.getTrueHeading();
            boolean interpolated = primaryTrack.getNewestTrackingReport() instanceof InterpolatedTrackingReport;

            TrackingPoint.EventCertainty certainty = TrackingPoint.EventCertainty.UNDEFINED;
            if (behaviourManager != null) {
                EventCertainty eventCertainty = getBehaviourManager().getEventCertaintyAtCurrentPosition(eventClass, primaryTrack);
                if (eventCertainty != null) {
                    certainty = TrackingPoint.EventCertainty.create(eventCertainty.getCertainty());
                }
            }

            addTrackingPoint(event, mmsi, positionTimestamp, position, cog, sog, hdg, interpolated, certainty);
        } else {
            event = buildEvent(primaryTrack, otherTracks);
        }

        eventRepository.save(event);
    }

    /**
     * Add a tracking point to an event and a target.
     */
    protected static void addTrackingPoint(Event event, int mmsi, LocalDateTime positionTimestamp, Position position, Float cog, Float sog, Float hdg, Boolean interpolated, TrackingPoint.EventCertainty eventCertainty) {
        event.getBehaviour(mmsi).addTrackingPoint(
                TrackingPointBuilder.TrackingPoint()
                        .timestamp(positionTimestamp)
                        .positionInterpolated(interpolated)
                        .eventCertainty(eventCertainty)
                        .speedOverGround(sog)
                        .courseOverGround(cog)
                        .trueHeading(hdg)
                        .latitude(position.getLatitude())
                        .longitude(position.getLongitude())
                        .getTrackingPoint()
        );
    }

    /**
     * Add the most recent track points (except THE most recent one) to the track history.
     * @param event
     * @param track
     */
    protected void addPreviousTrackingPoints(Event event, Track track) {
        List<TrackingReport> trackingReports = track.getTrackingReports();

        Iterator<TrackingReport> positionReportIterator = trackingReports.iterator();

        while (positionReportIterator.hasNext()) {
            TrackingReport trackingReport = positionReportIterator.next();

            if (trackingReport.getTimestamp().isBefore(track.getTimeOfLastPositionReport()) /* Do not add the last one - duplicate */) {
                String eventCertaintyKey = BehaviourManagerImpl.getEventCertaintyKey(event.getClass());
                EventCertainty eventCertaintyTmp = (EventCertainty) trackingReport.getProperty(eventCertaintyKey);
                TrackingPoint.EventCertainty eventCertainty = eventCertaintyTmp == null ? TrackingPoint.EventCertainty.UNDEFINED : TrackingPoint.EventCertainty.create(eventCertaintyTmp.getCertainty());

                if (event instanceof CloseEncounterEvent || eventCertainty != TrackingPoint.EventCertainty.UNDEFINED) /* Small hack to store one TP per grid cell for some event types TODO */ {
                    addTrackingPoint(event, track.getMmsi(),
                            trackingReport.getTimestamp(),
                            trackingReport.getPosition(),
                            trackingReport.getCourseOverGround(),
                            trackingReport.getSpeedOverGround(),
                            trackingReport.getTrueHeading(),
                            trackingReport instanceof InterpolatedTrackingReport,
                            eventCertainty);
                }
            }
        }
    }

    /** Maximum time a track may be predicted and still be included in analysis (in minutes) */
    private int trackPredictionTimeMax;

    protected void setTrackPredictionTimeMax(int trackPredictionTimeMax) {
        this.trackPredictionTimeMax = trackPredictionTimeMax;
    }

    /** Return true if there are no AisTrackingReports or if the newest AisTrackingReport is too old. */
    protected boolean isLastAisTrackingReportTooOld(Track track, LocalDateTime now) {
        if (trackPredictionTimeMax == -1) {
            return false;
        }
        final LocalDateTime timeOfLastAisTrackingReport = track.getTimeOfLastAisTrackingReport();
        return timeOfLastAisTrackingReport == null || timeOfLastAisTrackingReport.until(now, ChronoUnit.MINUTES) > trackPredictionTimeMax;
    }

    protected EventRepository getEventRepository() {
        return eventRepository;
    }

    protected EventEmittingTrackerImpl getTrackingService() {
        return trackingService instanceof EventEmittingTrackerImpl ? (EventEmittingTrackerImpl) trackingService : null;
    }

    protected final static long toEpochMillis(LocalDateTime t) {
        return LocalDateTime.MIN.equals(t) ? Instant.EPOCH.toEpochMilli() : DateTimeUtil.LOCALDATETIME_UTC_TO_MILLIS.apply(t);
    }

}
