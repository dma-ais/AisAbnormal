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
import dk.dma.enav.model.geometry.Position;
import net.jcip.annotations.NotThreadSafe;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeSet;
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
 */
@NotThreadSafe
public final class Track implements Cloneable {

    public static final String TIMESTAMP_ANY_UPDATE = "lastUpdate";
    public static final String CELL_ID = "cellId";
    public static final String SHIP_TYPE = "type";
    public static final String VESSEL_DIM_BOW = "dimbw";
    public static final String VESSEL_DIM_STERN = "dimsn";
    public static final String VESSEL_DIM_PORT = "dimpt";
    public static final String VESSEL_DIM_STARBOARD = "dimsb";
    public static final String VESSEL_LENGTH = "loa";
    public static final String VESSEL_BEAM = "beam";
    public static final String SHIP_NAME = "name";
    public static final String IMO = "imo";
    public static final String CALLSIGN = "callsign";
    public static final String SAFETY_ZONE = "safetyZone";
    public static final String EXTENT = "extent";

    private final int mmsi;
    private final Map<String, Object> properties = new HashMap<>(10);

    private static final Comparator<TrackingReport> byTimestamp = comparingLong(TrackingReport::getTimestamp);
    private static final Supplier<TreeSet<TrackingReport>> treeSetSupplier = () -> new TreeSet<TrackingReport>(byTimestamp);
    private boolean positionReportPurgeEnable = true;

    final static int MAX_AGE_POSITION_REPORTS_MINUTES = 10;
    TreeSet<TrackingReport> trackingReports = treeSetSupplier.get();

    /**
     * Create a new track with the given MMSI no.
     * @param mmsi
     */
    public Track(int mmsi) {
        this.mmsi = mmsi;
    }

    public int getMmsi() {
        return mmsi;
    }

    public Object getProperty(String propertyName) {
        return properties.get(propertyName);
    }

    public void setProperty(String propertyName, Object propertyValue) {
        properties.put(propertyName, propertyValue);
    }

    public void removeProperty(String propertyName) {
        properties.remove(propertyName);
    }

    /**
     * Enable or disable purging of old position reports from the track.
     * @param positionReportPurgeEnable
     */
    public void setPositionReportPurgeEnable(boolean positionReportPurgeEnable) {
        this.positionReportPurgeEnable = positionReportPurgeEnable;
    }

    /**
     * Update the track with a new trackingReport
     */
    public void updatePosition(TrackingReport trackingReport) {
        trackingReports.add(trackingReport);
        purgePositionReports(MAX_AGE_POSITION_REPORTS_MINUTES);
    }

    /**
     * Get the oldest reported position report kept.
     * @return
     */
    private TrackingReport getOldestPositionReport() {
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
    public TrackingReport getPositionReport() {
        TrackingReport mostRecentTrackingReport = null;
        try {
            mostRecentTrackingReport = trackingReports.last();
        } catch(NoSuchElementException e)  {
        }
        return mostRecentTrackingReport;
    }

    /**
     * Get the position of the most recently reported speed over ground (if any).
     * This is a null-safe convenience method to replace getPositionReport().getSpeedOverGround().
     * @return
     */
    public Float getCourseOverGround() {
        Float cog = null;
        TrackingReport trackingReport = getPositionReport();
        if (trackingReport != null) {
            cog = trackingReport.getCourseOverGround();
        }
        return cog;
    }

    /**
     * Get the position of the most recently reported speed over ground (if any).
     * This is a null-safe convenience method to replace getPositionReport().getSpeedOverGround().
     * @return
     */
    public Float getSpeedOverGround() {
        Float sog = null;
        TrackingReport trackingReport = getPositionReport();
        if (trackingReport != null) {
            sog = trackingReport.getSpeedOverGround();
        }
        return sog;
    }

    /**
     * Get the position of the most recent position report (if any).
     * This is a null-safe convenience method to replace getPositionReport().getPosition().
     * @return
     */
    public Position getPosition() {
        Position position = null;
        TrackingReport trackingReport = getPositionReport();
        if (trackingReport != null) {
            position = trackingReport.getPosition();
        }
        return position;
    }

    /**
     * Get the interpolation status of the most recent reported position (if any).
     * This is a null-safe convenience method to replace getPositionReport().isInterpolated().
     * @return
     */
    public Boolean getPositionReportIsInterpolated() {
        Boolean isInterpolated = null;
        TrackingReport trackingReport = getPositionReport();
        if (trackingReport != null) {
            isInterpolated = trackingReport.isInterpolated();
        }
        return isInterpolated;
    }

    /**
     * Get the timestamp of the most recent reported position (if any).
     * This is a null-safe convenience method to replace getPositionReport().getTimestamp().
     * @return
     */
    public Long getPositionReportTimestamp() {
        Long timestamp = null;
        TrackingReport trackingReport = getPositionReport();
        if (trackingReport != null) {
            timestamp = trackingReport.getTimestamp();
        }
        return timestamp;
    }

    /**
     * Get a trail of historic position reports. The oldest will be a maximum of MAX_AGE_POSITION_REPORTS_MINUTES
     * minutes older than the newest (if position report purging is enabled; otherwise there is no limit).
     * @return
     */
    public List<TrackingReport> getTrackingReports() {
        purgePositionReports(MAX_AGE_POSITION_REPORTS_MINUTES);
        return ImmutableList.copyOf(trackingReports);
    }

    /**
     * Purge position reports older than the given amount of minutes, compared to the most
     * recent stored position report.
     * @return
     */
    private void purgePositionReports(int maxAgeMinutes) {
        if (positionReportPurgeEnable) {
            long now = getPositionReport().getTimestamp();
            long oldestKept = now - maxAgeMinutes*60*1000;

            TrackingReport oldestTrackingReport = getOldestPositionReport();
            if (oldestTrackingReport != null && oldestTrackingReport.getTimestamp() < oldestKept) {
                trackingReports = trackingReports
                .stream()
                .filter(p -> p.getTimestamp() >= oldestKept)
                .collect(Collectors.toCollection(treeSetSupplier));
            }
        }
    }

    @Override
    public Track clone() {
        return new Cloner().deepClone(this);
    }

}
