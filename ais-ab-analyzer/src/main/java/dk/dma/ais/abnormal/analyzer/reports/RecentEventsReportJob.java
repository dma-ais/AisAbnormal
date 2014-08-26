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

import static org.apache.commons.lang.StringUtils.isBlank;

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
            email.append(eventType.getSimpleName() + " (" + eventsByType.get(eventType).size() + ")\n");
            email.append("------------------------------------------------------------------------------------\n");
            eventsByType.get(eventType).forEach(event -> {
                tabbedAppend(email, event.getId());
                tabbedAppend(email, event.getStartTime());
                tabbedAppend(email, event.getEndTime());
                tabbedAppend(email, event.getBehaviours().iterator().next().getVessel().getMmsi());
                tabbedAppend(email, event.getBehaviours().iterator().next().getVessel().getName());
                email.append('\n');
            });
            email.append("====================================================================================\n");
            email.append("\n");
        });
        email.append("\n");
        email.append("</pre>");
        email.append("</html>");

        return email.toString();
    }

    private static StringBuffer tabbedAppend(StringBuffer sb, Object object) {
        if (object instanceof Date) {
            object = DATE_FORMAT.format(object);
        }
        sb.append(object == null || isBlank(object.toString()) ? "\t\t" : object.toString());
        sb.append('\t');
        return sb;
    }

}
