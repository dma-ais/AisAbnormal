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
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
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
        StringBuffer email = new StringBuffer();

        email.append("<html>");
        email.append("<h2>Free flow data</h2>");
        email.append("<h4>Detected between " + date0 + " and " + date1 + "</h4>");
        email.append("<br/>");

        email.append("<pre>");
        for (FreeFlowAnalysis.FreeFlowData freeFlowData : data) {
            email.append(String.format("%-20s ", freeFlowData.getTimestamp()));
            email.append(String.format("MMSI:%9d ", freeFlowData.getMmsi()));
            email.append(String.format("NAME:%-20s ", freeFlowData.getName()));
            email.append(String.format("TYPE:%2d ", freeFlowData.getType()));
            email.append(String.format("LOA:%3d ", freeFlowData.getLoa()));
            email.append('\n');

            List<FreeFlowAnalysis.FreeFlowData.TrackInsideEllipse> tracks = freeFlowData.getTracks();
            for (FreeFlowAnalysis.FreeFlowData.TrackInsideEllipse track : tracks) {
                email.append('\t');
                email.append(String.format("MMSI:%9d ", track.getMmsi()));
                email.append(String.format("NAME:%-20s ", track.getName()));
                email.append(String.format("TYPE:%2d ", track.getType()));
                email.append(String.format("LOA:%3d ", track.getLoa()));
                email.append(String.format("BRG:%3d ", track.getBearing()));
                email.append(String.format("DIS:%5d ", track.getDistance()));
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
