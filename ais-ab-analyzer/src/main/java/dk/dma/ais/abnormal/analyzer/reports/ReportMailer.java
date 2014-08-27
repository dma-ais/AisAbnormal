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

public class ReportMailer {

    private static final Logger LOG = LoggerFactory.getLogger(ReportMailer.class);

    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    @Inject
    Configuration configuration;

    public static final String CONFKEY_SMTP_HOST = "reports.mailer.smtp.host";
    public static final String CONFKEY_SMTP_PORT = "reports.mailer.smtp.port";
    public static final String CONFKEY_SMTP_USER = "reports.mailer.smtp.user";
    public static final String CONFKEY_SMTP_PASS = "reports.mailer.smtp.pass";
    public static final String CONFKEY_SMTP_SSL  = "reports.mailer.smtp.ssl";
    public static final String CONFKEY_SMTP_FROM = "reports.mailer.smtp.from";
    public static final String CONFKEY_SMTP_TO   = "reports.mailer.smtp.to";

    public void sendReport(String subject, String body) {
        try {
            HtmlEmail email = new HtmlEmail();
            email.setHostName(configuration.getString(CONFKEY_SMTP_HOST, "localhost"));
            email.setSmtpPort(configuration.getInt(CONFKEY_SMTP_PORT, 465));
            email.setAuthenticator(new DefaultAuthenticator(
                configuration.getString(CONFKEY_SMTP_USER, "anonymous"),
                configuration.getString(CONFKEY_SMTP_PASS, "guest")
            ));
            email.setStartTLSEnabled(false);
            email.setSSLOnConnect(configuration.getBoolean(CONFKEY_SMTP_SSL, true));
            email.setFrom(configuration.getString(CONFKEY_SMTP_FROM, ""));
            email.setSubject(subject);
            email.setHtmlMsg(body);
            String[] receivers = configuration.getStringArray(CONFKEY_SMTP_TO);
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
