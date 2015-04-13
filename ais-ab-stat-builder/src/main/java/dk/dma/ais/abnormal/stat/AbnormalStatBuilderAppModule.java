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

package dk.dma.ais.abnormal.stat;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import dk.dma.ais.abnormal.stat.db.StatisticDataRepository;
import dk.dma.ais.abnormal.stat.db.mapdb.StatisticDataRepositoryMapDB;
import dk.dma.ais.abnormal.stat.statistics.CourseOverGroundStatistic;
import dk.dma.ais.abnormal.stat.statistics.ShipTypeAndSizeStatistic;
import dk.dma.ais.concurrency.stripedexecutor.StripedExecutorService;
import dk.dma.ais.filter.ReplayDownSampleFilter;
import dk.dma.ais.reader.AisReader;
import dk.dma.ais.reader.AisReaders;
import dk.dma.ais.tracker.eventEmittingTracker.EventEmittingTracker;
import dk.dma.ais.tracker.eventEmittingTracker.EventEmittingTrackerImpl;
import dk.dma.enav.model.geometry.grid.Grid;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AbnormalStatBuilderAppModule extends AbstractModule {
    private static Logger LOG = LoggerFactory.getLogger(AbnormalStatBuilderAppModule.class);

    private final String outputFilename;
    private final String inputDirectory;
    private final String inputFilenamePattern;
    private final boolean inputRecursive;
    private final Integer gridSize;
    private final Integer downSampling;

    public AbnormalStatBuilderAppModule(String outputFilename, String inputDirectory, String inputFilenamePattern, boolean inputRecursive, Integer gridSize, Integer downSampling) {
        this.outputFilename = outputFilename;
        this.inputDirectory = inputDirectory;
        this.inputFilenamePattern = inputFilenamePattern;
        this.inputRecursive = inputRecursive;
        this.gridSize = gridSize;
        this.downSampling = downSampling;
    }

    @Override
    public void configure() {
        install(new FactoryModuleBuilder()
                .implement(PacketHandler.class, PacketHandlerImpl.class)
                .build(PacketHandlerFactory.class));

        bind(StripedExecutorService.class).in(Singleton.class);
        bind(AbnormalStatBuilderApp.class).in(Singleton.class);
        bind(ProgressIndicator.class).in(Singleton.class);
        bind(AppStatisticsService.class).to(AppStatisticsServiceImpl.class).in(Singleton.class);
        bind(dk.dma.ais.abnormal.application.statistics.AppStatisticsService.class).to(AppStatisticsServiceImpl.class).in(Singleton.class);
        bind(ShipTypeAndSizeStatistic.class);
        bind(CourseOverGroundStatistic.class);
    }

    @Provides
    @Singleton
    EventEmittingTracker provideEventEmittingTracker() {
        return new EventEmittingTrackerImpl(provideGrid(), initVesselBlackList(provideConfiguration()));
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
    Configuration provideConfiguration() {
        Configuration configuration = null;
        try {
            PropertiesConfiguration configFile = new PropertiesConfiguration("stat-builder.properties") ;
            configuration = configFile;
            LOG.info("Loaded configuration file " + configFile.getFile().toString() + ".");
        } catch (ConfigurationException e) {
            configuration = new BaseConfiguration();
            LOG.warn(e.getMessage() + ". Using blank configuration.");
        }
        return configuration;
    }

    @Provides
    @Singleton
    Grid provideGrid() {
        Grid grid = null;
        try {
            grid = Grid.createSize(gridSize);
            LOG.info("Created Grid with grid size of " + gridSize + " meters.");
        } catch (Exception e) {
            LOG.error("Failed to create Grid object", e);
        }
        return grid;
    }

    @Provides
    @Singleton
    StatisticDataRepository provideStatisticDataRepository() {
        StatisticDataRepository statisticsRepository = null;
        try {
            statisticsRepository = new StatisticDataRepositoryMapDB(outputFilename);
            statisticsRepository.openForWrite(true);
            LOG.info("Opened statistic set database with filename '" + outputFilename + "'.");
        } catch (Exception e) {
            LOG.error("Failed to create StatisticDataRepository object", e);
        }
        return statisticsRepository;
    }

    @Provides
    @Singleton
    AisReader provideAisReader() {
        AisReader aisReader = null;
        try {
            aisReader = AisReaders.createDirectoryReader(inputDirectory, inputFilenamePattern, inputRecursive);
            LOG.info("Created AisReader.");
        } catch (Exception e) {
            LOG.error("Failed to create AisReader object", e);
        }
        return aisReader;
    }

    /**
     * Initialize internal data structures required to accept/reject track updates based on black list mechanism.
     * @param configuration
     * @return
     */
    private static int[] initVesselBlackList(Configuration configuration) {
        ArrayList<Integer> blacklistedMmsis = new ArrayList<>();
        try {
            List blacklistedMmsisConfig = configuration.getList("blacklist.mmsi");
            blacklistedMmsisConfig.forEach(
                    blacklistedMmsi -> {
                        try {
                            Integer blacklistedMmsiBoxed = Integer.valueOf(blacklistedMmsi.toString());
                            if (blacklistedMmsiBoxed > 0 && blacklistedMmsiBoxed < 1000000000) {
                                blacklistedMmsis.add(blacklistedMmsiBoxed);
                            } else if (blacklistedMmsiBoxed != -1) {
                                LOG.warn("Black listed MMSI no. out of range: " + blacklistedMmsiBoxed + ".");
                            }
                        } catch (NumberFormatException e) {
                            LOG.warn("Black listed MMSI no. \"" + blacklistedMmsi + "\" cannot be cast to integer.");
                        }
                    }
            );
        } catch (ConversionException e) {
            LOG.warn(e.getMessage(), e);
        }

        if (blacklistedMmsis.size() > 0) {
            LOG.info("The following " + blacklistedMmsis.size() + " MMSI numbers are black listed and will not be tracked.");
            LOG.info(Arrays.toString(blacklistedMmsis.toArray()));
        }

        int[] array = new int[blacklistedMmsis.size()];
        for (int i = 0; i < blacklistedMmsis.size(); i++) {
            array[i] = blacklistedMmsis.get(i);
        }

        return array;
    }
}
