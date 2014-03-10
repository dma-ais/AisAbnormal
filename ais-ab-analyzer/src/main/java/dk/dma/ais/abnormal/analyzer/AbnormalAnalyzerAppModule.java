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

package dk.dma.ais.abnormal.analyzer;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dk.dma.ais.abnormal.analyzer.behaviour.BehaviourManager;
import dk.dma.ais.abnormal.analyzer.behaviour.BehaviourManagerImpl;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.jpa.JpaEventRepository;
import dk.dma.ais.abnormal.event.db.jpa.JpaSessionFactoryFactory;
import dk.dma.ais.abnormal.stat.db.StatisticDataRepository;
import dk.dma.ais.abnormal.stat.db.data.DatasetMetaData;
import dk.dma.ais.abnormal.stat.db.data.ShipTypeAndSizeStatisticData;
import dk.dma.ais.abnormal.stat.db.mapdb.StatisticDataRepositoryMapDB;
import dk.dma.ais.abnormal.tracker.TrackingService;
import dk.dma.ais.abnormal.tracker.TrackingServiceImpl;
import dk.dma.ais.filter.ReplayDownSampleFilter;
import dk.dma.ais.reader.AisReader;
import dk.dma.ais.reader.AisReaders;
import dk.dma.enav.model.geometry.grid.Grid;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Set;

public final class AbnormalAnalyzerAppModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(AbnormalAnalyzerAppModule.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private final URL aisDataSourceUrl;
    private final String statistics;
    private final String pathToEventDatabase;
    private final Integer downSampling;
    private final String eventRepositoryType;
    private final String eventDataDbHost;
    private final Integer eventDataDbPort;
    private final String eventDataDbName;
    private final String eventDataDbUsername;
    private final String eventDataDbPassword;

    public AbnormalAnalyzerAppModule(URL aisDataSourceUrl, String statistics, String eventDataDbFile, Integer downSampling, String eventRepositoryType, String eventDataDbHost, Integer eventDataDbPort, String eventDataDbName, String eventDataDbUsername, String eventDataDbPassword) {
        this.aisDataSourceUrl = aisDataSourceUrl;
        this.statistics = statistics;
        this.pathToEventDatabase = eventDataDbFile;
        this.downSampling = downSampling;
        this.eventRepositoryType = eventRepositoryType;
        this.eventDataDbHost = eventDataDbHost;
        this.eventDataDbPort = eventDataDbPort;
        this.eventDataDbName = eventDataDbName;
        this.eventDataDbUsername = eventDataDbUsername;
        this.eventDataDbPassword = eventDataDbPassword;
    }

    @Override
    public void configure() {
        bind(AbnormalAnalyzerApp.class).in(Singleton.class);
        bind(AppStatisticsService.class).to(AppStatisticsServiceImpl.class).in(Singleton.class);
        bind(dk.dma.ais.abnormal.application.statistics.AppStatisticsService.class).to(AppStatisticsServiceImpl.class).in(Singleton.class);
        bind(PacketHandler.class).to(PacketHandlerImpl.class).in(Singleton.class);
        bind(TrackingService.class).to(TrackingServiceImpl.class).in(Singleton.class);
        bind(BehaviourManager.class).to(BehaviourManagerImpl.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    Configuration provideConfiguration() {
        Configuration configuration = null;
        try {
            PropertiesConfiguration configFile = new PropertiesConfiguration("analyzer.properties") ;
            configuration = configFile;
            LOG.info("Loaded configuration file " + configFile.getFile().toString() + ".");
        } catch (ConfigurationException e) {
            configuration = new BaseConfiguration();
            LOG.warn(e.getMessage() + ". Using blank configuration.");
        }
        return configuration;
    }

    @Provides
    ReplayDownSampleFilter provideReplayDownSampleFilter() {
        ReplayDownSampleFilter filter = null;
        try {
            filter = new ReplayDownSampleFilter(downSampling);
            LOG.info("Created ReplayDownSampleFilter with down sampling period of " + downSampling + " secs.");
        } catch (Exception e) {
            LOG.error("Failed to create ReplayDownSampleFilter object", e);
        }
        return filter;
    }

    @Provides
    @Singleton
    EventRepository provideEventRepository() {
        SessionFactory sessionFactory;

        try {
            if ("h2".equalsIgnoreCase(eventRepositoryType)) {
                    sessionFactory = JpaSessionFactoryFactory.newH2SessionFactory(new File(pathToEventDatabase));
            } else if ("pgsql".equalsIgnoreCase(eventRepositoryType)) {
                sessionFactory = JpaSessionFactoryFactory.newPostgresSessionFactory(eventDataDbHost, eventDataDbPort, eventDataDbName, eventDataDbUsername, eventDataDbPassword);
            } else {
                throw new IllegalArgumentException("eventRepositoryType: " + eventRepositoryType);
            }
        } catch (HibernateException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }

        return new JpaEventRepository(sessionFactory, false);
    }

    @Provides
    @Singleton
    StatisticDataRepository provideStatisticDataRepository() {
        StatisticDataRepository statisticsRepository = null;
        try {
            String statisticsFilename = statistics;
            statisticsRepository = new StatisticDataRepositoryMapDB(statisticsFilename);
            statisticsRepository.openForRead();
            LOG.info("Opened statistic set database with filename '" + statisticsFilename + "' for read.");
            if (!isValidStatisticDataRepositoryFormat(statisticsRepository)) {
                LOG.error("Statistic data repository is invalid. Analyses will be unreliable!");
            } else {
                LOG.info("Statistic data repository is valid.");
            }
        } catch (Exception e) {
            LOG.error("Failed to create or open StatisticDataRepository.", e);
        }
        return statisticsRepository;
    }

    @Provides
    @Singleton
    AisReader provideAisReader() {
        AisReader aisReader = null;

        String protocol = aisDataSourceUrl.getProtocol();
        LOG.debug("AIS data source protocol: " + protocol);

        if ("file".equalsIgnoreCase(protocol)) {
            try {
                File file = new File(aisDataSourceUrl.getPath());
                String path = file.getParent();
                String pattern = file.getName();
                LOG.debug("AIS data source is file system - " + path + "/" + pattern);

                aisReader = AisReaders.createDirectoryReader(path, pattern, true);
                LOG.info("Created AisReader (" + aisReader + ").");
            } catch (Exception e) {
                LOG.error("Failed to create AisReader.", e);
            }
        } else if ("tcp".equalsIgnoreCase(protocol)) {
            try {
                String host = aisDataSourceUrl.getHost();
                int port = aisDataSourceUrl.getPort();
                LOG.debug("AIS data source is TCP - " + host + ":" + port);

                aisReader = AisReaders.createReader(host, port);
                LOG.info("Created AisReader (" + aisReader + ").");
            } catch (Exception e) {
                LOG.error("Failed to create AisReader.", e);
            }
        }
        return aisReader;
    }

    @Provides
    @Singleton
    Grid provideGrid() {
        Grid grid = null;
        try {
            StatisticDataRepository statisticsRepository = AbnormalAnalyzerApp.getInjector().getInstance(StatisticDataRepository.class);
            DatasetMetaData metaData = statisticsRepository.getMetaData();
            Double gridResolution = metaData.getGridResolution();
            grid = Grid.create(gridResolution);
            LOG.info("Created Grid with size " + grid.getSize() + " meters.");
        } catch (Exception e) {
            LOG.error("Failed to create Grid object", e);
        }
        return grid;
    }

    private static boolean isValidStatisticDataRepositoryFormat(StatisticDataRepository statisticsRepository) {
        boolean valid = true;

        // TODO Check format version no.

        // Ensure that all expected statistics are present in the statistic file
        boolean containsStatisticShipSizeAndTypeStatistic = false;
        Set<String> statisticNames = statisticsRepository.getStatisticNames();
        for (String statisticName : statisticNames) {
            if ("ShipTypeAndSizeStatistic".equals(statisticName)) {
                containsStatisticShipSizeAndTypeStatistic = true;
            }
        }

        if (!containsStatisticShipSizeAndTypeStatistic) {
            LOG.error("Statistic data do not contain data for statistic \"ShipTypeAndSizeStatistic\"");
            valid = false;
        }

        // Check ShipTypeAndSizeStatistic
        ShipTypeAndSizeStatisticData shipSizeAndTypeStatistic = (ShipTypeAndSizeStatisticData) statisticsRepository.getStatisticDataForRandomCell("ShipTypeAndSizeStatistic");

        return valid;
    }

}
