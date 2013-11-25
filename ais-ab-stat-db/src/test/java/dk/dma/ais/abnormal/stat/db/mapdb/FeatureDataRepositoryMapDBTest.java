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

package dk.dma.ais.abnormal.stat.db.mapdb;

import dk.dma.ais.abnormal.stat.db.FeatureDataRepository;
import dk.dma.ais.abnormal.stat.db.data.FeatureData;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

public class FeatureDataRepositoryMapDBTest {

    static final Logger LOG = LoggerFactory.getLogger(FeatureDataRepositoryMapDB.class);

    // 10e6 giver p fil på 8719932942 på 16 min
    // 10e3 giver p fil på 8579854 på 2-3sek
    static final long NUM_CELLS = (long) 10e3;

    static final long ONE_PCT_OF_CELLS = NUM_CELLS / 100;
    static final short N1 = 10;
    static final short N2 = 10;
    static final long N_PROD = NUM_CELLS* N1 * N2;

    static final String TEST_FEATURE_NAME = "testFeature";
    
    private static String dbFileName;

    @BeforeClass
    public static void writeSomeTestData() throws Exception {
        String tmpFilePath = getTempFilePath();
        dbFileName = tmpFilePath + "/" + UUID.randomUUID();
        LOG.info("dbFileName: " + dbFileName);

        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName, false);

        LOG.info("Generating " + N_PROD + " test data... ");
        for (long cellId = 0; cellId < NUM_CELLS; cellId++) {
            FeatureData featureData = new FeatureData();

            for (short key1 = 0; key1 < N1; key1++) {
                for (short key2 = 0; key2 < N2; key2++) {
                    featureData.setStatistic(key1, key2, "t", (key1*key2) % 100);
                }
            }

            featureDataRepository.put(TEST_FEATURE_NAME, cellId, featureData);

            if (cellId % ONE_PCT_OF_CELLS == 0) {
                LOG.info(100 * cellId / NUM_CELLS + "%");
                //printMemInfo();
            }
        }
        LOG.info("done.");

