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
import org.apache.commons.configuration.Configuration;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_REPORTS_ENABLED;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_REPORTS_RECENTEVENTS_CRON;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * The Report Scheduler configures a schedule of all periodic reports to be
 * automatically generated and distributed.
 *
 * @author Thomas Borg Salling <tbsalling@tbsalling.dk>
 */
public class ReportScheduler {

    private final Scheduler scheduler;

    @Inject
    private Configuration configuration;

    /**
     * The logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ReportScheduler.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    public void start() {
        if (isEnabled()) {
            try {
                scheduler.start();
            } catch (SchedulerException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    @Inject
    public ReportScheduler(final Configuration configuration, final SchedulerFactory factory, final ReportJobFactory jobFactory) throws SchedulerException {
        this.configuration = configuration;
        if (isEnabled()) {
            scheduler = factory.getScheduler();
            scheduler.setJobFactory(jobFactory);
            addDailyEventsReportJob();
        } else {
            scheduler = null;
        }
    }

    private boolean isEnabled() {
        return configuration.getBoolean(CONFKEY_REPORTS_ENABLED, false);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (isEnabled()) {
            scheduler.shutdown();
        }
    }

    private void addDailyEventsReportJob() {
        try {
            Trigger trigger1 = newTrigger()
                    .withIdentity("DailyEventsReportJobTrigger")
                    .startNow()
                    .withSchedule(cronSchedule(configuration.getString(CONFKEY_REPORTS_RECENTEVENTS_CRON, "0/30 * * * * ?")))
                    .build();

            JobDetail job1 = newJob(RecentEventsReportJob.class)
                .withIdentity("DailyEventsReportJob")
                .build();

            scheduler.scheduleJob(job1, trigger1);
        } catch (SchedulerException se) {
            LOG.error(se.getMessage(), se);
        }
    }
}