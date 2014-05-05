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

import com.google.common.collect.ImmutableList;
import com.rits.cloning.Cloner;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage5;
import dk.dma.ais.message.AisStaticCommon;
import dk.dma.ais.message.IVesselPositionMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;
import net.jcip.annotations.NotThreadSafe;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingLong;

/**
 * The Track class contains the consolidated information known about a given target - likely as the result
 * of several received AIS messages.
 *
 * Track supports dynamic storage of track properties via the getProperty()/setProperty() methods, but
 * also contains some business logic and knowledge of what it stores; e.g. via the getMmsi(), and get/setPosition()
 * methods.
 *
 * Information which is available directly in AisPackets is stored and retrieved from these. Other information,
 * such as cell-id's, event booking-keeping, etc. is set dynamically as custom properties.
 *
 */
@NotThreadSafe
public final class Track implements Cloneable {

    public static final String CELL_ID = "cellId";
    public static final String SAFETY_ZONE = "safetyZone";
    public static final String EXTENT = "extent";

    private final int mmsi;

    private final Map<String, Object> properties = new HashMap<>(3);

    private static final Comparator<TrackingReport> byTimestamp = comparingLong(TrackingReport::getTimestamp);
    private static final Supplier<TreeSet<TrackingReport>> treeSetSupplier = () -> new TreeSet<TrackingReport>(byTimestamp);
    private boolean positionReportPurgeEnable = true;

    public final static int MAX_AGE_POSITION_REPORTS_MINUTES = 10;
    private TreeSet<TrackingReport> trackingReports = treeSetSupplier.get();

    /** The last received AIS packet of type 5 */
    private AisPacket lastStaticReport;

    /** Cached value of ship type from lastStaticReport - for faster reads */
    private Integer shipType;

    /** Cached value of ship name from lastStaticReport - for faster reads */
    private String shipName;

    /** Cached value of ship callsign from lastStaticReport - for faster reads */
    private String callsign;

    /** Cached value of IMO no. lastStaticReport - for faster reads */
    private Integer imo;

    /** Cached value of dimension A from lastStaticReport - for faster reads */
    private Integer shipDimensionBow;

    /** Cached value of dimension B from lastStaticReport - for faster reads */
    private Integer shipDimensionStern;

    /** Cached value of dimension C from lastStaticReport - for faster reads */
    private Integer shipDimensionPort;

    /** Cached value of dimension D from lastStaticReport - for faster reads */
    private Integer shipDimensionStarboard;

    private long timeOfLastUpdate = 0L;

    /**
     * Create a new track with the given MMSI no.
     * @param mmsi the MMSI no. assigned to this track.
     */
    public Track(int mmsi) {
        this.mmsi = mmsi;
    }

    /*
     * Update this track with a new AisPacket. The MMSI no. inside the packet must match this Track's MMSI no.
     * and in order to maintain low insertion-cost only packets newer than the previously received are accepted.
     *
     * This update will be treated as an update received from a real AIS source and the packet will be stored for
     * a period of time to support replay.
     *
     * @param p
     */
    public void update(AisPacket p) {
        checkAisPacket(p);
        AisMessage msg = p.tryGetAisMessage();
        if (msg instanceof AisStaticCommon) {
            addStaticReport(p);
        }
        if (msg instanceof IVesselPositionMessage) {
            addTrackingReport(new AisTrackingReport(p));
        }
    }

    /**
     * Update this track with a new AisPacket. The MMSI no. inside the packet must match this Track's MMSI no.
     * and in order to maintain low insertion-cost only packets newer than the previously received are accepted.
     *
     * This update is treated as an interpolated (artifical, non-real) update.
     *
     * @param m
     */
    public void update(long timestamp, AisMessage m) {
        if (m instanceof IVesselPositionMessage) {
            IVesselPositionMessage pm = (IVesselPositionMessage) m;
            update(timestamp, pm.getValidPosition(), (float) (pm.getCog() / 10.0), (float) (pm.getSog() / 10.0), pm.getTrueHeading());
        }
    }

    /**
     * Update this track with interpolated or predicted information (as opposed to information
     * received from an AIS receiver or basestation).
     */
    public void update(long timestamp, Position position, float cog, float sog, float hdg) {
        InterpolatedTrackingReport trackingReport = new InterpolatedTrackingReport(timestamp, position, cog, sog, hdg);
        addTrackingReport(trackingReport);

        //trackingReports.add(trackingReport);
        //timeOfLastUpdate = timestamp;
        //purgeTrackingReports(MAX_AGE_POSITION_REPORTS_MINUTES);
    }