        LOG.info("Closing repository... ");
        featureDataRepository.close();
        LOG.info("done.");
    }

    @Test
    public void testGetFeatureDataCell1() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 7;
        final short key1 = N1 - 1, key2 = N2 - 2;

        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName, true);

        FeatureData featureData = featureDataRepository.get(TEST_FEATURE_NAME, testCellId);
        assertEquals(N1, (int) featureData.getNumberOfLevel1Entries());
        assertEquals((1*1)%100, featureData.getStatistic((short) 1, (short) 1, "t"));
        assertEquals((7*7)%100, featureData.getStatistic((short) 7, (short) 7, "t"));
        assertEquals((9*8)%100, featureData.getStatistic((short) 9, (short) 8, "t"));
        assertEquals((key1 * key2) % 100, featureData.getStatistic(key1, key2, "t"));
        assertNull(featureData.getStatistic((short) (N1 + 1), (short) 4, "t"));
        assertNull(featureData.getStatistic((short) 1, (short) (N2 + 2), "t"));
        assertNull(featureData.getStatistic((short) 8, (short) 9, "wrongt"));
    }

    @Test
    public void testGetFeatureDataCell2() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 7;
        final short key1 = N1 - 1, key2 = N2 - 3;

        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName, true);

        FeatureData featureData = featureDataRepository.get(TEST_FEATURE_NAME, testCellId);
        assertEquals(N1, (int) featureData.getNumberOfLevel1Entries());
        assertEquals((1*1) % 100, featureData.getStatistic((short) 1, (short) 1, "t"));
        assertEquals((7*7) % 100, featureData.getStatistic((short) 7, (short) 7, "t"));
        assertEquals((key1 * key2) % 100, featureData.getStatistic(key1, key2, "t"));
        assertNull(featureData.getStatistic((short) (N1 + 1), (short) 4, "t"));
        assertNull(featureData.getStatistic((short) 1, (short) (N2 +2), "t"));
        assertNull(featureData.getStatistic((short) 8, (short) 9, "wrongt"));
    }

    @Test
    public void testUpdateFeatureDataStatistic() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 97;
        final short key1 = N1 - 1, key2 = N2 - 2;

        LOG.info("Getting FeatureData and verifying original contents");
        FeatureDataRepository featureDataRepository1 = new FeatureDataRepositoryMapDB(dbFileName, false);
        FeatureData featureData1 = featureDataRepository1.get(TEST_FEATURE_NAME, testCellId);
        assertEquals((key1*key2) % 100, featureData1.getStatistic(key1, key2, "t"));
        LOG.info("Updating FeatureData");
        featureData1.setStatistic(key1, key2, "t", 2157);
        featureDataRepository1.put(TEST_FEATURE_NAME, testCellId, featureData1);
        LOG.info("Closing repository");
        featureDataRepository1.close();
        featureDataRepository1 = null;
        LOG.info("Done");

        LOG.info("Opening repository");
        FeatureDataRepository featureDataRepository2 = new FeatureDataRepositoryMapDB(dbFileName, true);
        LOG.info("Reading FeatureData");
        FeatureData featureData2 = featureDataRepository2.get(TEST_FEATURE_NAME, testCellId);
        LOG.info("Checking that value is updated");
        assertEquals(2157, featureData2.getStatistic(key1, key2, "t"));
        LOG.info("Done. Closing repository.");
        featureDataRepository2.close();
        featureDataRepository2 = null;
    }

    @Test
    public void testAddFeatureDataStatistic() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 96;
        final short key1 = N1 - 1, key2 = N2 - 2;

        LOG.info("Getting FeatureData and verifying original contents");
        FeatureDataRepository featureDataRepository1 = new FeatureDataRepositoryMapDB(dbFileName, false);
        FeatureData featureData1 = featureDataRepository1.get(TEST_FEATURE_NAME, testCellId);
        assertNull(featureData1.getStatistic(key1, key2, "newStatistic"));
        LOG.info("Adding FeatureData statistic");
        featureData1.setStatistic(key1, key2, "newStatistic", 43287);
        featureDataRepository1.put(TEST_FEATURE_NAME, testCellId, featureData1);
        LOG.info("Closing repository");
        featureDataRepository1.close();
        featureDataRepository1 = null;
        LOG.info("Done");

        LOG.info("Opening repository");
        FeatureDataRepository featureDataRepository2 = new FeatureDataRepositoryMapDB(dbFileName, true);
        LOG.info("Reading FeatureData");
        FeatureData featureData2 = featureDataRepository2.get(TEST_FEATURE_NAME, testCellId);
        LOG.info("Checking that statistic is added");
        assertEquals(43287, featureData2.getStatistic(key1, key2, "newStatistic"));
        assertEquals((key1 * key2) % 100, featureData2.getStatistic(key1, key2, "t"));
        LOG.info("Done. Closing repository.");
        featureDataRepository2.close();
        featureDataRepository2 = null;
    }

    @Test
    public void testFeatureNames() throws Exception {
        LOG.info("Opening datastore");
        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName, true);
        LOG.info("Done.");

        LOG.info("Gettings feature names");
        Set<String> featureNames = featureDataRepository.getFeatureNames();
        for (String featureName : featureNames) {
            LOG.info("   Feature name: " + featureName);
        }
        LOG.info("Found " + featureNames.size() + " feature names.");

        assertEquals(1, featureNames.size());
        assertEquals(TEST_FEATURE_NAME, featureNames.toArray(new String[0])[0]);
    }

    private static String getTempFilePath() {
        String tempFilePath = null;

        try {
            File temp = File.createTempFile("tmp-", ".tmp");

            String absolutePath = temp.getAbsolutePath();
            tempFilePath = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));
        } catch(IOException e){
            e.printStackTrace();
        }

        return tempFilePath;
    }

}
