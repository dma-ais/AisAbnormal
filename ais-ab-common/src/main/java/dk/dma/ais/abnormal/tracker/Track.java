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
    public static final String SPEED_OVER_GROUND = "sog";
    public static final String COURSE_OVER_GROUND = "cog";
    public static final String CELL_ID = "cellId";
    public static final String SHIP_TYPE = "vesselType";
    public static final String VESSEL_LENGTH = "vesselLength";
    public static final String SHIP_NAME = "vesselName";
    public static final String IMO = "imo";
    public static final String CALLSIGN = "callsign";

    private final int mmsi;
    private final Map<String, Object> properties = new HashMap<>(10);

    private static final Comparator<PositionReport> byTimestamp = comparingLong(PositionReport::getTimestamp);
    private static final Supplier<TreeSet<PositionReport>> treeSetSupplier = () -> new TreeSet<PositionReport>(byTimestamp);
    private boolean positionReportPurgeEnable = true;

    final static int MAX_AGE_POSITION_REPORTS_MINUTES = 10;
    TreeSet<PositionReport> positionReports = treeSetSupplier.get();

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
     * Update the track with a new positionReport
     */
    public void updatePosition(PositionReport positionReport) {
        positionReports.add(positionReport);
        purgePositionReports(MAX_AGE_POSITION_REPORTS_MINUTES);
    }

    /**
     * Get the oldest reported position report kept.
     * @return
     */
    private PositionReport getOldestPositionReport() {
        PositionReport oldestPositionReport = null;
        try {
            oldestPositionReport = positionReports.first();
        } catch(NoSuchElementException e)  {
        }
        return oldestPositionReport;
    }

    /**
     * Get the most recently reported position report.
     * @return
     */
    public PositionReport getPositionReport() {
        PositionReport mostRecentPositionReport = null;
        try {
            mostRecentPositionReport = positionReports.last();
        } catch(NoSuchElementException e)  {
        }
        return mostRecentPositionReport;
    }

    /**
     * Get the position of the most recent position report (if any).
     * This is a null-safe convenience method to replace getPositionReport().getPosition().
     * @return
     */
    public Position getPositionReportPosition() {
        Position position = null;
        PositionReport positionReport = getPositionReport();
        if (positionReport != null) {
            position = positionReport.getPosition();
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
        PositionReport positionReport = getPositionReport();
        if (positionReport != null) {
            isInterpolated = positionReport.isInterpolated();
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
        PositionReport positionReport = getPositionReport();
        if (positionReport != null) {
            timestamp = positionReport.getTimestamp();
        }
        return timestamp;
    }

    /**
     * Get a trail of historic position reports. The oldest will be a maximum of MAX_AGE_POSITION_REPORTS_MINUTES
     * minutes older than the newest (if position report purging is enabled; otherwise there is no limit).
     * @return
     */
    public List<PositionReport> getPositionReports() {
        purgePositionReports(MAX_AGE_POSITION_REPORTS_MINUTES);
        return ImmutableList.copyOf(positionReports);
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

            PositionReport oldestPositionReport = getOldestPositionReport();
            if (oldestPositionReport != null && oldestPositionReport.getTimestamp() < oldestKept) {
                positionReports = positionReports
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
