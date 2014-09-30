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
package dk.dma.ais.abnormal.analyzer.reports;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.dma.ais.abnormal.analyzer.analysis.FreeFlowAnalysis;
import dk.dma.ais.abnormal.tracker.Track;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * This class is a Job which is executed to generate a "recent events" report.
 *
 * A recent event report is a list of events raised since the previous run of this job.
 * At the first invocation, the report contains events from the previous 24 hours.
 *
 * @author Thomas Borg Salling <tbsalling@tbsalling.dk>
 */
@NotThreadSafe
@Singleton
public class FreeFlowAnalysisReportJob implements Job {

    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger(FreeFlowAnalysisReportJob.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private static long lastRun = -1L;

    @Inject
    private Configuration configuration;

    @Inject
    private FreeFlowAnalysis freeFlowAnalysis;

    @Inject
    private ReportMailer reportMailer;

    private static final String DATE_FORMAT_STRING = "dd/MM/yyyy HH:mm";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_STRING);

    public FreeFlowAnalysisReportJob() {
    }

    /**
     * The execute method is triggered by the scheduler, when the report should be generated.
     *
     * @param jobExecutionContext
     * @throws org.quartz.JobExecutionException
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        LOG.debug("FreeFlowAnalysisReportJob triggered");

        DateTime t2 = new DateTime();
        DateTime t1 = lastRun >= 0 ? new DateTime(lastRun) : t2.minusHours(24);
        lastRun = t2.getMillis();

        List<FreeFlowAnalysis.FreeFlowData> tmpData = freeFlowAnalysis.getTmpData();

        String reportBody = generateReportBody(t1.toDate(), t2.toDate(), tmpData);
        reportMailer.send("Free flow analysis", reportBody);

        LOG.debug("FreeFlowAnalysisReportJob finished");
    }

    private String generateReportBody(Date date0, Date date1, List<FreeFlowAnalysis.FreeFlowData> data) {
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss Z");
        StringBuffer email = new StringBuffer();

        email.append("<html>");
        email.append("<h2>Free flow data</h2>");
        email.append("<h4>Detected between " + date0 + " and " + date1 + "</h4>");
        email.append("<br/>");

        email.append("<pre>");

        email.append(
            String.format("%-25s %-9s %-20s %-2s %-3s %-3s %-3s %-3s %-3s %-25s %-3s %-4s%n",
            "TIMESTAMP", "MMSI", "NAME", "TP", "LOA", "BM", "COG", "HDG", "SOG", "POS", "BRG", "DST")
        );
        email.append("----------------------------------------------------------------------------------------------------------------------\n");

        for (FreeFlowAnalysis.FreeFlowData freeFlowData : data) {
            Track t0 = freeFlowData.getTrackSnapshot();
            Position p0 = freeFlowData.getTrackCenterPosition();

            email.append("<b>");
            email.append(String.format("%-25s ", fmt.withZoneUTC().print(t0.getTimeOfLastPositionReport())));
            email.append(String.format("%9d ", t0.getMmsi()));
            email.append(String.format("%-20s ", t0.getShipName()));
            email.append(String.format("%2d ", t0.getShipType()));
            email.append(String.format("%3d ", t0.getVesselLength()));
            email.append(String.format("%3d ", t0.getVesselBeam()));
            email.append(String.format("%3.0f ", t0.getCourseOverGround()));
            email.append(String.format("%3.0f ", t0.getTrueHeading()));
            email.append(String.format("%3.0f ", t0.getSpeedOverGround()));
            email.append(String.format("%-25s ", p0.toString()));
            email.append("</b>");
            email.append('\n');

            List<FreeFlowAnalysis.FreeFlowData.TrackInsideEllipse> tracks = freeFlowData.getTracksInsideEllipse();
            for (FreeFlowAnalysis.FreeFlowData.TrackInsideEllipse track : tracks) {
                Track t1 = track.getTrackSnapshot();
                Position p1 = track.getTrackCenterPosition();

                int d = (int) p0.distanceTo(p1, CoordinateSystem.CARTESIAN);
                int b = (int) p0.rhumbLineBearingTo(p1);

                email.append(String.format("%-25s ", fmt.withZoneUTC().print(t1.getTimeOfLastPositionReport())));
                email.append(String.format("%9d ", t1.getMmsi()));
                email.append(String.format("%-20s ", t1.getShipName()));
                email.append(String.format("%2d ", t1.getShipType()));
                email.append(String.format("%3d ", t1.getVesselLength()));
                email.append(String.format("%3d ", t1.getVesselBeam()));
                email.append(String.format("%3.0f ", t1.getCourseOverGround()));
                email.append(String.format("%3.0f ", t1.getTrueHeading()));
                email.append(String.format("%3.0f ", t1.getSpeedOverGround()));
                email.append(String.format("%-25s ", p1.toString()));
                email.append(String.format("%3d ", b));
                email.append(String.format("%4d ", d));
                email.append('\n');
            }
            email.append('\n');
        }

        email.append("======================================================================================================================\n");
        email.append("\n");

        email.append("</pre>");
        email.append("</html>");

        return email.toString();
    }

}
