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

import dk.dma.ais.abnormal.event.db.csv.CsvEventRepository;
import dk.dma.ais.abnormal.event.db.jpa.JpaEventRepository;
import dk.dma.ais.filter.GeoMaskFilter;
import dk.dma.enav.model.geometry.BoundingBox;
import dk.dma.enav.model.geometry.grid.Grid;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_APPL_GRID_RESOLUTION_DEFAULT;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_EVENTS_CSV_FILE;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_EVENTS_H2_FILE;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_EVENTS_REPOSITORY_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AbnormalAnalyzerAppModuleTest {

    private PropertiesConfiguration configuration;
    private AbnormalAnalyzerAppModule sut;

    @Before
    public void config() {
        configuration = new PropertiesConfiguration();
        sut = new AbnormalAnalyzerAppModule(null) {
            @Override
            Configuration getConfiguration() {
                return configuration;
            }
        };
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotProvideUnknownEventRepositoryH2() throws IOException {
        configuration.addProperty(CONFKEY_EVENTS_REPOSITORY_TYPE, "unknown");
        sut.provideEventRepository().getClass();
    }

    @Test
    public void canProvideCsvEventRepository() throws IOException {
        Files.deleteIfExists(Paths.get("events.csv"));
        configuration.addProperty(CONFKEY_EVENTS_REPOSITORY_TYPE, "csv");
        configuration.addProperty(CONFKEY_EVENTS_CSV_FILE, "events.csv");
        assertEquals(CsvEventRepository.class, sut.provideEventRepository().getClass());
    }

    @Test
    public void canProvideJpaEventRepositoryH2() throws IOException {
        configuration.addProperty(CONFKEY_EVENTS_REPOSITORY_TYPE, "h2");
        configuration.addProperty(CONFKEY_EVENTS_H2_FILE, "h2file");
        assertEquals(JpaEventRepository.class, sut.provideEventRepository().getClass());
    }

    @Test(expected = NullPointerException.class) /* Tests that 'pgsql' is accepted as config value */
    public void canProvideJpaEventRepositoryPgsql() throws IOException {
        configuration.addProperty(CONFKEY_EVENTS_REPOSITORY_TYPE, "pgsql");
        /*    Requires pgsql online
        configuration.addProperty(CONFKEY_EVENTS_PGSQL_PORT, "9999");
        configuration.addProperty(CONFKEY_EVENTS_PGSQL_NAME, "pgsql");
        configuration.addProperty(CONFKEY_EVENTS_PGSQL_USERNAME, "pgsql");
        configuration.addProperty(CONFKEY_EVENTS_PGSQL_PASSWORD, "pgsql");
        */
        assertEquals(JpaEventRepository.class, sut.provideEventRepository().getClass());
    }

    @Test
    public void canProvideGridWithDefaultResolution() {
        Grid grid = sut.provideGrid();
        assertEquals(200.0, grid.getResolution(), 1e-6);
    }

    @Test
    public void canProvideGridWithResolutionFromConfigFile() {
        configuration.addProperty(CONFKEY_APPL_GRID_RESOLUTION_DEFAULT, 149);
        Grid grid = sut.provideGrid();
        assertEquals(149, grid.getResolution(), 1e-6);
    }

    @Test
    public void canReadXmlResourceForGeoMaskFilter() throws Exception {
        URL configResource = this.getClass().getClassLoader().getResource("analyzer.properties");
        AbnormalAnalyzerAppModule appModule = new AbnormalAnalyzerAppModule(new File(configResource.toURI()).toPath());

        GeoMaskFilter geoMaskFilter = appModule.provideGeoMaskFilter();
        List<BoundingBox> boundingBoxes = geoMaskFilter.getSuppressedBoundingBoxes();

        assertNotNull(boundingBoxes);
        assertEquals(258, boundingBoxes.size());

        assertEquals(56.13767740, boundingBoxes.get(0).getMinLat(), 1e-8);
        assertEquals(56.17285020, boundingBoxes.get(0).getMaxLat(), 1e-8);
        assertEquals(10.20465090, boundingBoxes.get(0).getMinLon(), 1e-8);
        assertEquals(10.26770390, boundingBoxes.get(0).getMaxLon(), 1e-8);

        assertEquals(55.55393080, boundingBoxes.get(1).getMinLat(), 1e-8);
        assertEquals(55.56227290, boundingBoxes.get(1).getMaxLat(), 1e-8);
        assertEquals( 9.72816210, boundingBoxes.get(1).getMinLon(), 1e-8);
        assertEquals( 9.76877229, boundingBoxes.get(1).getMaxLon(), 1e-8);
    }
}