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
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.TrackingPoint;
import dk.dma.ais.abnormal.event.db.domain.builders.TrackingPointBuilder;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.TrackingReport;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.enav.model.geometry.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
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
 */
public abstract class Analysis {

    private static final Logger LOG = LoggerFactory.getLogger(Analysis.class);

    private final EventRepository eventRepository;
    private final TrackingService trackingService;
    private final BehaviourManager behaviourManager;

    protected Analysis(EventRepository eventRepository, TrackingService trackingService, BehaviourManager behaviourManager) {
        this.eventRepository = eventRepository;
        this.trackingService = trackingService;
        this.behaviourManager = behaviourManager;
        if (behaviourManager != null) {
            this.behaviourManager.registerSubscriber(this);
        }
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

    /**
     * This abstract method is intended to be implemented by subclasses, so that they can build
     * and return the proper Event entity when a new event is raised.
     *
     * @param track
     * @return
     */
    protected abstract Event buildEvent(Track track);

    /**
     * If an event of the given type and involving the given track has already been raised, then lower it.
     * @param track
     */
    protected void lowerExistingAbnormalEventIfExists(Class<? extends Event> eventClass, Track track) {
        Integer mmsi = track.getMmsi();
        Event ongoingEvent = eventRepository.findOngoingEventByVessel(mmsi, eventClass);
        if (ongoingEvent != null) {
            Date timestamp = new Date((Long) track.getProperty(Track.TIMESTAMP_ANY_UPDATE));
            ongoingEvent.setState(Event.State.PAST);
            ongoingEvent.setEndTime(timestamp);
            eventRepository.save(ongoingEvent);
        }
    }

    /**
     * Raise a new event of type eventClass for the given track. If such an event has already been raised then
     * maintain it and add the tracks newest behaviour to it.
     *
     * @param eventClass
     * @param track
     */
    protected void raiseOrMaintainAbnormalEvent(Class<? extends Event> eventClass, Track track) {
        Integer mmsi = track.getMmsi();
        Event event = eventRepository.findOngoingEventByVessel(mmsi, eventClass);

        if (event != null) {
            Date positionTimestamp = new Date(track.getPositionReportTimestamp());
            Position position = track.getPositionReportPosition();
            Float cog = (Float) track.getProperty(Track.COURSE_OVER_GROUND);
            Float sog = (Float) track.getProperty(Track.SPEED_OVER_GROUND);
            Boolean interpolated = track.getPositionReportIsInterpolated();

            TrackingPoint.EventCertainty certainty = TrackingPoint.EventCertainty.UNDEFINED;
            if (behaviourManager != null) {
                EventCertainty eventCertainty = getBehaviourManager().getEventCertaintyAtCurrentPosition(eventClass, track);
                if (eventCertainty != null) {
                    certainty = TrackingPoint.EventCertainty.create(eventCertainty.getCertainty());
                }
            }

            addTrackingPoint(event, positionTimestamp, position, cog, sog, interpolated, certainty);
        } else {
            event = buildEvent(track);
        }

        eventRepository.save(event);
    }

    /**
     * Add a tracking point to an event.
     */
    protected static void addTrackingPoint(Event event, Date positionTimestamp, Position position, Float cog, Float sog, Boolean interpolated, TrackingPoint.EventCertainty eventCertainty) {
        event.getBehaviour().addTrackingPoint(
                TrackingPointBuilder.TrackingPoint()
                        .timestamp(positionTimestamp)
                        .positionInterpolated(interpolated)
                        .eventCertainty(eventCertainty)
                        .speedOverGround(sog)
                        .courseOverGround(cog)
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
            // Do not add the last one - duplicate
            if (positionReportIterator.hasNext()) {
                TrackingPoint.EventCertainty certainty = null;

                String eventCertaintyKey = BehaviourManagerImpl.getEventCertaintyKey(event.getClass());
                EventCertainty eventCertaintyTmp = (EventCertainty) trackingReport.getProperty(eventCertaintyKey);
                TrackingPoint.EventCertainty eventCertainty = eventCertaintyTmp == null ? TrackingPoint.EventCertainty.UNDEFINED : TrackingPoint.EventCertainty.create(eventCertaintyTmp.getCertainty());

                if (eventCertainty != TrackingPoint.EventCertainty.UNDEFINED) /* Small hack to store one TP per grid cell */ {
                    addTrackingPoint(event,
                            new Date(trackingReport.getTimestamp()),
                            trackingReport.getPosition(),
                            trackingReport.getCourseOverGround(),
                            trackingReport.getSpeedOverGround(),
                            trackingReport.isInterpolated(),
                            eventCertainty);
                }
            }
        }
    }

    protected EventRepository getEventRepository() {
        return eventRepository;
    }

    protected TrackingService getTrackingService() {
        return trackingService;
    }
}
