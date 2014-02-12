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

package dk.dma.ais.abnormal.tracker;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import dk.dma.ais.abnormal.application.statistics.AppStatisticsService;
import dk.dma.ais.abnormal.tracker.events.CellChangedEvent;
import dk.dma.ais.abnormal.tracker.events.PositionChangedEvent;
import dk.dma.ais.abnormal.tracker.events.TimeEvent;
import dk.dma.ais.abnormal.tracker.events.TrackStaleEvent;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.AisPosition;
import dk.dma.ais.message.AisStaticCommon;
import dk.dma.ais.message.AisTargetType;
import dk.dma.ais.message.IPositionMessage;
import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.model.geometry.grid.Cell;
import dk.dma.enav.model.geometry.grid.Grid;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TrackingServiceImpl is a thread-safe implementation of the TrackingService interface.
 */
@ThreadSafe
public class TrackingServiceImpl implements TrackingService {

    static final Logger LOG = LoggerFactory.getLogger(TrackingServiceImpl.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private EventBus eventBus = new EventBus();
    private final AppStatisticsService statisticsService;

    @GuardedBy("tracksLock")
    final HashMap<Integer, Track> tracks = new HashMap<>(256);
    private final Lock tracksLock = new ReentrantLock();

    final Grid grid;

    static final int TRACK_STALE_SECS = 1800;  // 30 mins
    static final int TRACK_INTERPOLATION_REQUIRED_SECS = 30;  // 30 secs
    static final int INTERPOLATION_TIME_STEP_MILLIS = 10000;

    /**
     * A simple counter which is used to reduce CPU load by only performing more complicated
     * calculations when this markTrigger has reached certain values.
     */
    private int markTrigger;

    /**
     * The last hour-of-the-day when a timestamp message was log to the LOG.
     */
    private int markLastHourLogged;

    /**
     * The time since Epoch when the most recent TimeEvent was posted to the EventBus.
     */
    private long lastTimeEventMillis = 0;

    /**
     * The approximate no. of milliseconds between each TimeEvent.
     */
    static final int TIME_EVENT_PERIOD_MILLIS = 60000;

    @Inject
    public TrackingServiceImpl(Grid grid, AppStatisticsService statisticsService) {
        eventBus.register(this);
        this.grid = grid;
        this.statisticsService = statisticsService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(Date timestamp, AisMessage aisMessage) {
        final AisTargetType targetType = aisMessage.getTargetType();

        int mmsi = aisMessage.getUserId();

        if (targetType == AisTargetType.A || targetType == AisTargetType.B) {
            Track track = getOrCreateTrack(mmsi);
            Long currentUpdate = timestamp.getTime();

            // Manage timestamps
            Long lastAnyUpdate = (Long) track.getProperty(Track.TIMESTAMP_ANY_UPDATE);
            if (lastAnyUpdate == null) {
                lastAnyUpdate = 0L;
            }

            Long lastPositionUpdate = track.getPositionReportTimestamp();
            if (lastPositionUpdate == null) {
                lastPositionUpdate = 0L;
            }

            // Rebirth track if stale
            if (isTrackStale(lastAnyUpdate, lastPositionUpdate, currentUpdate)) {
                removeTrack(mmsi);
                track = getOrCreateTrack(mmsi);
                lastAnyUpdate = 0L;
                lastPositionUpdate = 0L;
            }

            // Perform track updates
            if (currentUpdate >= lastAnyUpdate) {
                if (aisMessage instanceof IVesselPositionMessage) {
                    IVesselPositionMessage positionMessage = (IVesselPositionMessage) aisMessage;
                    final boolean aisMessageHasValidPosition = positionMessage.getPos().getGeoLocation() != null;
                    if (aisMessageHasValidPosition) {
                        if (isInterpolationRequired(lastPositionUpdate, currentUpdate)) {
                            interpolatePositions(track, currentUpdate, positionMessage);
                        } else {
                            updatePosition(track, currentUpdate, positionMessage);
                        }
                    }
                }

                updateTimestamp(track, currentUpdate);

                if (aisMessage instanceof IVesselPositionMessage) {
                    IVesselPositionMessage vesselPositionMessage = (IVesselPositionMessage) aisMessage;
                    updateSpeedOverGround(track, vesselPositionMessage);
                    updateCourseOverGround(track, vesselPositionMessage);
                }

                if (aisMessage instanceof AisStaticCommon) {
                    AisStaticCommon staticCommon = (AisStaticCommon) aisMessage;
                    updateVesselName(track, staticCommon);
                    updateCallsign(track, staticCommon);
                    updateShipType(track, staticCommon);
                    updateVesselLength(track, staticCommon);
                }

                if (aisMessage instanceof AisMessage5) {
                    AisMessage5 aisMessage5 = (AisMessage5) aisMessage;
                    updateImo(track, aisMessage5);
                }
            } else {
                statisticsService.incOutOfSequenceMessages();
                Long timeDelta = lastAnyUpdate - currentUpdate;
                LOG.debug("Message of type " + aisMessage.getMsgId() + " ignored because it is out of sequence (delayed) by " + timeDelta + " msecs (mmsi " + mmsi + ")");
            }
        }

        mark(timestamp);
    }

    private void interpolatePositions(Track track, Long currentUpdate, IPositionMessage positionMessage) {
        TrackingReport trackingReport = track.getPositionReport();
        Position p1 = trackingReport.getPosition();
        Float cog = trackingReport.getCourseOverGround();
        Float sog = trackingReport.getSpeedOverGround();
        long t1 = trackingReport.getTimestamp();

        Position p2 = positionMessage.getPos().getGeoLocation();
        long t2 = currentUpdate;

        LOG.debug("p1=" + p1 + ", p2=" + p2);

        Map<Long, Position> interpolatedPositions = calculateInterpolatedPositions(p1, t1, p2, t2);

        Set<Map.Entry<Long, Position>> interpolatedPositionEntries = interpolatedPositions.entrySet();
        Iterator<Map.Entry<Long, Position>> iterator = interpolatedPositionEntries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Position> positionEntry = iterator.next();
            long positionTimestamp = positionEntry.getKey();
            Position p = positionEntry.getValue();
            updatePosition(track, positionTimestamp, p, cog, sog, iterator.hasNext());
        }

        LOG.debug("Used " + interpolatedPositionEntries.size() + " interpolation points for track " + track.getMmsi());
    }

    static final Map<Long, Position> calculateInterpolatedPositions(Position p1, long t1, Position p2, long t2) {
        TreeMap<Long, Position> interpolatedPositions = new TreeMap<>();

        if (t2 < t1) {
            LOG.error("Cannot interpolate backwards: " + t1 + " " + t2);
            return interpolatedPositions;
        }

        long t = t1 + INTERPOLATION_TIME_STEP_MILLIS;
        for (; t < t2; t += INTERPOLATION_TIME_STEP_MILLIS) {
            Position interpolatedPosition = linearInterpolation(p1, t1, p2, t2, t);
            interpolatedPositions.put(t, interpolatedPosition);
        }

        interpolatedPositions.put(t2, p2);

        return interpolatedPositions;
    }

    static final double linearInterpolation(double x1, long t1, double x2, long t2, long t) {
        return x1 + (x2 - x1) / (t2 - t1) * (t - t1);
    }

    static final Position linearInterpolation(Position p1, long t1, Position p2, long t2, long t) {
        double interpolatedLatitude = linearInterpolation(p1.getLatitude(), t1, p2.getLatitude(), t2, t);
        double interpolatedLongitude = linearInterpolation(p1.getLongitude(), t1, p2.getLongitude(), t2, t);
        return Position.create(interpolatedLatitude, interpolatedLongitude);
    }

    static boolean isTrackStale(long lastAnyUpdate, long lastPositionUpdate, long currentUpdate) {
        long lastUpdate = Math.max(lastAnyUpdate, lastPositionUpdate);
        boolean trackStale = lastUpdate > 0L && currentUpdate - lastUpdate >= TRACK_STALE_SECS * 1000L;
        if (trackStale) {
            LOG.debug("Track is stale (" + currentUpdate + ", " + lastUpdate + ")");
        }
        return trackStale;
    }

    private static boolean isInterpolationRequired(long lastPositionUpdate, long currentPositionUpdate) {
        boolean interpolationRequired = lastPositionUpdate > 0L && currentPositionUpdate - lastPositionUpdate >= TRACK_INTERPOLATION_REQUIRED_SECS * 1000L;
        if (interpolationRequired) {
            LOG.debug("Interpolation is required (" + currentPositionUpdate + ", " + lastPositionUpdate + ")");
        }
        return interpolationRequired;
    }

    private void updateTimestamp(Track track, Long timestampMillis) {
        track.setProperty(Track.TIMESTAMP_ANY_UPDATE, timestampMillis);
    }

    private void updateVesselName(Track track, AisStaticCommon aisMessage) {
        String name = aisMessage.getName();
        track.setProperty(Track.SHIP_NAME, name);
    }

    private void updateImo(Track track, AisMessage5 aisMessage) {
        Integer imo = (int) aisMessage.getImo();
        track.setProperty(Track.IMO, imo);
    }

    private void updateCallsign(Track track, AisStaticCommon aisMessage) {
        String callsign = aisMessage.getCallsign();
        track.setProperty(Track.CALLSIGN, callsign);
    }

    private void updateCourseOverGround(Track track, IVesselPositionMessage aisMessage) {
        IVesselPositionMessage positionMessage = (IVesselPositionMessage) aisMessage;
        int cog = positionMessage.getCog();
        track.setProperty(Track.COURSE_OVER_GROUND, new Float(cog / 10.000000000000));
    }

    private void updateSpeedOverGround(Track track, IVesselPositionMessage aisMessage) {
        IVesselPositionMessage positionMessage = (IVesselPositionMessage) aisMessage;
        int sog = positionMessage.getSog();
        track.setProperty(Track.SPEED_OVER_GROUND, new Float(sog / 10.000000000000));
    }

    private void updatePosition(Track track, long positionTimestamp, IVesselPositionMessage aisMessage) {
        AisPosition aisPosition = aisMessage.getPos();
        float cog = (float) (aisMessage.getCog() / 10.0);
        float sog = (float) (aisMessage.getSog() / 10.0);
        Position position = aisPosition.getGeoLocation();

        updatePosition(track, positionTimestamp, position, cog, sog, false);
    }

    private void updatePosition(Track track, long positionTimestamp, Position position, float cog, float sog, boolean positionIsInterpolated) {
        track.setProperty(Track.TIMESTAMP_ANY_UPDATE, Long.valueOf(positionTimestamp));

        performUpdatePosition(track, positionTimestamp, position, cog, sog, positionIsInterpolated);
        performUpdateCellId(track, position);
    }

    private void performUpdatePosition(Track track, long positionTimestamp, Position position, float cog, float sog, boolean positionIsInterpolated) {
        Position oldPosition = track.getPositionReportPosition();

        TrackingReport trackingReport = TrackingReport.create(positionTimestamp, position, cog, sog, positionIsInterpolated);
        track.updatePosition(trackingReport);

        eventBus.post(new PositionChangedEvent(track, oldPosition));
    }

    private void performUpdateCellId(Track track, Position position) {
        Long oldCellId = (Long) track.getProperty(Track.CELL_ID);
        if (position != null) {
            Cell cell = grid.getCell(position);
            Long newCellId = cell.getCellId();
            track.setProperty(Track.CELL_ID, newCellId);
            if ((oldCellId == null && newCellId != null) ||
                    (oldCellId != null && newCellId == null) ||
                    (oldCellId != null && newCellId != null && !oldCellId.equals(newCellId))) {
                eventBus.post(new CellChangedEvent(track, oldCellId));
            }
        } else {
            track.removeProperty(Track.CELL_ID);
            if (oldCellId != null) {
                eventBus.post(new CellChangedEvent(track, oldCellId));
            }
            LOG.warn("Message type contained no valid position (mmsi " + track.getMmsi() + ")");
        }
    }

    private void updateShipType(Track track, AisStaticCommon aisMessage) {
        Integer shipType = aisMessage.getShipType();
        track.setProperty(Track.SHIP_TYPE, shipType);
    }

    private void updateVesselLength(Track track, AisStaticCommon aisMessage) {
        Integer vesselLength = aisMessage.getDimBow() + aisMessage.getDimStern();
        track.setProperty(Track.VESSEL_LENGTH, vesselLength);
    }

    private void removeTrack(int mmsi) {
        tracksLock.lock();
        try {
            Track track = tracks.get(mmsi);
            tracks.remove(mmsi);
            eventBus.post(new TrackStaleEvent(track));
        } finally {
            tracksLock.unlock();
        }
    }

    private Track getOrCreateTrack(int mmsi) {
        Track track;
        tracksLock.lock();
        try {
            track = tracks.get(mmsi);
            if (track == null) {
                track = new Track(mmsi);
                tracks.put(mmsi, track);
            }
        } finally {
            tracksLock.unlock();
        }
        return track;
    }

    /**
     * {@inheritDoc}
     */
    @Subscribe
    @SuppressWarnings("unused")
    public void listen(DeadEvent event) {
        LOG.trace("No subscribers were interested in this event: " + event.getEvent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerSubscriber(Object subscriber) {
        eventBus.register(subscriber);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Track> cloneTracks() {
        ImmutableSet.Builder<Track> setBuilder = new ImmutableSet.Builder<>();

        tracksLock.lock();
        try {
            tracks.values().forEach(
                t -> { setBuilder.add(t.clone());
            });
        } finally {
            tracksLock.unlock();
        }

        return setBuilder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getNumberOfTracks() {
        int n;

        tracksLock.lock();
        try {
            n = tracks.size();
        } finally {
            tracksLock.unlock();
        }

        return n;
    }

    /**
     * Occasionally check how far we have come in the data stream, and fire a TimeEvent event to the EventBus
     * approximately every TIME_EVENT_PERIOD_MILLIS msecs. The mechanism is based on timestamps from the data
     * stream and therefore quite unprecise. Significant slack must be expected.
     *
     * @param timestamp timestamp of current message.
     */
    private void mark(Date timestamp) {
        // Put marks in the log
        if ((markTrigger & 0xffff) == 0) {
            Calendar markCalendar = Calendar.getInstance();
            markCalendar.setTime(timestamp);
            int t = markCalendar.get(Calendar.HOUR_OF_DAY);
            if (t != markLastHourLogged) {
                markLastHourLogged = t;
                LOG.info("Now processing stream at time " + timestamp);
            }
        }

        // Fire TimeEvents
        //if ((markTrigger & 0xff) == 0) {
        long timestampMillis = timestamp.getTime();
        long millisSinceLastTimeEvent = timestampMillis - lastTimeEventMillis;

        if (millisSinceLastTimeEvent >= TIME_EVENT_PERIOD_MILLIS) {
            TimeEvent timeEvent = new TimeEvent(timestampMillis, lastTimeEventMillis == 0 ? -1 : (int) millisSinceLastTimeEvent);
            eventBus.post(timeEvent);
            lastTimeEventMillis = timestampMillis;
            if (LOG.isDebugEnabled()) {
                LOG.debug("TimeEvent emitted at time " + timeEvent.getTimestamp() + " msecs (" + timeEvent.getMillisSinceLastMark() + " msecs since last).");
            }
        }
        //}

        markTrigger++;
    }

}
