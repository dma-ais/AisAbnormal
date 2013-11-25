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

import com.google.inject.Guice;
import com.google.inject.Injector;
import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AbnormalStatBuilderAppTest {

    /*
     * Test that the appStatistics bean injected in app and features is indeed the same instance.
     * If dependency injection is done wrong, different instances may be used.
     */
    @Test
    public void testApplicationStatistics() throws Exception {
        File tempFile = File.createTempFile("ais-ab-stat-builder", "");
        String[] args = new String[]{"-dir" ,"src/test/resources", "-input", "ais-sample-micro.txt.gz", "-output", tempFile.getCanonicalPath()};

        Injector injector = Guice.createInjector(new AbnormalStatBuilderAppTestModule(tempFile.getCanonicalPath()));
        AbnormalStatBuilderApp.setInjector(injector);
        AbnormalStatBuilderApp app = injector.getInstance(AbnormalStatBuilderApp.class);

        app.execute(args);

        AppStatisticsService appStatistics = app.getAppStatisticsService();

        assertEquals(9, appStatistics.getMessageCount());
        assertEquals(8, appStatistics.getPosMsgCount());
        assertEquals(1, appStatistics.getStatMsgCount());
    }

    @Test
    public void testFeatureStatistics() throws Exception {
        File tempFile = File.createTempFile("ais-ab-stat-builder", "");
        String[] args = new String[]{"-dir" ,"src/test/resources", "-input", "ais-sample-micro.txt.gz", "-output", tempFile.getCanonicalPath()};

        Injector injector = Guice.createInjector(new AbnormalStatBuilderAppTestModule(tempFile.getCanonicalPath()));
        AbnormalStatBuilderApp.setInjector(injector);
        AbnormalStatBuilderApp app = injector.getInstance(AbnormalStatBuilderApp.class);

        app.execute(args);

        AppStatisticsService appStatistics = app.getAppStatisticsService();

        assertEquals((Long) 8L, appStatistics.getFeatureStatistics("ShipTypeAndSizeFeature","Events processed"));
    }

    @Test
    public void testMetadataWrittenToDatabase() throws Exception {
        File tempFile = File.createTempFile("ais-ab-stat-builder", "");
        String[] args = new String[]{"-dir" ,"src/test/resources", "-input", "ais-sample-micro.txt.gz", "-output", tempFile.getCanonicalPath()};

        Injector injector = Guice.createInjector(new AbnormalStatBuilderAppTestModule(tempFile.getCanonicalPath()));
        AbnormalStatBuilderApp.setInjector(injector);
        AbnormalStatBuilderApp app = injector.getInstance(AbnormalStatBuilderApp.class);

        app.execute(args);

        FeatureDataRepository featureDataRepository = injector.getInstance(FeatureDataRepository.class);

        assertNotNull(featureDataRepository.getMetaData());
        assertEquals((Double) 200.0, featureDataRepository.getMetaData().getGridResolution());
    }

}