    /**
     * Get a custom property previously set on this track.
     * @param propertyName
     * @return
     */
    public Object getProperty(String propertyName) {
        return properties.get(propertyName);
    }

    /**
     * Attach/set a custom property on this track.
     *
     * @param propertyName
     * @param propertyValue
     */
    public void setProperty(String propertyName, Object propertyValue) {
        properties.put(propertyName, propertyValue);
    }

    /**
     * Remove a custom property previously set on this track.
     *
     * @param propertyName
     */
    public void removeProperty(String propertyName) {
        properties.remove(propertyName);
    }

    /**
     * Get the MMSI no. of this track
     *
     * @return
     */
    public int getMmsi() {
        return mmsi;
    }

    /**
     * Get the timestamp of the last update of any type.
     *
     * @return
     */
    public long getTimeOfLastUpdate() {
        return timeOfLastUpdate;
    }

    /**
     * Get the timestamp of the last position report.
     *
     * @return
     */
    public long getTimeOfLastPositionReport() {
        long t = 0L;
        try {
            t = trackingReports.last().getTimestamp();
        } catch (NoSuchElementException e) {
        }
        return t;
    }

    /** Return the last received static report (if any) */
    public AisPacket getLastStaticReport() {
        return lastStaticReport;
    }

    /**
     * Enable or disable purging of old position reports from the track.
     * @param positionReportPurgeEnable
     */
    public void setPositionReportPurgeEnable(boolean positionReportPurgeEnable) {
        this.positionReportPurgeEnable = positionReportPurgeEnable;
    }

    /**
     * Update the track with a new static report
     */
    private void addStaticReport(AisPacket p) {
        AisStaticCommon msg = (AisStaticCommon) p.tryGetAisMessage();
        lastStaticReport = p;
        timeOfLastUpdate = p.getBestTimestamp();
        callsign = msg.getCallsign();
        shipType = msg.getShipType();
        shipName = msg.getName();
        shipDimensionBow = msg.getDimBow();
        shipDimensionStern = msg.getDimStern();
        shipDimensionPort = msg.getDimPort();
        shipDimensionStarboard = msg.getDimStarboard();
        if (msg instanceof AisMessage5) {
            imo = (int) ((AisMessage5) msg).getImo();
        }
    }

    /**
     * Update the track with a new trackingReport
     */
    private void addTrackingReport(TrackingReport trackingReport) {
        trackingReports.add(trackingReport);
        timeOfLastUpdate = trackingReport.getTimestamp();
        purgeTrackingReports(MAX_AGE_POSITION_REPORTS_MINUTES);
    }

    /**
     * Get the oldest reported position report kept.
     * @return
     */
    private TrackingReport getOldestTrackingReport() {
        TrackingReport oldestTrackingReport = null;
        try {
            oldestTrackingReport = trackingReports.first();
        } catch(NoSuchElementException e)  {
        }
        return oldestTrackingReport;
    }

    /**
     * Get the most recently reported position report.
     * @return
     */
    public TrackingReport getNewestTrackingReport() {
        TrackingReport mostRecentTrackingReport = null;
        try {
            mostRecentTrackingReport = trackingReports.last();
        } catch(NoSuchElementException e)  {
        }
        return mostRecentTrackingReport;
    }

    /**
     * Get a trail of historic position reports. The oldest will be a maximum of MAX_AGE_POSITION_REPORTS_MINUTES
     * minutes older than the newest (if position report purging is enabled; otherwise there is no limit).
     * @return
     */
    public List<TrackingReport> getTrackingReports() {
        purgeTrackingReports(MAX_AGE_POSITION_REPORTS_MINUTES);
        return ImmutableList.copyOf(trackingReports);
    }

    /**
     * Purge position reports older than the given amount of minutes, compared to the most
     * recent stored position report.
     * @return
     */
    private void purgeTrackingReports(int maxAgeMinutes) {
        if (positionReportPurgeEnable) {
            TrackingReport newestTrackingReport = getNewestTrackingReport();
            if (newestTrackingReport != null) {
                long now = newestTrackingReport.getTimestamp();
                long oldestKept = now - maxAgeMinutes * 60 * 1000;

                TrackingReport oldestTrackingReport = getOldestTrackingReport();
                if (oldestTrackingReport != null && oldestTrackingReport.getTimestamp() < oldestKept) {
                    trackingReports = trackingReports
                            .stream()
                            .filter(p -> p.getTimestamp() >= oldestKept)
                            .collect(Collectors.toCollection(treeSetSupplier));
                }
            }
        }
    }

