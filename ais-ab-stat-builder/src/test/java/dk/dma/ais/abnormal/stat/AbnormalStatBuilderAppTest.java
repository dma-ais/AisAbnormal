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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
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
        String outputFilename = tempFile.getCanonicalPath();
        String inputDirectory = "src/test/resources";
        String inputFilenamePattern = "ais-sample-micro.txt.gz";
        String[] args = new String[]{"-inputDirectory", inputDirectory, "-input", inputFilenamePattern, "-output", outputFilename};

        Injector injector = Guice.createInjector(new AbnormalStatBuilderAppTestModule(tempFile.getCanonicalPath(), inputDirectory, inputFilenamePattern, false, 200.0));
        AbnormalStatBuilderApp.setInjector(injector);
        AbnormalStatBuilderApp app = injector.getInstance(AbnormalStatBuilderApp.class);
        AbnormalStatBuilderApp.userArguments = parseUserArguments(args);

        app.execute(new String[]{});

        AppStatisticsService appStatistics = injector.getInstance(AppStatisticsService.class);
        assertEquals(9, appStatistics.getMessageCount());
        assertEquals(8, appStatistics.getPosMsgCount());
        assertEquals(1, appStatistics.getStatMsgCount());
    }

    @Test
    public void testFeatureStatistics() throws Exception {
        File tempFile = File.createTempFile("ais-ab-stat-builder", "");
        String outputFilename = tempFile.getCanonicalPath();
        String inputDirectory = "src/test/resources";
        String inputFilenamePattern = "ais-sample-micro.txt.gz";
        String[] args = new String[]{"-inputDirectory", inputDirectory, "-input", inputFilenamePattern, "-output", outputFilename};

        Injector injector = Guice.createInjector(new AbnormalStatBuilderAppTestModule(tempFile.getCanonicalPath(), inputDirectory, inputFilenamePattern, false, 200.0));
        AbnormalStatBuilderApp.setInjector(injector);
        AbnormalStatBuilderApp app = injector.getInstance(AbnormalStatBuilderApp.class);

        AbnormalStatBuilderApp.userArguments = parseUserArguments(args);
        app.execute(new String[]{});

        AppStatisticsService appStatistics = injector.getInstance(AppStatisticsService.class);
        assertEquals((Long) 8L, appStatistics.getFeatureStatistics("ShipTypeAndSizeFeature", "Events processed"));
    }

    @Test
    public void testMetadataWrittenToDatabase() throws Exception {
        File tempFile = File.createTempFile("ais-ab-stat-builder", "");
        String outputFilename = tempFile.getCanonicalPath();
        String inputDirectory = "src/test/resources";
        String inputFilenamePattern = "ais-sample-micro.txt.gz";
        String[] args = new String[]{"-inputDirectory", inputDirectory, "-input", inputFilenamePattern, "-output", outputFilename};

        Injector injector = Guice.createInjector(new AbnormalStatBuilderAppTestModule(tempFile.getCanonicalPath(), inputDirectory, inputFilenamePattern, false, 200.0));
        AbnormalStatBuilderApp.setInjector(injector);
        AbnormalStatBuilderApp app = injector.getInstance(AbnormalStatBuilderApp.class);

        AbnormalStatBuilderApp.userArguments = parseUserArguments(args);
        app.execute(new String[]{});

        // Repo is closed by app. Get a new one.
        Injector injector2 = Guice.createInjector(new AbnormalStatBuilderAppTestModule(tempFile.getCanonicalPath(), inputDirectory, inputFilenamePattern, false, 200.0));
        FeatureDataRepository featureDataRepository = injector2.getInstance(FeatureDataRepository.class);

        assertNotNull(featureDataRepository.getMetaData());
        assertEquals((Double) 0.0017966313162819712 /* res 200.0 */, featureDataRepository.getMetaData().getGridResolution(), 1e-10);
        assertEquals((Integer) 60, featureDataRepository.getMetaData().getDownsampling());
        assertEquals((Short) (short) 1, featureDataRepository.getMetaData().getFormatVersion());
    }

    private static UserArguments parseUserArguments(String[] args) {
        UserArguments userArguments = new UserArguments();
        try {
            new JCommander(userArguments, args);
        } catch (ParameterException e) {
            e.printStackTrace(System.err);
            new JCommander(userArguments, new String[] { "-help" }).usage();
        }
        return userArguments;
    }

}
