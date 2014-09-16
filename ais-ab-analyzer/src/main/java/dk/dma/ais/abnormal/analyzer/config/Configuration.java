package dk.dma.ais.abnormal.analyzer.config;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * This class is a singleton which holds configuration parameters for the application.
 */
@Singleton
public final class Configuration {
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

    public static final String CONFKEY_BLACKLIST_MMSI = "blacklist.mmsi";
    public static final String CONFKEY_FILTER_LOCATION_BBOX_NORTH = "filter.location.bbox.north";
    public static final String CONFKEY_FILTER_LOCATION_BBOX_SOUTH = "filter.location.bbox.south";
    public static final String CONFKEY_FILTER_LOCATION_BBOX_WEST = "filter.location.bbox.west";
    public static final String CONFKEY_FILTER_LOCATION_BBOX_EAST = "filter.location.bbox.east";
    public static final String CONFKEY_STATISTICS_FILE = "statistics.file";
    public static final String CONFKEY_EVENTS_REPOSITORY_TYPE = "events.repository.type";
    public static final String CONFKEY_EVENTS_PGSQL_HOST = "events.pgsql.host";
    public static final String CONFKEY_EVENTS_PGSQL_PORT = "events.pgsql.port";
    public static final String CONFKEY_EVENTS_PGSQL_NAME = "events.pgsql.name";
    public static final String CONFKEY_EVENTS_PGSQL_USERNAME = "events.pgsql.username";
    public static final String CONFKEY_EVENTS_PGSQL_PASSWORD = "events.pgsql.password";
    public static final String CONFKEY_EVENTS_H2_FILE = "events.h2.file";
    public static final String CONFKEY_AIS_DATASOURCE_URL = "ais.datasource.url";
    public static final String CONFKEY_AIS_DATASOURCE_DOWNSAMPLING = "ais.datasource.downsampling";
    public static final String CONFKEY_REPORTS_ENABLED = "reports.enabled";
    public static final String CONFKEY_REPORTS_RECENTEVENTS_CRON = "reports.recentevents.cron";
    public static final String CONFKEY_REPORTS_MAILER_SMTP_HOST = "reports.mailer.smtp.host";
    public static final String CONFKEY_REPORTS_MAILER_SMTP_PORT = "reports.mailer.smtp.port";
    public static final String CONFKEY_REPORTS_MAILER_SMTP_USER = "reports.mailer.smtp.user";
    public static final String CONFKEY_REPORTS_MAILER_SMTP_PASS = "reports.mailer.smtp.pass";
    public static final String CONFKEY_REPORTS_MAILER_SMTP_SSL = "reports.mailer.smtp.ssl";
    public static final String CONFKEY_REPORTS_MAILER_SMTP_FROM = "reports.mailer.smtp.from";
    public static final String CONFKEY_REPORTS_MAILER_SMTP_TO = "reports.mailer.smtp.to";
    
