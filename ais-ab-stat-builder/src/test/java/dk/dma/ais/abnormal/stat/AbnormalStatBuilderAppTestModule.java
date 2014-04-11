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
import dk.dma.ais.abnormal.stat.statistics.ShipTypeAndSizeStatistic;
import dk.dma.ais.abnormal.tracker.EventEmittingTracker;
import dk.dma.ais.abnormal.tracker.Tracker;
import dk.dma.ais.concurrency.stripedexecutor.StripedExecutorService;
import dk.dma.ais.reader.AisReader;
import dk.dma.ais.reader.AisReaders;
import dk.dma.enav.model.geometry.grid.Grid;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbnormalStatBuilderAppTestModule extends AbstractModule {

    private static Logger LOG = LoggerFactory.getLogger(AbnormalStatBuilderAppModule.class);

    private final String outputFilename;
    private final String inputDirectory;
    private final String inputFilenamePattern;
    private final boolean inputRecursive;
    private final Double gridResolution;

    public AbnormalStatBuilderAppTestModule(String outputFilename, String inputDirectory, String inputFilenamePattern, boolean inputRecursive, Double gridResolution) {
        this.outputFilename = outputFilename;
        this.inputDirectory = inputDirectory;
        this.inputFilenamePattern = inputFilenamePattern;
        this.inputRecursive = inputRecursive;
        this.gridResolution = gridResolution;
    }

    @Override
    public void configure() {
        install(new FactoryModuleBuilder()
                .implement(PacketHandler.class, PacketHandlerImpl.class)
                .build(PacketHandlerFactory.class));

        bind(StripedExecutorService.class).in(Singleton.class);
        bind(AbnormalStatBuilderApp.class).in(Singleton.class);
        bind(AppStatisticsService.class).to(AppStatisticsServiceImpl.class).in(Singleton.class);
        bind(dk.dma.ais.abnormal.application.statistics.AppStatisticsService.class).to(AppStatisticsServiceImpl.class).in(Singleton.class);
        bind(Tracker.class).to(EventEmittingTracker.class).in(Singleton.class);
        bind(ShipTypeAndSizeStatistic.class);

        // Test stubs
        // bind(StatisticDataRepository.class).to(StatisticDataRepositoryTestStub);
    }

    @Provides
    @Singleton
    Configuration provideConfiguration() {
        return new BaseConfiguration();
    }

    @Provides
    @Singleton
    Grid provideGrid() {
        Grid grid = null;
        try {
            grid = Grid.createSize(gridResolution);
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
            LOG.info("Using dbFileName: " + outputFilename);
            statisticsRepository = new StatisticDataRepositoryMapDB(outputFilename);
            statisticsRepository.openForWrite(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statisticsRepository;
    }

    @Provides
    AisReader provideAisReader() {
        AisReader aisReader = null;
        try {
            aisReader = AisReaders.createDirectoryReader(inputDirectory, inputFilenamePattern, inputRecursive);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return aisReader;
    }

}
