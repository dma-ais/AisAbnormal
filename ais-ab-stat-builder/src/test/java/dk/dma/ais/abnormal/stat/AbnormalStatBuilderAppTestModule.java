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
        bind(AbnormalStatBuilderApp.class).in(Singleton.class);
        bind(PacketHandler.class).to(PacketHandlerImpl.class).in(Singleton.class);
        bind(AppStatisticsService.class).to(AppStatisticsServiceImpl.class).in(Singleton.class);
        bind(TrackingService.class).to(TrackingServiceImpl.class).in(Singleton.class);
        bind(ShipTypeAndSizeFeature.class);

        // Test stubs
        // bind(FeatureDataRepository.class).to(FeatureDataRepositoryTestStub);

    }

    @Provides @Singleton
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
    FeatureDataRepository provideFeatureDataRepository() {
        FeatureDataRepository featureDataRepository = null;
        try {
            LOG.info("Using dbFileName: " + outputFilename);
            featureDataRepository = new FeatureDataRepositoryMapDB(outputFilename, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return featureDataRepository;
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