    public static boolean isValid(org.apache.commons.configuration.Configuration configuration) {
        if (configuration.isEmpty()) {
            return false;
        }

        List<String> keys = getAllConfigurationKeys();

        // Check all parameters present
        final int[] missingConfigKeys = {0};
        keys.forEach(key -> {
            if (!configuration.containsKey(key)) {
                missingConfigKeys[0]++;
                LOG.error("Missing configuration key: " + key);
            }
        });
        if (missingConfigKeys[0] > 0) {
            LOG.error("Missing " + missingConfigKeys[0] + " configuration keys.");
            return false;
        }

        // Summarize config parameters to log
        keys.forEach(key -> {
            Object property = configuration.getProperty(key);
            if (property == null || isBlank(property.toString())) {
                LOG.warn("Configuration key is blank: " + key);
            } else {
                if (key.toLowerCase().contains(".pass")) {
                    LOG.info("Using " + key + " = ***");
                } else {
                    LOG.info("Using " + key + " = " + property);
                }
            }
        });

        // Validate CONFKEY_STATISTICS_FILE
        String statisticsFileName = configuration.getString(CONFKEY_STATISTICS_FILE);
        File statisticsFile = new File(statisticsFileName);
        if (! statisticsFile.exists()) {
            LOG.warn("File does not exist: " + statisticsFileName);
        } else if (! statisticsFile.canRead()) {
            LOG.warn("Can not read file: " + statisticsFileName);
        }

        // Validate CONFKEY_AIS_DATASOURCE_URL
        String aisDataSourceUrlAsString = configuration.getString(CONFKEY_AIS_DATASOURCE_URL);
        try {
            new URL(aisDataSourceUrlAsString);
        } catch (MalformedURLException e) {
            LOG.warn(e.getMessage(), e);
        }

        //
        String eventsRepositoryType = configuration.getString(CONFKEY_EVENTS_REPOSITORY_TYPE);
        if (eventsRepositoryType == null) {
            LOG.error("Missing value for configuration key: " + CONFKEY_EVENTS_REPOSITORY_TYPE);
            return false;
        } else {
            String t = eventsRepositoryType.toLowerCase().trim();
            if (! ("h2".equals(t) || "pgsql".equals(t))) {
                LOG.error("Illegal value: '" + t + "'. Only 'pgsql' or 'h2' allowed for configuration key: " + CONFKEY_EVENTS_REPOSITORY_TYPE);
                return false;
            }
        }

        // Validate H2
        if ("h2".equals(eventsRepositoryType.toLowerCase().trim())) {
            String eventsH2File = configuration.getString(CONFKEY_EVENTS_H2_FILE);
            if (eventsH2File == null || isBlank(eventsH2File)) {
                LOG.warn("Missing value for: " + CONFKEY_EVENTS_H2_FILE);
            }
            File h2File = new File(eventsH2File);
            if (h2File.exists()) {
                if (!h2File.canRead()) {
                    LOG.warn("Cannot read from: " + h2File.getAbsoluteFile());
                }
                if (!h2File.canWrite()) {
                    LOG.warn("Cannot write to: " + h2File.getAbsoluteFile());
                }
            }
        }

        // Validate PGSQL
        if ("pgsql".equals(eventsRepositoryType.toLowerCase().trim())) {
            String eventsPgsqlHost = configuration.getString(CONFKEY_EVENTS_PGSQL_HOST);
            if (eventsPgsqlHost == null || isBlank(eventsPgsqlHost)) {
                LOG.error("Missing value for: " + CONFKEY_EVENTS_PGSQL_HOST);
                return false;
            }

            String eventsPgsqlPort = configuration.getString(CONFKEY_EVENTS_PGSQL_PORT);
            if (eventsPgsqlPort == null || isBlank(eventsPgsqlPort)) {
                LOG.error("Missing value for: " + CONFKEY_EVENTS_PGSQL_PORT);
                return false;
            }

            String eventsPgsqlUsername = configuration.getString(CONFKEY_EVENTS_PGSQL_USERNAME);
            if (eventsPgsqlUsername == null || isBlank(eventsPgsqlUsername)) {
                LOG.error("Missing value for: " + CONFKEY_EVENTS_PGSQL_USERNAME);
                return false;
            }

            String eventsPgsqlPassword = configuration.getString(CONFKEY_EVENTS_PGSQL_PASSWORD);
            if (eventsPgsqlPassword == null || isBlank(eventsPgsqlPassword)) {
                LOG.error("Missing value for: " + CONFKEY_EVENTS_PGSQL_PASSWORD);
                return false;
            }

            String eventsPgsqlName = configuration.getString(CONFKEY_EVENTS_PGSQL_NAME);
            if (eventsPgsqlName == null || isBlank(eventsPgsqlName)) {
                LOG.error("Missing value for: " + CONFKEY_EVENTS_PGSQL_NAME);
                return false;
            }
        }

        //
        return true;
    }

    private static List<String> getAllConfigurationKeys() {
        List<String> confkeys = Lists.newArrayList();

        Field[] fields = Configuration.class.getFields();
        for (Field field : fields) {
            if (field.getType() == String.class && field.getName().startsWith("CONFKEY_")) {
                try {
                    confkeys.add((String) field.get(null));
                } catch (IllegalAccessException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }

        return confkeys;
    }

}
