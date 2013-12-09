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
import dk.dma.ais.abnormal.stat.db.data.DatasetMetaData;
import dk.dma.ais.abnormal.stat.db.data.FeatureData;
import dk.dma.ais.abnormal.stat.db.data.FeatureData2Key;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class FeatureDataRepositoryMapDBTest {
    static final Logger LOG = LoggerFactory.getLogger(FeatureDataRepositoryMapDB.class);

    // 10e6 giver p fil på 8719932942 på 16 min
    // 10e3 giver p fil på 8579854 på 2-3sek
    static final long NUM_CELLS = (long) 1e3;

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

        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository.openForWrite(false);

        writeTestDataToRepository(featureDataRepository);

        LOG.info("Closing repository... ");
        featureDataRepository.close();
        LOG.info("done.");
    }

    @Test
    public void testGetFeatureDataCell1() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 7;
        final short key1 = N1 - 1, key2 = N2 - 2;

        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository.openForWrite(false);

        FeatureData2Key featureData = (FeatureData2Key) featureDataRepository.getFeatureData(TEST_FEATURE_NAME, testCellId);
        assertEquals(N1, (int) featureData.numberOfLevel1Entries());
        assertEquals(     1  % 100, featureData.getStatistic((short) 1, (short) 1, "t"));
        assertEquals((7 * 7) % 100, featureData.getStatistic((short) 7, (short) 7, "t"));
        assertEquals((9*8)%100, featureData.getStatistic((short) 9, (short) 8, "t"));
        assertEquals((key1 * key2) % 100, featureData.getStatistic(key1, key2, "t"));
        assertNull(featureData.getStatistic((short) (N1 + 1), (short) 4, "t"));
        assertNull(featureData.getStatistic((short) 1, (short) (N2 + 2), "t"));
        assertNull(featureData.getStatistic((short) 8, (short) 9, "wrongt"));

        featureDataRepository.close();
    }

    @Test
    public void testGetFeatureDataCell2() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 7;
        final short key1 = N1 - 1, key2 = N2 - 3;

        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository.openForWrite(false);

        FeatureData2Key featureData = (FeatureData2Key) featureDataRepository.getFeatureData(TEST_FEATURE_NAME, testCellId);
        assertEquals(N1, (int) featureData.numberOfLevel1Entries());
        assertEquals(   1  % 100, featureData.getStatistic((short) 1, (short) 1, "t"));
        assertEquals((7*7) % 100, featureData.getStatistic((short) 7, (short) 7, "t"));
        assertEquals((key1 * key2) % 100, featureData.getStatistic(key1, key2, "t"));
        assertNull(featureData.getStatistic((short) (N1 + 1), (short) 4, "t"));
        assertNull(featureData.getStatistic((short) 1, (short) (N2 +2), "t"));
        assertNull(featureData.getStatistic((short) 8, (short) 9, "wrongt"));

        featureDataRepository.close();
    }

    @Test
    public void testUpdateFeatureDataStatistic() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 97;
        final short key1 = N1 - 1, key2 = N2 - 2;

        LOG.info("Getting FeatureData and verifying original contents");
        FeatureDataRepository featureDataRepository1 = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository1.openForWrite(false);
        FeatureData2Key featureData1 = (FeatureData2Key) featureDataRepository1.getFeatureData(TEST_FEATURE_NAME, testCellId);
        assertEquals((key1*key2) % 100, featureData1.getStatistic(key1, key2, "t"));
        LOG.info("Updating FeatureData");
        featureData1.setStatistic(key1, key2, "t", 2157);
        featureDataRepository1.putFeatureData(TEST_FEATURE_NAME, testCellId, featureData1);
        LOG.info("Closing repository");
        featureDataRepository1.close();
        LOG.info("Done");

        LOG.info("Opening repository");
        FeatureDataRepository featureDataRepository2 = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository2.openForRead();
        LOG.info("Reading FeatureData");
        FeatureData2Key featureData2 = (FeatureData2Key) featureDataRepository2.getFeatureData(TEST_FEATURE_NAME, testCellId);
        LOG.info("Checking that value is updated");
        assertEquals(2157, featureData2.getStatistic(key1, key2, "t"));
        LOG.info("Done. Closing repository.");
        featureDataRepository2.close();
    }

    @Test
    public void testAddFeatureDataStatistic() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 96;
        final short key1 = N1 - 1, key2 = N2 - 2;

        LOG.info("Getting FeatureData and verifying original contents");
        FeatureDataRepository featureDataRepository1 = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository1.openForWrite(false);
        FeatureData2Key featureData1 = (FeatureData2Key) featureDataRepository1.getFeatureData(TEST_FEATURE_NAME, testCellId);
        assertNull(featureData1.getStatistic(key1, key2, "newStatistic"));
        LOG.info("Adding FeatureData statistic");
        featureData1.setStatistic(key1, key2, "newStatistic", 43287);
        featureDataRepository1.putFeatureData(TEST_FEATURE_NAME, testCellId, featureData1);
        LOG.info("Closing repository");
        featureDataRepository1.close();
        LOG.info("Done");

        LOG.info("Opening repository");
        FeatureDataRepository featureDataRepository2 = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository2.openForRead();
        LOG.info("Reading FeatureData");
        FeatureData2Key featureData2 = (FeatureData2Key) featureDataRepository2.getFeatureData(TEST_FEATURE_NAME, testCellId);
        LOG.info("Checking that statistic is added");
        assertEquals(43287, featureData2.getStatistic(key1, key2, "newStatistic"));
        assertEquals((key1 * key2) % 100, featureData2.getStatistic(key1, key2, "t"));
        LOG.info("Done. Closing repository.");
        featureDataRepository2.close();
    }

    @Test
    public void testFeatureNames() throws Exception {
        LOG.info("Opening datastore");
        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository.openForRead();
        LOG.info("Done.");

        LOG.info("Gettings feature names");
        Set<String> featureNames = featureDataRepository.getFeatureNames();
        for (String featureName : featureNames) {
            LOG.info("   Feature name: " + featureName);
        }
        LOG.info("Found " + featureNames.size() + " feature names.");

        assertEquals(1, featureNames.size());
        assertEquals(TEST_FEATURE_NAME, featureNames.toArray(new String[1])[0]);

        featureDataRepository.close();
    }

    @Test
    public void testRepositoryCanBeReadInReadOnlyMode() throws Exception {
        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository.openForRead();

        DatasetMetaData metaData = featureDataRepository.getMetaData();
        assertNotNull(metaData);

        Set<String> featureNames = featureDataRepository.getFeatureNames();
        for (String featureName : featureNames) {
            long numberOfCells = featureDataRepository.getNumberOfCells(featureName);
            FeatureData featureData = featureDataRepository.getFeatureData(featureName, 1);

            assertTrue(numberOfCells >= 0);
            assertNotNull(featureData);
        }

        featureDataRepository.close();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRepositoryCannotBeWrittenInReadOnlyMode() throws Exception {
        // We cannot use same db file as for other tests, because this one will not .close() and therefore
        // probably corrupt checksums in the file. So we make our own dbFile:
        String tmpFilePath = getTempFilePath();
        String dbFileName = tmpFilePath + "/" + UUID.randomUUID() + ".featureData";
        LOG.debug("testRepositoryCannotBeWrittenInReadOnlyMode: dbFileName = " + dbFileName);
        File dbFile = new File(dbFileName);
        assertFalse(dbFile.exists());

        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository.openForRead();

        DatasetMetaData metaData = featureDataRepository.getMetaData();
        assertNotNull(metaData);

        Set<String> featureNames = featureDataRepository.getFeatureNames();
        assertTrue(featureNames.size() > 0);
        String testFeatureName = featureNames.iterator().next();

        Set<Long> allCellsWithData = featureDataRepository.getAllCellsWithData();
        assertTrue(allCellsWithData.size() > 0);
        Long testCellId = allCellsWithData.iterator().next();

        LOG.debug("Trying to write to feature "  + testFeatureName + ", cell " + testCellId);

        FeatureData featureData = featureDataRepository.getFeatureData(testFeatureName, testCellId);
        featureDataRepository.putFeatureData(testFeatureName, testCellId, featureData);
        assertTrue(false); // Should never get to here
    }

    @Test
    public void testInMemoryDumpToDiskOnClose() throws Exception {
        String tmpFilePath = getTempFilePath();
        String dbFileName = tmpFilePath + "/" + UUID.randomUUID() + ".featureData";
        LOG.debug("testInMemoryDumpToDiskOnClose: dbFileName = " + dbFileName);
        File dbFile = new File(dbFileName);
        assertFalse(dbFile.exists());

        // Create in-memory database
        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository.openForWrite(true);
        assertFalse(dbFile.exists());
        writeTestDataToRepository(featureDataRepository);
        assertFalse(dbFile.exists());
        featureDataRepository.close();
        assertTrue(dbFile.exists());

        // Re-open database from disk to check contents
        FeatureDataRepositoryMapDB featureDataRepository1 = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository1.openForRead();

        final long testCellId = (NUM_CELLS / 2) + 7;
        final short key1 = N1 - 1, key2 = N2 - 2;

        FeatureData2Key featureData = (FeatureData2Key) featureDataRepository1.getFeatureData(TEST_FEATURE_NAME, testCellId);
        assertEquals(N1, (int) featureData.numberOfLevel1Entries());
        assertEquals(   1 %100, featureData.getStatistic((short) 1, (short) 1, "t"));
        assertEquals((7*7)%100, featureData.getStatistic((short) 7, (short) 7, "t"));
        assertEquals((9*8)%100, featureData.getStatistic((short) 9, (short) 8, "t"));
        assertEquals((key1 * key2) % 100, featureData.getStatistic(key1, key2, "t"));
        assertNull(featureData.getStatistic((short) (N1 + 1), (short) 4, "t"));
        assertNull(featureData.getStatistic((short) 1, (short) (N2 + 2), "t"));
        assertNull(featureData.getStatistic((short) 8, (short) 9, "wrongt"));
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

    private static void writeTestDataToRepository(FeatureDataRepository featureDataRepository) {
        LOG.info("Generating " + N_PROD + " test data... ");
        for (long cellId = 0; cellId < NUM_CELLS; cellId++) {
            FeatureData2Key featureData = new FeatureData2Key(FeatureDataRepositoryMapDBTest.class, "key1", "key2");

            for (short key1 = 0; key1 < N1; key1++) {
                for (short key2 = 0; key2 < N2; key2++) {
                    featureData.setStatistic(key1, key2, "t", (key1*key2) % 100);
                }
            }

            featureDataRepository.putFeatureData(TEST_FEATURE_NAME, cellId, featureData);

            if (cellId % ONE_PCT_OF_CELLS == 0) {
                LOG.info(100 * cellId / NUM_CELLS + "%");
                //printMemInfo();
            }
        }
        LOG.info("done.");

        LOG.info("storing dataset metadata.");
        DatasetMetaData datasetMetaData = new DatasetMetaData(123.0, 60);
        featureDataRepository.putMetaData(datasetMetaData);
        LOG.info("done.");
    }

}
