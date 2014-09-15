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
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_REPORTS_MAILER_SMTP_FROM;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_REPORTS_MAILER_SMTP_HOST;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_REPORTS_MAILER_SMTP_PASS;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_REPORTS_MAILER_SMTP_PORT;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_REPORTS_MAILER_SMTP_SSL;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_REPORTS_MAILER_SMTP_TO;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_REPORTS_MAILER_SMTP_USER;


/**
 * The ReportMailer will send a message consisting of a subject and a body to a
 * pre-configured set of recipients.
 *
 * @author Thomas Borg Salling <tbsalling@tbsalling.dk>
 */
public class ReportMailer {

    private static final Logger LOG = LoggerFactory.getLogger(ReportMailer.class);

    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    /** The application main configuration class */
    @Inject
    Configuration configuration;

    /**
     * Send email with subject and message body.
     * @param subject the email subject.
     * @param body the email body.
     */
    public void send(String subject, String body) {
        try {
            HtmlEmail email = new HtmlEmail();
            email.setHostName(configuration.getString(CONFKEY_REPORTS_MAILER_SMTP_HOST, "localhost"));
            email.setSmtpPort(configuration.getInt(CONFKEY_REPORTS_MAILER_SMTP_PORT, 465));
            email.setAuthenticator(new DefaultAuthenticator(
                configuration.getString(CONFKEY_REPORTS_MAILER_SMTP_USER, "anonymous"),
                configuration.getString(CONFKEY_REPORTS_MAILER_SMTP_PASS, "guest")
            ));
            email.setStartTLSEnabled(false);
            email.setSSLOnConnect(configuration.getBoolean(CONFKEY_REPORTS_MAILER_SMTP_SSL, true));
            email.setFrom(configuration.getString(CONFKEY_REPORTS_MAILER_SMTP_FROM, ""));
            email.setSubject(subject);
            email.setHtmlMsg(body);
            String[] receivers = configuration.getStringArray(CONFKEY_REPORTS_MAILER_SMTP_TO);
            for (String receiver : receivers) {
                email.addTo(receiver);
            }
            email.send();
            LOG.info("Report sent with email to " + email.getToAddresses().toString() + " (\"" + subject + "\")");
        } catch (EmailException e) {
            LOG.error(e.getMessage(), e);
        }
    }

}
