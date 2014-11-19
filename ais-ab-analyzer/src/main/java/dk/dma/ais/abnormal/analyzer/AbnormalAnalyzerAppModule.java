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

import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import dk.dma.ais.abnormal.analyzer.behaviour.BehaviourManager;
import dk.dma.ais.abnormal.analyzer.behaviour.BehaviourManagerImpl;
import dk.dma.ais.abnormal.analyzer.reports.ReportJobFactory;
import dk.dma.ais.abnormal.analyzer.reports.ReportMailer;
import dk.dma.ais.abnormal.analyzer.reports.ReportScheduler;
import dk.dma.ais.abnormal.event.db.EventRepository;
import dk.dma.ais.abnormal.event.db.jpa.JpaEventRepository;
import dk.dma.ais.abnormal.event.db.jpa.JpaSessionFactoryFactory;
import dk.dma.ais.abnormal.stat.db.StatisticDataRepository;
import dk.dma.ais.abnormal.stat.db.data.DatasetMetaData;
import dk.dma.ais.abnormal.stat.db.data.ShipTypeAndSizeStatisticData;
import dk.dma.ais.abnormal.stat.db.mapdb.StatisticDataRepositoryMapDB;
import dk.dma.ais.abnormal.tracker.EventEmittingTracker;
import dk.dma.ais.abnormal.tracker.Tracker;
import dk.dma.ais.filter.ExpressionFilter;
import dk.dma.ais.filter.GeoMaskFilter;
import dk.dma.ais.filter.IPacketFilter;
import dk.dma.ais.filter.LocationFilter;
import dk.dma.ais.filter.ReplayDownSampleFilter;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.reader.AisReader;
import dk.dma.ais.reader.AisReaders;
import dk.dma.enav.model.geometry.BoundingBox;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.model.geometry.grid.Grid;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_AIS_DATASOURCE_DOWNSAMPLING;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_AIS_DATASOURCE_URL;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_EVENTS_H2_FILE;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_EVENTS_PGSQL_HOST;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_EVENTS_PGSQL_NAME;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_EVENTS_PGSQL_PASSWORD;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_EVENTS_PGSQL_PORT;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_EVENTS_PGSQL_USERNAME;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_EVENTS_REPOSITORY_TYPE;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_FILTER_CUSTOM_EXPRESSION;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_FILTER_LOCATION_BBOX_EAST;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_FILTER_LOCATION_BBOX_NORTH;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_FILTER_LOCATION_BBOX_SOUTH;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_FILTER_LOCATION_BBOX_WEST;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_FILTER_SHIPNAME_SKIP;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_STATISTICS_FILE;
import static dk.dma.ais.packet.AisPacketFilters.parseExpressionFilter;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * This is the Google Guice module class which defines creates objects to be injected by Guice.
 *
 * @author Thomas Borg Salling <tbsalling@tbsalling.dk>
 */
public final class AbnormalAnalyzerAppModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(AbnormalAnalyzerAppModule.class);
    {
        LOG.info(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    public final static long STARTUP_TIMESTAMP = System.currentTimeMillis();

    private final Path configFile;

    public AbnormalAnalyzerAppModule(Path configFile) {
        this.configFile = configFile;
    }

    @Override
    public void configure() {
        bind(AbnormalAnalyzerApp.class).in(Singleton.class);
        bind(AppStatisticsService.class).to(AppStatisticsServiceImpl.class).in(Singleton.class);
        bind(dk.dma.ais.abnormal.application.statistics.AppStatisticsService.class).to(AppStatisticsServiceImpl.class).in(Singleton.class);
        bind(PacketHandler.class).to(PacketHandlerImpl.class).in(Singleton.class);
        bind(Tracker.class).to(EventEmittingTracker.class).in(Singleton.class);
        bind(BehaviourManager.class).to(BehaviourManagerImpl.class).in(Singleton.class);
        bind(SchedulerFactory.class).to(StdSchedulerFactory.class).in(Scopes.SINGLETON);
        bind(ReportJobFactory.class).in(Scopes.SINGLETON);
        bind(ReportScheduler.class).in(Scopes.SINGLETON);
        bind(ReportMailer.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    Configuration provideConfiguration() {
        PropertiesConfiguration configuration = null;
        if (Files.exists(configFile)) {
            try {
                configuration = new PropertiesConfiguration(configFile.toFile());
            } catch (ConfigurationException e) {
                LOG.error(e.getMessage(), e);
                System.exit(-1);
            }
            LOG.info("Using configuration file: " + configFile);
        }
        if (configuration == null) {
            LOG.error("Could not find configuration file: " + configFile);
            printConfigurationTemplate();
            System.exit(-1);
        }
        if (configuration.isEmpty()) {
            LOG.error("Configuration file was empty: " + configFile);
            printConfigurationTemplate();
            System.exit(-1);
        }
        if (! dk.dma.ais.abnormal.analyzer.config.Configuration.isValid(configuration)) {
            LOG.error("Configuration is invalid: " + configFile);
            printConfigurationTemplate();
            System.exit(-1);
        }
        LOG.info("Configuration file located, read and presumed valid.");
        return configuration;
    }

    private void printConfigurationTemplate() {
        InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("analyzer.properties");

        System.out.println("Use this template for configuration file:");
        System.out.println("-----------------------------------------");
        try {
            ByteStreams.copy(resourceAsStream, System.out);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        System.out.println("-----------------------------------------");
    }

    @Provides
    @Singleton
    EventRepository provideEventRepository() {
        SessionFactory sessionFactory;
        Configuration configuration = getConfiguration();
        String eventRepositoryType = configuration.getString(CONFKEY_EVENTS_REPOSITORY_TYPE);
        try {
            if ("h2".equalsIgnoreCase(eventRepositoryType)) {
                sessionFactory = JpaSessionFactoryFactory.newH2SessionFactory(new File(configuration.getString(CONFKEY_EVENTS_H2_FILE)));
            } else if ("pgsql".equalsIgnoreCase(eventRepositoryType)) {
                sessionFactory = JpaSessionFactoryFactory.newPostgresSessionFactory(
                    configuration.getString(CONFKEY_EVENTS_PGSQL_HOST),
                    configuration.getInt(CONFKEY_EVENTS_PGSQL_PORT, 8432),
                    configuration.getString(CONFKEY_EVENTS_PGSQL_NAME),
                    configuration.getString(CONFKEY_EVENTS_PGSQL_USERNAME),
                    configuration.getString(CONFKEY_EVENTS_PGSQL_PASSWORD)
                );
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
        Configuration configuration = getConfiguration();
        StatisticDataRepository statisticsRepository = null;
        try {
            String statisticsFilename = configuration.getString(CONFKEY_STATISTICS_FILE);
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

        Configuration configuration = getConfiguration();
        String aisDatasourceUrlAsString = configuration.getString(CONFKEY_AIS_DATASOURCE_URL);

        URL aisDataSourceUrl;
        try {
            aisDataSourceUrl = new URL(aisDatasourceUrlAsString);
        } catch (MalformedURLException e) {
            LOG.error(e.getMessage(), e);
            return null;
        }

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

    @Provides
    Set<IPacketFilter> provideFilters() {
        return Sets.newHashSet(
            provideReplayDownSampleFilter(),
            provideGeoMaskFilter(),
            provideLocationFilter(),
            provideExpressionFilter()
        );
    }

    @Provides
    IPacketFilter provideExpressionFilter() {
        Configuration configuration = getConfiguration();
        String filterExpression = configuration.getString(CONFKEY_FILTER_CUSTOM_EXPRESSION);
        IPacketFilter expressionFilter;
        if (isBlank(filterExpression)) {
            expressionFilter = aisPacket -> false;
        } else {
            expressionFilter = new ExpressionFilter(filterExpression);
        }
        LOG.info("Created ExpressionFilter with expression: " + filterExpression);
        return expressionFilter;
    }

    @Provides
    ReplayDownSampleFilter provideReplayDownSampleFilter() {
        Configuration configuration = getConfiguration();
        int downsampling = configuration.getInt(CONFKEY_AIS_DATASOURCE_DOWNSAMPLING, 0);
        ReplayDownSampleFilter filter = new ReplayDownSampleFilter(downsampling);
        LOG.info("Created ReplayDownSampleFilter with down sampling period of " + downsampling + " secs.");
        return filter;
    }

    @Provides
    GeoMaskFilter provideGeoMaskFilter() {
        List<BoundingBox> boundingBoxes = null;

        URL resource = ClassLoader.class.getResource("/geomask.xml");
        LOG.info("Reading geomask from " + resource.toString());
        try {
            boundingBoxes = parseGeoMaskXmlInputStream(resource.openStream());
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

        return new GeoMaskFilter(boundingBoxes);
    }

    @Provides
    LocationFilter provideLocationFilter() {
        Configuration configuration = getConfiguration();

        Float north = configuration.getFloat(CONFKEY_FILTER_LOCATION_BBOX_NORTH, null);
        Float south = configuration.getFloat(CONFKEY_FILTER_LOCATION_BBOX_SOUTH, null);
        Float east = configuration.getFloat(CONFKEY_FILTER_LOCATION_BBOX_EAST, null);
        Float west = configuration.getFloat(CONFKEY_FILTER_LOCATION_BBOX_WEST, null);

        BoundingBox tmpBbox = null;
        if (north != null && south != null && east != null && west != null) {
            tmpBbox = BoundingBox.create(Position.create(north, west), Position.create(south, east), CoordinateSystem.CARTESIAN);
            LOG.info("Area: " + tmpBbox);
        } else {
            LOG.warn("No location-based pre-filtering of messages.");
        }

        LocationFilter filter = new LocationFilter();

        if (tmpBbox == null) {
            filter.addFilterGeometry(e -> true);
        } else {
            final BoundingBox bbox = tmpBbox;
            filter.addFilterGeometry(position -> {
                if (position == null) {
                    return false;
                }
                return bbox.contains(position);
            });
        }

        return filter;
    }

    @Provides
    @Named("shipNameFilter")
    Predicate<AisPacket> provideShipNameFilter() {
        Configuration configuration = getConfiguration();
        String[] shipNames = configuration.getStringArray(CONFKEY_FILTER_SHIPNAME_SKIP);
        Predicate<AisPacket> filter = null;
        if (shipNames != null && shipNames.length > 0 && !(shipNames.length == 1 && shipNames[0].trim().length() == 0)) {
            String filterExpression = "";
            for (int i = 0; i < shipNames.length; i++) {
                filterExpression += "t.name ~ " + shipNames[i];
                if (i != shipNames.length - 1) {
                    filterExpression += " | ";
                }
            }
            LOG.debug("filterExpression: " + filterExpression);
            filter = parseExpressionFilter(filterExpression);
            LOG.info("Created ship name filter: " + filterExpression);
        }
        return filter != null ? filter : aisPacket -> false;
    }

    private List<BoundingBox> parseGeoMaskXmlInputStream(InputStream is) {
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.parse(is);
            document.normalizeDocument();
            final NodeList areaPolygons = document.getElementsByTagName("area_polygon");
            final int numAreaPolygons = areaPolygons.getLength();
            for (int i = 0; i < numAreaPolygons; i++) {
                Node areaPolygon = areaPolygons.item(i);
                LOG.debug("XML reading area_polygon " + areaPolygon.getAttributes().getNamedItem("name").toString());
                NodeList polygons = areaPolygon.getChildNodes();
                int numPolygons = polygons.getLength();
                for (int p = 0; p < numPolygons; p++) {
                    Node polygon = polygons.item(p);
                    if (polygon instanceof Element) {
                        NodeList items = polygon.getChildNodes();
                        int numItems = items.getLength();
                        BoundingBox boundingBox = null;
                        try {
                            for (int j = 0; j < numItems; j++) {
                                Node item = items.item(j);
                                if (item instanceof Element) {
                                    final double lat = Double.parseDouble(item.getAttributes().getNamedItem("lat").getNodeValue());
                                    final double lon = Double.parseDouble(item.getAttributes().getNamedItem("lon").getNodeValue());
                                    if (boundingBox == null) {
                                        boundingBox = BoundingBox.create(Position.create(lat, lon), Position.create(lat, lon), CoordinateSystem.CARTESIAN);
                                    } else {
                                        boundingBox = boundingBox.include(Position.create(lat, lon));
                                    }
                                }
                            }
                            LOG.info("Blocking messages in bbox " + areaPolygon.getAttributes().getNamedItem("name").toString() + ": " + boundingBox.toString() + " " + (boundingBox.getMaxLat()-boundingBox.getMinLat()) + " " + (boundingBox.getMaxLon()-boundingBox.getMinLon()));
                            boundingBoxes.add(boundingBox);
                        } catch (NumberFormatException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace(System.err);
        } catch (SAXException e) {
            e.printStackTrace(System.err);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }

        return boundingBoxes;
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

    private Configuration getConfiguration() {
        return AbnormalAnalyzerApp.getInjector().getInstance(Configuration.class);
    }
}
