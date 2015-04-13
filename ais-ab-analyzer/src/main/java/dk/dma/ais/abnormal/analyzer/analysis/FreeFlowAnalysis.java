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

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.dma.ais.abnormal.analyzer.AppStatisticsService;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.tracker.eventEmittingTracker.EventEmittingTracker;
import dk.dma.ais.tracker.eventEmittingTracker.Track;
import dk.dma.enav.model.geometry.BoundingBox;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Ellipse;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.util.CoordinateConverter;
import dk.dma.enav.util.geometry.Point;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_FREEFLOW_BBOX;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_FREEFLOW_CSVFILE;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_FREEFLOW_DCOG;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_FREEFLOW_MIN_REPORTING_PERIOD_MINUTES;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_FREEFLOW_PREDICTIONTIME_MAX;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_FREEFLOW_RUN_PERIOD;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_FREEFLOW_XB;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_FREEFLOW_XL;
import static dk.dma.ais.abnormal.util.AisDataHelper.trimAisString;
import static dk.dma.ais.abnormal.util.TrackPredicates.isCargoVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isTankerVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isVeryLongVessel;
import static dk.dma.enav.safety.SafetyZones.createEllipse;
import static dk.dma.enav.util.compass.CompassUtils.absoluteDirectionalDifference;
import static dk.dma.enav.util.compass.CompassUtils.compass2cartesian;
import static java.lang.System.nanoTime;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * This analysis analyses "free flow" of vessels in given areas.
 *
 * An area is said to have free flow, if no large vessels have similar vessels sailing in the
 * appoximate same direction close by. "Close by" is defined as an ellipse around the vessel.
 *
 * @author Thomas Borg Salling <tbsalling@tbsalling.dk>
 */
@NotThreadSafe
@Singleton
public class FreeFlowAnalysis extends PeriodicAnalysis {
    private static final Logger LOG = LoggerFactory.getLogger(FreeFlowAnalysis.class);

    private final AppStatisticsService statisticsService;
    private BoundingBox areaToBeAnalysed = null;

    /** Major axis of ellipse is xL times vessel's length-over-all */
    private final int xL;

    /** Minor axis of ellipse is xB times vessel's beam */
    private final int xB;

    /** Vessels' courses over ground must be within this no. of degrees to be paired in analysis */
    private final float dCog;

    /** A vessel pair can only be reported this often */
    private final int minReportingIntervalMillis;

    /** Name of the CSV file to which freeflow events will be appended */
    private final String csvFileName;