    /**
     * Predict this track's position forward to time atTime.
     * @param atTime timestamp in milliseconds since the Epoch
     */
    public void predict(long atTime) {
        long now = getTimeOfLastPositionReport();

        if (atTime > now) {
            TrackingReport trackingReport = getNewestTrackingReport();
            if (trackingReport != null) {
                Position currentPosition = trackingReport.getPosition();
                float cog = trackingReport.getCourseOverGround();
                float sog = trackingReport.getSpeedOverGround();
                float hdg = trackingReport.getTrueHeading();

                long deltaMillis = atTime - now;
                float deltaSeconds = deltaMillis / 1000;
                float deltaMinutes = deltaSeconds / 60;
                float deltaHours = deltaMinutes / 60;
                float distanceNauticalMiles = sog * deltaHours;
                float distanceMeters = distanceNauticalMiles * 1852;
                Position predictedPosition = CoordinateSystem.CARTESIAN.pointOnBearing(currentPosition, distanceMeters, cog);

                addTrackingReport(new InterpolatedTrackingReport(atTime, predictedPosition, cog, sog, hdg /* TODO should be PredictedTrackingReport */));
            } else {
                throw new IllegalStateException("No enough data to predict future position.");
            }
        } else {
            throw new IllegalArgumentException("atTime should be ahead of track's last known position.");
        }
    }

    @Override
    public Track clone() {
        return new Cloner().deepClone(this);
    }

    private void checkAisPacket(AisPacket p) {
        AisMessage msg = p.tryGetAisMessage();
        if (msg == null) {
            throw new IllegalArgumentException("Cannot extract AisMessage from AisPacket: " + p.toString());
        }
        if (msg.getUserId() != mmsi) {
            throw new IllegalArgumentException("This track only accepts updates for MMSI " + mmsi + ", not " + msg.getUserId() + ".");
        }
    }

    /** Convenience method to return track's last reported ship type or null */
    public Integer getShipType() {
        return shipType;
    }

    /** Convenience method to return track's last reported ship name or null */
    public String getShipName() {
        return shipName;
    }

    /** Convenience method to return track's last reported callsign or null */
    public String getCallsign() {
        return callsign;
    }

    /** Convenience method to return track's last reported IMO no. or null */
    public Integer getIMO() {
        return imo;
    }

    /** Convenience method to return track's last reported vessel length or null */
    public Integer getVesselLength() {
        return shipDimensionBow == null || shipDimensionPort == null ? null : shipDimensionBow + shipDimensionStern;
    }

    /** Convenience method to return track's last reported vessel beam or null */
    public Integer getVesselBeam() {
        return shipDimensionBow == null || shipDimensionPort == null ? null : shipDimensionPort + shipDimensionStarboard;
    }

    public Integer getShipDimensionBow() {
        return shipDimensionBow;
    }

    public Integer getShipDimensionStern() {
        return shipDimensionStern;
    }

    public Integer getShipDimensionPort() {
        return shipDimensionPort;
    }

    public Integer getShipDimensionStarboard() {
        return shipDimensionStarboard;
    }

    /** Convenience method to return track's last reported position or null */
    public Position getPosition() {
        return nullsafeGetFromNewestTrackingReport(tp -> tp.getPosition());
    }

    /** Convenience method to return track's last reported SOG or null */
    public Float getSpeedOverGround() {
        return nullsafeGetFromNewestTrackingReport(tp -> tp.getSpeedOverGround());
    }

    /** Convenience method to return track's last reported COG or null */
    public Float getCourseOverGround() {
        return nullsafeGetFromNewestTrackingReport(tp -> tp.getCourseOverGround());
    }

    /** Convenience method to return track's last reported heading or null */
    public Float getTrueHeading() {
        return nullsafeGetFromNewestTrackingReport(tp -> tp.getTrueHeading());
    }

    private <T> T nullsafeGetFromNewestTrackingReport(Function<? super TrackingReport, T> getter) {
        T value = null;
        TrackingReport tp = getNewestTrackingReport();
        if (tp != null) {
            try {
                value = getter.apply(tp);
            } catch (NullPointerException e) {
            }
        }
        return value;
    }

}
