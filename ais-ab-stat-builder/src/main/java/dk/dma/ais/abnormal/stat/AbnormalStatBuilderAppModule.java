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
import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import dk.dma.ais.abnormal.stat.db.mapdb.FeatureDataRepositoryMapDB;
import dk.dma.ais.abnormal.stat.features.ShipTypeAndSizeFeature;
import dk.dma.ais.abnormal.stat.tracker.TrackingService;
import dk.dma.ais.abnormal.stat.tracker.TrackingServiceImpl;
import dk.dma.ais.reader.AisReader;
import dk.dma.ais.reader.AisReaders;
import dk.dma.enav.model.geometry.grid.Grid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AbnormalStatBuilderAppModule extends AbstractModule {
    private static Logger LOG = LoggerFactory.getLogger(AbnormalStatBuilderAppModule.class);

    private final String outputFilename;
    private final String inputDirectory;
    private final String inputFilenamePattern;
    private final boolean inputRecursive;
    private final Integer gridSize;

    public AbnormalStatBuilderAppModule(String outputFilename, String inputDirectory, String inputFilenamePattern, boolean inputRecursive, Integer gridSize) {
        this.outputFilename = outputFilename;
        this.inputDirectory = inputDirectory;
        this.inputFilenamePattern = inputFilenamePattern;
        this.inputRecursive = inputRecursive;
        this.gridSize = gridSize;
    }

    @Override
    public void configure() {
        bind(AbnormalStatBuilderApp.class).in(Singleton.class);
        bind(PacketHandler.class).to(PacketHandlerImpl.class).in(Singleton.class);
        bind(AppStatisticsService.class).to(AppStatisticsServiceImpl.class).in(Singleton.class);
        bind(TrackingService.class).to(TrackingServiceImpl.class).in(Singleton.class);
        bind(ShipTypeAndSizeFeature.class);
    }

    @Provides @Singleton
    Grid provideGrid() {
        Grid grid = null;
        try {
            grid = Grid.createSize(gridSize);
        } catch (Exception e) {
            LOG.error("Failed to create Grid object", e);
        }
        return grid;
    }

    @Provides @Singleton
    FeatureDataRepository provideFeatureDataRepository() {
        FeatureDataRepository featureDataRepository = null;
        try {
            LOG.info("Using dbFileName: " + outputFilename);
            featureDataRepository = new FeatureDataRepositoryMapDB(outputFilename, false);
        } catch (Exception e) {
            LOG.error("Failed to create FeatureDataRepository object", e);
        }
        return featureDataRepository;
    }

    @Provides
    AisReader provideAisReader() {
        AisReader aisReader = null;
        try {
            aisReader = AisReaders.createDirectoryReader(inputDirectory, inputFilenamePattern, inputRecursive);
        } catch (Exception e) {
            LOG.error("Failed to create AisReader object", e);
        }
        return aisReader;
    }

}
