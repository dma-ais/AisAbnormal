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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.dma.ais.abnormal.analyzer.AppStatisticsService;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.ais.abnormal.tracker.Tracker;
import dk.dma.enav.model.geometry.BoundingBox;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Ellipse;
import dk.dma.enav.model.geometry.Position;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_FREEFLOW_BBOX;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_FREEFLOW_DCOG;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_FREEFLOW_RUN_PERIOD;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_FREEFLOW_XB;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_ANALYSIS_FREEFLOW_XL;
import static dk.dma.ais.abnormal.util.TrackPredicates.isCargoVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isTankerVessel;
import static dk.dma.ais.abnormal.util.TrackPredicates.isVeryLongVessel;
import static dk.dma.enav.safety.SafetyZones.createEllipse;
import static dk.dma.enav.util.compass.CompassUtils.absoluteDirectionalDifference;
import static java.lang.System.nanoTime;

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
    private final String analysisName;
    private BoundingBox areaToBeAnalysed = null;
    private final int xL;
    private final int xB;
    private final float dCog;

    @Inject
    public FreeFlowAnalysis(Configuration configuration, AppStatisticsService statisticsService, Tracker trackingService, EventRepository eventRepository) {
        super(eventRepository, trackingService, null);
        this.statisticsService = statisticsService;
        this.analysisName = this.getClass().getSimpleName();

        this.xL = configuration.getInt(CONFKEY_ANALYSIS_FREEFLOW_XL, 8);
        this.xB = configuration.getInt(CONFKEY_ANALYSIS_FREEFLOW_XB, 8);
        this.dCog = configuration.getFloat(CONFKEY_ANALYSIS_FREEFLOW_DCOG, 15f);

        List<Object> bboxConfig = configuration.getList(CONFKEY_ANALYSIS_FREEFLOW_BBOX);
        if (bboxConfig != null) {
            final double n = Double.valueOf(bboxConfig.get(0).toString());
            final double e = Double.valueOf(bboxConfig.get(1).toString());
            final double s = Double.valueOf(bboxConfig.get(2).toString());
            final double w = Double.valueOf(bboxConfig.get(3).toString());
            this.areaToBeAnalysed = BoundingBox.create(Position.create(n, e), Position.create(s, w), CoordinateSystem.CARTESIAN);
        }

        setAnalysisPeriodMillis(configuration.getInt(CONFKEY_ANALYSIS_FREEFLOW_RUN_PERIOD, 30000) * 1000);

        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    @Override
    public String toString() {
        return "FreeFlowAnalysis{} " + super.toString();
    }

    protected void performAnalysis() {
        LOG.debug("Starting " + analysisName + " " + getCurrentRunTime());
        final long systemTimeNanosBeforeAnalysis = nanoTime();

        Collection<Track> allTracks = getTrackingService().getTracks();

        List<Track> allRelevantTracksPredictedToNow = allTracks
                .stream()
                .filter(this::isVesselTypeToBeAnalysed)
                .filter(this::isInsideAreaToBeAnalysed)
                .map(this::predictToCurrentTime)
                .filter(this::isPredictedToCurrentTime)
                .collect(Collectors.toList());

        analyseFreeFlow(allRelevantTracksPredictedToNow);

        statisticsService.incAnalysisStatistics(analysisName, "Analyses performed");
        final long systemTimeNanosAfterAnalysis = nanoTime();
        LOG.debug(analysisName + " of " + allTracks.size() + " tracks completed in " + (systemTimeNanosAfterAnalysis - systemTimeNanosBeforeAnalysis) + " nsecs.");
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
        final Position pos0 = t0.getPosition();

        Set<Track> tracksSailingSameDirection = allTracks
            .stream()
            .filter(t -> t.getMmsi() != t0.getMmsi())
            .filter(t -> absoluteDirectionalDifference(cog0, t.getCourseOverGround()) < dCog)
            .collect(Collectors.toSet());

        if (tracksSailingSameDirection.size() > 0) {
            Ellipse ellipse = createEllipse(
                    pos0,
                    pos0,
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
                    .filter(t -> ellipse.contains(t.getPosition()))
                    .collect(Collectors.toList());

            if (tracksSailingSameDirectionAndContainedInEllipse.size() > 0) {
                LOG.debug("There are " + tracksSailingSameDirectionAndContainedInEllipse.size() + " tracks inside ellipse of " + t0.getMmsi() + " " + t0.getShipName());
                LOG.debug(new DateTime(t0.getTimeOfLastPositionReport()) + " " + "MMSI " + t0.getMmsi() + " " + t0.getShipName() + " " + t0.getShipType());
                List<FreeFlowData.TrackInsideEllipse> trks = Lists.newArrayList();
                for (Track t1 : tracksSailingSameDirectionAndContainedInEllipse) {
                    final Position pos1 = t1.getPosition();
                    final int d = (int) pos0.distanceTo(pos1, CoordinateSystem.CARTESIAN);
                    final int b = (int) pos0.rhumbLineBearingTo(pos1);
                    trks.add(new FreeFlowData.TrackInsideEllipse(t1.getMmsi(), t1.getShipName(), t1.getShipType(), t1.getVesselLength(), b, d));
                    LOG.debug("- MMSI " + t1.getMmsi() + " " + t1.getShipName() + " " + t1.getShipType() + " " + d + " " + b);
                }
                lock.lock();
                try {
                    tmpData.add(new FreeFlowData(new DateTime(t0.getTimeOfLastPositionReport()), t0.getMmsi(), t0.getShipName(), t0.getShipType(), t0.getVesselLength(), trks));
                } finally {
                    lock.unlock();
                }
            }

            LOG.debug("---");
        }
    }

    private boolean isVesselTypeToBeAnalysed(Track track) {
        return isVeryLongVessel.test(track) && (isTankerVessel.test(track) || isCargoVessel.test(track));
    }

    private boolean isInsideAreaToBeAnalysed(Track track) {
        Position position = track.getPosition();
        return position != null && areaToBeAnalysed != null && areaToBeAnalysed.contains(position);
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

    @Override
    protected Event buildEvent(Track primaryTrack, Track... otherTracks) {
      return null;
    }

    // --- Below: Temporary code - will be replaced when business logic is determined


    public List<FreeFlowData> getTmpData() {
        lock.lock();
        try {
            ImmutableList<FreeFlowData> copy = ImmutableList.copyOf(tmpData);
            tmpData.clear();
            return copy;
        } finally {
            lock.unlock();
        }
    }

    ReentrantLock lock = new ReentrantLock();

    @GuardedBy(value = "lock")
    public List<FreeFlowData> tmpData = new ArrayList<>();

    public static class FreeFlowData {
        private final DateTime timestamp;
        private final int mmsi;
        private final String name;
        private final int type;
        private final int loa;
        private final List<TrackInsideEllipse> tracks;

        public static class TrackInsideEllipse {
            private final int mmsi;
            private final String name;
            private final int type;
            private final int loa;
            private final int bearing;
            private final int distance;

            private TrackInsideEllipse(int mmsi, String name, int type, int loa, int bearing, int distance) {
                this.mmsi = mmsi;
                this.name = name;
                this.type = type;
                this.loa = loa;
                this.bearing = bearing;
                this.distance = distance;
            }

            public int getMmsi() {
                return mmsi;
            }

            public String getName() {
                return name;
            }

            public int getType() {
                return type;
            }

            public int getLoa() {
                return loa;
            }

            public int getBearing() {
                return bearing;
            }

            public int getDistance() {
                return distance;
            }
        }

        private FreeFlowData(DateTime timestamp, int mmsi, String name, int type, int loa, List<TrackInsideEllipse> tracks) {
            this.timestamp = timestamp;
            this.mmsi = mmsi;
            this.name = name;
            this.type = type;
            this.loa = loa;
            this.tracks = tracks;
        }

        public DateTime getTimestamp() {
            return timestamp;
        }

        public int getMmsi() {
            return mmsi;
        }

        public String getName() {
            return name;
        }

        public int getType() {
            return type;
        }

        public int getLoa() {
            return loa;
        }

        public List<TrackInsideEllipse> getTracks() {
            return tracks;
        }
    }
}
