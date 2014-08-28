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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.domain.Event;
import dk.dma.ais.abnormal.event.db.domain.TrackingPoint;
import dk.dma.ais.abnormal.event.db.domain.Vessel;
import dk.dma.ais.abnormal.util.Categorizer;
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

@NotThreadSafe
@Singleton
public class RecentEventsReportJob implements Job {

    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger(RecentEventsReportJob.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private static long lastRun = -1L;

    @Inject
    private Configuration configuration;

    @Inject
    private EventRepository eventRepository;

    @Inject
    private ReportMailer reportMailer;

    private static final String DATE_FORMAT_STRING = "dd/MM/yyyy HH:mm";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_STRING);

    public RecentEventsReportJob() {
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        LOG.debug("RecentEventsReportJob triggered");

        DateTime t2 = new DateTime();
        DateTime t1 = lastRun >= 0 ? new DateTime(lastRun) : t2.minusHours(24);
        lastRun = t2.getMillis();

        Multimap<Class<? extends Event>, Event> eventsByType = getEventsByType(t1.toDate(), t2.toDate());
        String reportBody = generateReportBody(t1.toDate(), t2.toDate(), eventsByType);
        reportMailer.sendReport("Abnormal events", reportBody);

        LOG.debug("RecentEventsReportJob finished");
    }

    private Multimap<Class<? extends Event>, Event> getEventsByType(Date from, Date to) {
        List<Event> events = eventRepository.findEventsByFromAndTo(from, to);
        ArrayListMultimap<Class<? extends Event>, Event> eventsByType = ArrayListMultimap.create();
        events.stream().forEach(event -> eventsByType.put(event.getClass(), event));
        return eventsByType;
    }

    private String generateReportBody(Date date0, Date date1, Multimap<Class<? extends Event>, Event> eventsByType) {
        StringBuffer email = new StringBuffer();

        email.append("<html>");
        email.append("<h2>Abnormal events</h2>");
        email.append("<h4>Detected between " + date0 + " and " + date1 + "</h4>");
        email.append("<br/>");

        email.append("<pre>");
        eventsByType.keySet().forEach(eventType -> {
            email.append(eventType.getSimpleName() + " (" + eventsByType.get(eventType).size() + ")\n\n");
            email.append(String.format("%-8s %-16s %-16s %-9s %-20s %-3s %-9s %-7s %-7s %-4s %-5s %-3s%n",
                "#", "BEGIN", "END", "MMSI", "NAME", "LOA", "TYPE", "LAT", "LON", "SOG", "COG", "HDG"));
            email.append("----------------------------------------------------------------------------------------------------------------------\n");
            eventsByType.get(eventType).forEach(event -> {
                Vessel vessel = event.getBehaviours().iterator().next().getVessel();
                TrackingPoint tp = event.getBehaviours().iterator().next().getTrackingPoints().last();
                email.append(String.format("%8d ", event.getId()));
                email.append(String.format("%16s ", DATE_FORMAT.format(event.getStartTime())));
                email.append(String.format("%16s ", event.getEndTime() == null ? " " : DATE_FORMAT.format(event.getStartTime())));
                email.append(String.format("%9d ", vessel.getMmsi()));
                email.append(String.format("%-20s ", vessel.getName() == null ? "" : vessel.getName()));
                email.append(String.format("%3d ", vessel.getLength() == null ? -1 : vessel.getLength()));
                email.append(String.format("%-9s ", vessel.getType() == null ? "" : Categorizer.mapShipTypeCategoryToString(Categorizer.mapShipTypeToCategory(vessel.getType()))).toUpperCase());
                email.append(String.format("%7.4f ", tp.getLatitude() == null ? Float.NaN : tp.getLatitude()));
                email.append(String.format("%7.4f ", tp.getLongitude() == null ? Float.NaN : tp.getLongitude()));
                email.append(String.format("%4.1f ", tp.getSpeedOverGround() == null ? Float.NaN : tp.getSpeedOverGround()));
                email.append(String.format("%5.1f ", tp.getCourseOverGround() == null ? Float.NaN : tp.getCourseOverGround()));
                email.append(String.format("%3.0f ", tp.getTrueHeading() == null ? Float.NaN : tp.getTrueHeading()));
                email.append('\n');
            });
            email.append("======================================================================================================================\n");
            email.append("\n");
        });
        email.append("\n");
        email.append("</pre>");
        email.append("</html>");

        return email.toString();
    }

}
