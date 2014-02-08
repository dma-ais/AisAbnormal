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
import dk.dma.ais.abnormal.stat.db.data.ShipTypeAndSizeFeatureData;
import dk.dma.ais.abnormal.util.Categorizer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FeatureDataRepositoryMapDBTest {
    static final Logger LOG = LoggerFactory.getLogger(FeatureDataRepositoryMapDB.class);

    // 10e6 giver p fil på 8719932942 på 16 min
    // 10e3 giver p fil på 8579854 på 2-3sek
    static final long NUM_CELLS = (long) 1e3;

    static final long ONE_PCT_OF_CELLS = NUM_CELLS / 100;
    static final int N1 = Categorizer.NUM_SHIP_TYPE_CATEGORIES - 1;
    static final int N2 = Categorizer.NUM_SHIP_SIZE_CATEGORIES - 1;
    static final long N_PROD = NUM_CELLS * N1 * N2;

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
        final int key1 = N1 - 1, key2 = N2 - 2;

        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository.openForWrite(false);

        ShipTypeAndSizeFeatureData featureData = (ShipTypeAndSizeFeatureData) featureDataRepository.getFeatureData(TEST_FEATURE_NAME, testCellId);
        assertEquals((Integer) (1 % 100), featureData.getValue(1, 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertEquals((Integer) ((7*4) % 100), featureData.getValue(7, 4, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertEquals((Integer) ((6*3)%100), featureData.getValue(6, 3, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertEquals((Integer) ((key1 * key2) % 100), featureData.getValue(key1, key2, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));

        featureDataRepository.close();
    }

    @Test
    public void testGetFeatureDataCell2() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 7;
        final int key1 = N1 - 1, key2 = N2 - 3;

        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository.openForWrite(false);

        ShipTypeAndSizeFeatureData featureData = (ShipTypeAndSizeFeatureData) featureDataRepository.getFeatureData(TEST_FEATURE_NAME, testCellId);
        assertEquals((Integer)  (  1  % 100), featureData.getValue(1, 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertEquals((Integer) ((4*2) % 100), featureData.getValue(4, 2, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertEquals((Integer) ((key1 * key2) % 100), featureData.getValue(key1, key2, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));

        featureDataRepository.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFeatureDataIllegalKey1() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 7;

        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository.openForWrite(false);

        ShipTypeAndSizeFeatureData featureData = (ShipTypeAndSizeFeatureData) featureDataRepository.getFeatureData(TEST_FEATURE_NAME, testCellId);
        try {
            featureData.getValue(N1 + 1, 4, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT);
        } finally {
            featureDataRepository.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFeatureDataIllegalKey2() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 7;

        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository.openForWrite(false);

        ShipTypeAndSizeFeatureData featureData = (ShipTypeAndSizeFeatureData) featureDataRepository.getFeatureData(TEST_FEATURE_NAME, testCellId);
        try {
            featureData.getValue(7, N2 + 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT);
        } finally {
            featureDataRepository.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFeatureDataIllegalStatisticsName() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 7;
        final int key1 = N1 - 1, key2 = N2 - 3;

        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository.openForWrite(false);

        ShipTypeAndSizeFeatureData featureData = (ShipTypeAndSizeFeatureData) featureDataRepository.getFeatureData(TEST_FEATURE_NAME, testCellId);
        try {
            featureData.getValue(key1, key2, "wrongt");
        } finally {
            featureDataRepository.close();
        }
    }

    @Test
    public void testUpdateFeatureDataStatistic() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 97;
        final int key1 = N1 - 1, key2 = N2 - 2;

        LOG.info("Getting FeatureData and verifying original contents");
        FeatureDataRepository featureDataRepository1 = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository1.openForWrite(false);
        ShipTypeAndSizeFeatureData featureData1 = (ShipTypeAndSizeFeatureData) featureDataRepository1.getFeatureData(TEST_FEATURE_NAME, testCellId);
        assertEquals((Integer) ((key1 * key2) % 100), featureData1.getValue(key1, key2, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        LOG.info("Updating FeatureData");
        featureData1.setValue(key1, key2, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT, 2157);
        featureDataRepository1.putFeatureData(TEST_FEATURE_NAME, testCellId, featureData1);
        LOG.info("Closing repository");
        featureDataRepository1.close();
        LOG.info("Done");

        LOG.info("Opening repository");
        FeatureDataRepository featureDataRepository2 = new FeatureDataRepositoryMapDB(dbFileName);
        featureDataRepository2.openForRead();
        LOG.info("Reading FeatureData");
        ShipTypeAndSizeFeatureData featureData2 = (ShipTypeAndSizeFeatureData) featureDataRepository2.getFeatureData(TEST_FEATURE_NAME, testCellId);
        LOG.info("Checking that value is updated");
        assertEquals((Integer) 2157, featureData2.getValue(key1, key2, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
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

        Set<Long> allCellsWithData = featureDataRepository.getCellsWithData(testFeatureName);
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
        final int key1 = N1 - 1, key2 = N2 - 2;

        ShipTypeAndSizeFeatureData featureData = (ShipTypeAndSizeFeatureData) featureDataRepository1.getFeatureData(TEST_FEATURE_NAME, testCellId);
        assertEquals((Integer)(   1 %100), featureData.getValue(1, 1, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertEquals((Integer)((7*4)%100), featureData.getValue(7, 4, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertEquals((Integer)((6*3)%100), featureData.getValue(6, 3, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
        assertEquals((Integer)((key1 * key2) % 100), featureData.getValue(key1, key2, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT));
    }

    @Test
    public void testPrepareBackupDBFile() throws IOException {
        // Prepare test data
        String tmpFilePath = getTempFilePath();
        String databaseName = UUID.randomUUID().toString();

        String dbFileName = tmpFilePath + "/" + databaseName + ".featureData";
        String dbBackupFileName = tmpFilePath + "/" + databaseName + ".backup.featureData";
        String dbPrevBackupFileName = tmpFilePath + "/" + databaseName + ".backup.previous.featureData";

        String dbPFileName = dbFileName.concat(".p");
        String dbBackupPFileName = dbBackupFileName.concat(".p");
        String dbPrevBackupPFileName = dbPrevBackupFileName.concat(".p");

        File dbFile = new File(dbFileName);
        File dbBackupFile = new File(dbBackupFileName);
        File dbPrevBackupFile = new File(dbPrevBackupFileName);

        File dbPFile = new File(dbPFileName);
        File dbBackupPFile = new File(dbBackupPFileName);
        File dbPrevBackupPFile = new File(dbPrevBackupPFileName);

        LOG.debug("dbFileName: " + dbFileName);
        LOG.debug("dbBackupFileName: " + dbBackupFileName);
        LOG.debug("dbPrevBackupFileName: " + dbPrevBackupFileName);

        // Assert start condition
        assertFalse(dbFile.exists());
        assertFalse(dbPFile.exists());
        assertFalse(dbBackupFile.exists());
        assertFalse(dbBackupPFile.exists());
        assertFalse(dbPrevBackupFile.exists());
        assertFalse(dbPrevBackupPFile.exists());

        // Test backup file can be created
        dbBackupFile = FeatureDataRepositoryMapDB.prepareBackupDBFileFor(dbFile);
        assertNotNull(dbBackupFile);
        assertFalse(dbBackupFile.exists());
        LOG.debug("dbBackupFile: " + dbBackupFile.getAbsolutePath());
        dbBackupFile.createNewFile();
        dbBackupPFile.createNewFile();
        assertTrue(dbBackupFile.exists());
        assertTrue(dbBackupFile.isFile());
        assertEquals(dbBackupFileName, dbBackupFile.getAbsolutePath());
        assertFalse(dbPrevBackupFile.exists());

        // Test 1st file rotation
        dbBackupFile = FeatureDataRepositoryMapDB.prepareBackupDBFileFor(dbFile);
        assertFalse(dbBackupFile.exists());
        assertTrue(dbPrevBackupFile.exists());
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
            ShipTypeAndSizeFeatureData featureData = ShipTypeAndSizeFeatureData.create();

            for (int key1 = 0; key1 <= N1; key1++) {
                for (int key2 = 0; key2 <= N2; key2++) {
                    featureData.setValue(key1, key2, ShipTypeAndSizeFeatureData.STAT_SHIP_COUNT, (key1 * key2) % 100);
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