    @Inject
    public FreeFlowAnalysis(Configuration configuration, AppStatisticsService statisticsService, EventEmittingTracker trackingService, EventRepository eventRepository) {
        super(eventRepository, trackingService, null);
        this.statisticsService = statisticsService;

        this.xL = configuration.getInt(CONFKEY_ANALYSIS_FREEFLOW_XL, 8);
        this.xB = configuration.getInt(CONFKEY_ANALYSIS_FREEFLOW_XB, 8);
        this.dCog = configuration.getFloat(CONFKEY_ANALYSIS_FREEFLOW_DCOG, 15f);
        this.minReportingIntervalMillis = configuration.getInt(CONFKEY_ANALYSIS_FREEFLOW_MIN_REPORTING_PERIOD_MINUTES, 60) * 60 * 1000;

        String csvFileNameTmp = configuration.getString(CONFKEY_ANALYSIS_FREEFLOW_CSVFILE, null);
        if (csvFileNameTmp == null || isBlank(csvFileNameTmp)) {
            this.csvFileName = null;
            LOG.warn("Writing of free flow events to CSV file is disabled");
        } else {
            this.csvFileName = csvFileNameTmp.trim();
            LOG.info("Free flow events are appended to CSV file: " + this.csvFileName);
        }

        List<Object> bboxConfig = configuration.getList(CONFKEY_ANALYSIS_FREEFLOW_BBOX);
        if (bboxConfig != null) {
            final double n = Double.valueOf(bboxConfig.get(0).toString());
            final double e = Double.valueOf(bboxConfig.get(1).toString());
            final double s = Double.valueOf(bboxConfig.get(2).toString());
            final double w = Double.valueOf(bboxConfig.get(3).toString());
            this.areaToBeAnalysed = BoundingBox.create(Position.create(n, e), Position.create(s, w), CoordinateSystem.CARTESIAN);
        }

        setTrackPredictionTimeMax(configuration.getInteger(CONFKEY_ANALYSIS_FREEFLOW_PREDICTIONTIME_MAX, -1));
        setAnalysisPeriodMillis(configuration.getInt(CONFKEY_ANALYSIS_FREEFLOW_RUN_PERIOD, 30000) * 1000);

        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    @Override
    public String toString() {
        return "FreeFlowAnalysis{" +
                "areaToBeAnalysed=" + areaToBeAnalysed +
                ", xL=" + xL +
                ", xB=" + xB +
                ", dCog=" + dCog +
                ", minReportingIntervalMillis=" + minReportingIntervalMillis +
                "} " + super.toString();
    }

    protected void performAnalysis() {
        LOG.debug("Starting " + getAnalysisName() + " " + getCurrentRunTime());
        final long systemTimeNanosBeforeAnalysis = nanoTime();

        Collection<Track> allTracks = getTrackingService().getTracks();

        List<Track> allRelevantTracksPredictedToNow = allTracks
                .stream()
                .filter(this::isVesselTypeToBeAnalysed)
                .filter(this::isInsideAreaToBeAnalysed)
                .filter(this::isMinimumSpeedOverGround)
                .map(this::predictToCurrentTime)
                .filter(this::isPredictedToCurrentTime)
                .collect(Collectors.toList());

        analyseFreeFlow(allRelevantTracksPredictedToNow);

        statisticsService.incAnalysisStatistics(getAnalysisName(), "Analyses performed");
        final long systemTimeNanosAfterAnalysis = nanoTime();
        LOG.debug(getAnalysisName() + " of " + allTracks.size() + " tracks completed in " + (systemTimeNanosAfterAnalysis - systemTimeNanosBeforeAnalysis) + " nsecs.");
    }

    private void analyseFreeFlow(Collection<Track> tracks) {
        LOG.debug("Performing analysis of " + tracks.size() + " tracks");
        final long t0 = nanoTime();

        tracks
            .stream()
            .forEach(t -> analyseFreeFlow(tracks, t));

        final long t1 = nanoTime();
        LOG.debug("Analysis performed in " + (t1-t0)/1000 + " msecs");
    }

    private void analyseFreeFlow(Collection<Track> allTracks, Track t0) {
        LOG.debug("Performing free flow analysis of " + t0.getMmsi());

        final float cog0 = t0.getCourseOverGround();
        final Position pc0 = centerOfVessel(t0.getPosition(), t0.getTrueHeading(), t0.getShipDimensionStern(), t0.getShipDimensionBow(), t0.getShipDimensionPort(), t0.getShipDimensionStarboard());

        Set<Track> tracksSailingSameDirection = allTracks
            .stream()
            .filter(t -> t.getMmsi() != t0.getMmsi())
            .filter(t -> !isLastAisTrackingReportTooOld(t, t.getTimeOfLastPositionReport()))
            .filter(t -> absoluteDirectionalDifference(cog0, t.getCourseOverGround()) < dCog)
            .collect(Collectors.toSet());

        if (tracksSailingSameDirection.size() > 0) {
            Ellipse ellipse = createEllipse(
                pc0,
                pc0,
                cog0,
                t0.getVesselLength(),
                t0.getVesselBeam(),
                t0.getShipDimensionStern(),
                t0.getShipDimensionStarboard(),
                xL,
                xB,
                1
            );

            LOG.debug("ellipse: " + ellipse);

            List<Track> tracksSailingSameDirectionAndContainedInEllipse = tracksSailingSameDirection
                .stream()
                .filter(t -> ellipse.contains(centerOfVessel(t.getPosition(), t.getTrueHeading(), t.getShipDimensionStern(), t.getShipDimensionBow(), t.getShipDimensionPort(), t.getShipDimensionStarboard())))
                .collect(Collectors.toList());

            if (tracksSailingSameDirectionAndContainedInEllipse.size() > 0) {
                LOG.debug("There are " + tracksSailingSameDirectionAndContainedInEllipse.size() + " tracks inside ellipse of " + t0.getMmsi() + " " + t0.getShipName());
                LOG.debug(new DateTime(t0.getTimeOfLastPositionReport()) + " " + "MMSI " + t0.getMmsi() + " " + t0.getShipName() + " " + t0.getShipType());
                List<FreeFlowData.TrackInsideEllipse> tracksInsideEllipse = Lists.newArrayList();
                for (Track t1 : tracksSailingSameDirectionAndContainedInEllipse) {
                    if (! reportedRecently(t0, t1, t0.getTimeOfLastPositionReport())) {
                        final Position pc1 = centerOfVessel(t1.getPosition(), t1.getTrueHeading(), t1.getShipDimensionStern(), t1.getShipDimensionBow(), t1.getShipDimensionPort(), t1.getShipDimensionStarboard());
                        try {
                            tracksInsideEllipse.add(new FreeFlowData.TrackInsideEllipse(t1.clone(), pc1));
                            markReported(t0, t1, t0.getTimeOfLastPositionReport());
                        } catch (CloneNotSupportedException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                }
                if (tracksInsideEllipse.size() > 0) {
                    try {
                        writeToCSVFile(new FreeFlowData(t0.clone(), pc0, tracksInsideEllipse));
                    } catch (CloneNotSupportedException e) {
                        LOG.error(e.getMessage(), e);
                    }
                } else {
                    LOG.debug("Nothing new to report.");
                }
            }
        }
    }

    private Map<String, Long> reported = new HashMap<>();

    private void markReported(Track t0, Track t1, long timestamp) {
        String key = String.valueOf(t0.getMmsi()) + "/" + String.valueOf(t1.getMmsi());
        reported.put(key, timestamp);
    }

    private boolean reportedRecently(Track t0, Track t1, long timestamp) {
        String key = String.valueOf(t0.getMmsi()) + "/" + String.valueOf(t1.getMmsi());
        Long lastReport = reported.get(key);
        return lastReport != null && timestamp-lastReport < minReportingIntervalMillis;
    }

    private boolean isVesselTypeToBeAnalysed(Track track) {
        return isVeryLongVessel.test(track) && (isTankerVessel.test(track) || isCargoVessel.test(track));
    }

    private boolean isInsideAreaToBeAnalysed(Track track) {
        Position position = track.getPosition();
        return position != null && areaToBeAnalysed != null && areaToBeAnalysed.contains(position);
    }

    private boolean isMinimumSpeedOverGround(Track track) {
        Float speedOverGround = track.getSpeedOverGround();
        return speedOverGround == null || speedOverGround >= 1f;
    }

    private boolean isPredictedToCurrentTime(Track track) {
        return track.getTimeOfLastPositionReport() == getCurrentRunTime();
    }

    private Track predictToCurrentTime(Track track) {
        // TODO clone track
        if (track.getTimeOfLastPositionReport() < getCurrentRunTime()) {
            try {
                track.predict(getCurrentRunTime());
            } catch (IllegalStateException e) {
                // java.lang.IllegalStateException: No enough data to predict future position.
            }
        }
        return track;
    }

    /**
     * Given an AIS position, the vessel's heading and dimensions from position sensor to bow, stern, starboard, and port
     * compute the vessel's center point.
     *
     * @param aisPosition
     * @param hdg
     * @param dimStern
     * @param dimBow
     * @param dimPort
     * @param dimStarboard
     * @return
     */
    static Position centerOfVessel(Position aisPosition, float hdg, int dimStern, int dimBow, int dimPort, int dimStarboard) {
        // Compute direction of half axis alpha
        final double thetaDeg = compass2cartesian(hdg);

        // Transform latitude/longitude to cartesian coordinates
        final Position geodeticReference = aisPosition;
        final CoordinateConverter coordinateConverter = new CoordinateConverter(geodeticReference.getLongitude(), geodeticReference.getLatitude());
        final double trackLatitude = aisPosition.getLatitude();
        final double trackLongitude = aisPosition.getLongitude();
        final double x = coordinateConverter.lon2x(trackLongitude, trackLatitude);
        final double y = coordinateConverter.lat2y(trackLongitude, trackLatitude);

        // Cartesion point of AIS position
        final Point pAis = new Point(x, y);

        // Compute cartesian center of vessel
        final Point pc = new Point((dimBow + dimStern)/2 - dimStern, (dimPort+dimStarboard)/2 - dimStarboard);

        // Rotate to comply with hdg
        final Point pcr = pc.rotate(pAis, thetaDeg);

        // Convert back to geodesic coordinates
        return Position.create(coordinateConverter.y2Lat(pcr.getX(), pcr.getY()), coordinateConverter.x2Lon(pcr.getX(), pcr.getY()));
    }

    @Override
    protected Event buildEvent(Track primaryTrack, Track... otherTracks) {
      return null;
    }

    private ReentrantLock lock = new ReentrantLock();
    @GuardedBy("lock")
    private FileWriter fileWriter = null;
    @GuardedBy("lock")
    private CSVPrinter csvFilePrinter = null;
    DateTimeFormatter fmt = DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss");

    private void writeToCSVFile(FreeFlowData freeFlowData) {
        if (csvFileName == null)
            return;

        final File csvFile = new File(csvFileName);
        final boolean fileExists = csvFile.exists() == true || csvFile.length() > 0;

        lock.lock();

        if (!fileExists) {
            try {
                if (csvFilePrinter != null) csvFilePrinter.close();
                if (fileWriter != null) fileWriter.close();
            } catch (IOException e) {
                LOG.warn(e.getMessage(), e);
            }
            csvFilePrinter = null;
            fileWriter = null;
        }

        try {
            if (fileWriter == null) {
                try {
                    fileWriter = new FileWriter(csvFile, true);
                    if (csvFilePrinter == null) {
                        try {
                            csvFilePrinter = new CSVPrinter(fileWriter, CSVFormat.RFC4180.withCommentMarker('#'));
                        } catch (IOException e) {
                            csvFilePrinter = null;
                            LOG.error(e.getMessage(), e);
                            LOG.error("Failed to write line to CSV file: " + freeFlowData);
                            return;
                        }
                    }
                } catch (IOException e) {
                    fileWriter = null;
                    LOG.error(e.getMessage(), e);
                    LOG.error("Failed to write line to CSV file: " + freeFlowData);
                    return;
                }
            }

            if (!fileExists) {
                LOG.info("Created new CSV file: " + csvFile.getAbsolutePath());
                csvFilePrinter.printComment("Generated by AIS Abnormal Behaviour Analyzer");
                csvFilePrinter.printComment("File created: " + fmt.print(new Date().getTime()));
                csvFilePrinter.printRecord("TIMESTAMP (GMT)", "MMSI1", "NAME1", "TP1", "LOA1", "BM1", "COG1", "HDG1", "SOG1", "LAT1", "LON1", "MMSI2", "NAME2", "TP2", "LOA2", "BM2", "COG2", "HDG2", "SOG2", "LAT2", "LON2", "BRG", "DST");
            }

            final Track t0 = freeFlowData.getTrackSnapshot();
            final Position p0 = freeFlowData.getTrackCenterPosition();

            List<FreeFlowData.TrackInsideEllipse> tracks = freeFlowData.getTracksInsideEllipse();
            for (FreeFlowData.TrackInsideEllipse track : tracks) {
                final Track t1 = track.getTrackSnapshot();
                final Position p1 = track.getTrackCenterPosition();
                final int d = (int) p0.distanceTo(p1, CoordinateSystem.CARTESIAN);
                final int b = (int) p0.rhumbLineBearingTo(p1);

                List csvRecord = new ArrayList<>();
                csvRecord.add(String.format(Locale.ENGLISH, "%s", fmt.print(t0.getTimeOfLastPositionReport())));
                csvRecord.add(String.format(Locale.ENGLISH, "%d", t0.getMmsi()));
                csvRecord.add(String.format(Locale.ENGLISH, "%s", trimAisString(t0.getShipName()).replace(',', ' ')));
                csvRecord.add(String.format(Locale.ENGLISH, "%d", t0.getShipType()));
                csvRecord.add(String.format(Locale.ENGLISH, "%d", t0.getVesselLength()));
                csvRecord.add(String.format(Locale.ENGLISH, "%d", t0.getVesselBeam()));
                csvRecord.add(String.format(Locale.ENGLISH, "%.0f", t0.getCourseOverGround()));
                csvRecord.add(String.format(Locale.ENGLISH, "%.0f", t0.getTrueHeading()));
                csvRecord.add(String.format(Locale.ENGLISH, "%.0f", t0.getSpeedOverGround()));
                csvRecord.add(String.format(Locale.ENGLISH, "%.4f", p0.getLatitude()));
                csvRecord.add(String.format(Locale.ENGLISH, "%.4f", p0.getLongitude()));

                csvRecord.add(String.format(Locale.ENGLISH, "%d", t1.getMmsi()));
                csvRecord.add(String.format(Locale.ENGLISH, "%s", trimAisString(t1.getShipName()).replace(',', ' ')));
                csvRecord.add(String.format(Locale.ENGLISH, "%d", t1.getShipType()));
                csvRecord.add(String.format(Locale.ENGLISH, "%d", t1.getVesselLength()));
                csvRecord.add(String.format(Locale.ENGLISH, "%d", t1.getVesselBeam()));
                csvRecord.add(String.format(Locale.ENGLISH, "%.0f", t1.getCourseOverGround()));
                csvRecord.add(String.format(Locale.ENGLISH, "%.0f", t1.getTrueHeading()));
                csvRecord.add(String.format(Locale.ENGLISH, "%.0f", t1.getSpeedOverGround()));
                csvRecord.add(String.format(Locale.ENGLISH, "%.4f", p1.getLatitude()));
                csvRecord.add(String.format(Locale.ENGLISH, "%.4f", p1.getLongitude()));
                csvRecord.add(String.format(Locale.ENGLISH, "%d", b));
                csvRecord.add(String.format(Locale.ENGLISH, "%d", d));

                try {
                    csvFilePrinter.printRecord(csvRecord);
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                    LOG.error("Failed to write line to CSV file: " + freeFlowData);
                }
            }
            csvFilePrinter.flush();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            lock.unlock();
        }
    }

    public static class FreeFlowData {
        private final Track trackSnapshot;
        private final Position trackCenterPosition;

        private final List<TrackInsideEllipse> tracksInsideEllipse;

        public static class TrackInsideEllipse {
            private final Track trackSnapshot;
            private final Position trackCenterPosition;

            private TrackInsideEllipse(Track trackSnapshot, Position trackCenterPosition) {
                this.trackSnapshot = trackSnapshot;
                this.trackCenterPosition = trackCenterPosition;
            }

            public Track getTrackSnapshot() {
                return trackSnapshot;
            }

            public Position getTrackCenterPosition() {
                return trackCenterPosition;
            }

            @Override
            public String toString() {
                return "TrackInsideEllipse{" +
                        "trackSnapshot=" + trackSnapshot +
                        ", trackCenterPosition=" + trackCenterPosition +
                        '}';
            }
        }

        private FreeFlowData(Track trackSnapshot, Position trackCenterPosition, List<TrackInsideEllipse> tracksInsideEllipse) {
            this.trackSnapshot = trackSnapshot;
            this.trackCenterPosition = trackCenterPosition;
            this.tracksInsideEllipse = tracksInsideEllipse;
        }

        public Track getTrackSnapshot() {
            return trackSnapshot;
        }

        public Position getTrackCenterPosition() {
            return trackCenterPosition;
        }

        public List<TrackInsideEllipse> getTracksInsideEllipse() {
            return tracksInsideEllipse;
        }

        @Override
        public String toString() {
            return "FreeFlowData{" +
                    "trackSnapshot=" + trackSnapshot +
                    ", trackCenterPosition=" + trackCenterPosition +
                    ", tracksInsideEllipse=" + tracksInsideEllipse +
                    '}';
        }
    }
}
