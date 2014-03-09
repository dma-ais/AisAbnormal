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

import dk.dma.ais.abnormal.stat.db.StatisticDataRepository;
import dk.dma.ais.abnormal.stat.db.data.DatasetMetaData;
import dk.dma.ais.abnormal.stat.db.data.ShipTypeAndSizeStatisticData;
import dk.dma.ais.abnormal.stat.db.data.StatisticData;
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

public class StatisticDataRepositoryMapDBTest {
    static final Logger LOG = LoggerFactory.getLogger(StatisticDataRepositoryMapDB.class);

    // 10e6 giver p fil på 8719932942 på 16 min
    // 10e3 giver p fil på 8579854 på 2-3sek
    static final long NUM_CELLS = (long) 1e3;

    static final long ONE_PCT_OF_CELLS = NUM_CELLS / 100;
    static final int N1 = Categorizer.NUM_SHIP_TYPE_CATEGORIES - 1;
    static final int N2 = Categorizer.NUM_SHIP_SIZE_CATEGORIES - 1;
    static final long N_PROD = NUM_CELLS * N1 * N2;

    static final String TEST_STATISTIC_NAME = "testStatistic";
    
    private static String dbFileName;

    @BeforeClass
    public static void writeSomeTestData() throws Exception {
        String tmpFilePath = getTempFilePath();
        dbFileName = tmpFilePath + "/" + UUID.randomUUID();
        LOG.info("dbFileName: " + dbFileName);

        StatisticDataRepository statisticsRepository = new StatisticDataRepositoryMapDB(dbFileName);
        statisticsRepository.openForWrite(false);

        writeTestDataToRepository(statisticsRepository);

        LOG.info("Closing repository... ");
        statisticsRepository.close();
        LOG.info("done.");
    }

    @Test
    public void testGetStatisticDataCell1() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 7;
        final int key1 = N1 - 1, key2 = N2 - 2;

        StatisticDataRepository statisticsRepository = new StatisticDataRepositoryMapDB(dbFileName);
        statisticsRepository.openForWrite(false);

        ShipTypeAndSizeStatisticData statistics = (ShipTypeAndSizeStatisticData) statisticsRepository.getStatisticData(TEST_STATISTIC_NAME, testCellId);
        assertEquals((Integer) (1 % 100), statistics.getValue(1, 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertEquals((Integer) ((7*4) % 100), statistics.getValue(7, 4, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertEquals((Integer) ((6*3)%100), statistics.getValue(6, 3, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertEquals((Integer) ((key1 * key2) % 100), statistics.getValue(key1, key2, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));

        statisticsRepository.close();
    }

    @Test
    public void testGetStatisticDataCell2() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 7;
        final int key1 = N1 - 1, key2 = N2 - 3;

        StatisticDataRepository statisticsRepository = new StatisticDataRepositoryMapDB(dbFileName);
        statisticsRepository.openForWrite(false);

        ShipTypeAndSizeStatisticData statistics = (ShipTypeAndSizeStatisticData) statisticsRepository.getStatisticData(TEST_STATISTIC_NAME, testCellId);
        assertEquals((Integer)  (  1  % 100), statistics.getValue(1, 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertEquals((Integer) ((4*2) % 100), statistics.getValue(4, 2, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertEquals((Integer) ((key1 * key2) % 100), statistics.getValue(key1, key2, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));

        statisticsRepository.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetStatisticDataIllegalKey1() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 7;

        StatisticDataRepository statisticsRepository = new StatisticDataRepositoryMapDB(dbFileName);
        statisticsRepository.openForWrite(false);

        ShipTypeAndSizeStatisticData statistics = (ShipTypeAndSizeStatisticData) statisticsRepository.getStatisticData(TEST_STATISTIC_NAME, testCellId);
        try {
            statistics.getValue(N1 + 1, 4, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT);
        } finally {
            statisticsRepository.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetStatisticDataIllegalKey2() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 7;

        StatisticDataRepository statisticsRepository = new StatisticDataRepositoryMapDB(dbFileName);
        statisticsRepository.openForWrite(false);

        ShipTypeAndSizeStatisticData statistics = (ShipTypeAndSizeStatisticData) statisticsRepository.getStatisticData(TEST_STATISTIC_NAME, testCellId);
        try {
            statistics.getValue(7, N2 + 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT);
        } finally {
            statisticsRepository.close();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetStatisticDataIllegalStatisticsName() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 7;
        final int key1 = N1 - 1, key2 = N2 - 3;

        StatisticDataRepository statisticsRepository = new StatisticDataRepositoryMapDB(dbFileName);
        statisticsRepository.openForWrite(false);

        ShipTypeAndSizeStatisticData statistics = (ShipTypeAndSizeStatisticData) statisticsRepository.getStatisticData(TEST_STATISTIC_NAME, testCellId);
        try {
            statistics.getValue(key1, key2, "wrongt");
        } finally {
            statisticsRepository.close();
        }
    }

    @Test
    public void testUpdateStatisticDataStatistic() throws Exception {
        final long testCellId = (NUM_CELLS / 2) + 97;
        final int key1 = N1 - 1, key2 = N2 - 2;

        LOG.info("Getting StatisticData and verifying original contents");
        StatisticDataRepository statisticsRepository1 = new StatisticDataRepositoryMapDB(dbFileName);
        statisticsRepository1.openForWrite(false);
        ShipTypeAndSizeStatisticData statistics1 = (ShipTypeAndSizeStatisticData) statisticsRepository1.getStatisticData(TEST_STATISTIC_NAME, testCellId);
        assertEquals((Integer) ((key1 * key2) % 100), statistics1.getValue(key1, key2, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        LOG.info("Updating StatisticData");
        statistics1.setValue(key1, key2, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT, 2157);
        statisticsRepository1.putStatisticData(TEST_STATISTIC_NAME, testCellId, statistics1);
        LOG.info("Closing repository");
        statisticsRepository1.close();
        LOG.info("Done");

        LOG.info("Opening repository");
        StatisticDataRepository statisticsRepository2 = new StatisticDataRepositoryMapDB(dbFileName);
        statisticsRepository2.openForRead();
        LOG.info("Reading StatisticData");
        ShipTypeAndSizeStatisticData statistics2 = (ShipTypeAndSizeStatisticData) statisticsRepository2.getStatisticData(TEST_STATISTIC_NAME, testCellId);
        LOG.info("Checking that value is updated");
        assertEquals((Integer) 2157, statistics2.getValue(key1, key2, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        LOG.info("Done. Closing repository.");
        statisticsRepository2.close();
    }

    @Test
    public void testStatisticNames() throws Exception {
        LOG.info("Opening datastore");
        StatisticDataRepository statisticsRepository = new StatisticDataRepositoryMapDB(dbFileName);
        statisticsRepository.openForRead();
        LOG.info("Done.");

        LOG.info("Gettings statistic names");
        Set<String> statisticNames = statisticsRepository.getStatisticNames();
        for (String statisticName : statisticNames) {
            LOG.info("   Statistic name: " + statisticName);
        }
        LOG.info("Found " + statisticNames.size() + " statistic names.");

        assertEquals(1, statisticNames.size());
        assertEquals(TEST_STATISTIC_NAME, statisticNames.toArray(new String[1])[0]);

        statisticsRepository.close();
    }

    @Test
    public void testRepositoryCanBeReadInReadOnlyMode() throws Exception {
        StatisticDataRepository statisticsRepository = new StatisticDataRepositoryMapDB(dbFileName);
        statisticsRepository.openForRead();

        DatasetMetaData metaData = statisticsRepository.getMetaData();
        assertNotNull(metaData);

        Set<String> statisticNames = statisticsRepository.getStatisticNames();
        for (String statisticName : statisticNames) {
            long numberOfCells = statisticsRepository.getNumberOfCells(statisticName);
            StatisticData statistics = statisticsRepository.getStatisticData(statisticName, 1);

            assertTrue(numberOfCells >= 0);
            assertNotNull(statistics);
        }

        statisticsRepository.close();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRepositoryCannotBeWrittenInReadOnlyMode() throws Exception {
        // We cannot use same db file as for other tests, because this one will not .close() and therefore
        // probably corrupt checksums in the file. So we make our own dbFile:
        String tmpFilePath = getTempFilePath();
        String dbFileName = tmpFilePath + "/" + UUID.randomUUID() + ".statistics";
        LOG.debug("testRepositoryCannotBeWrittenInReadOnlyMode: dbFileName = " + dbFileName);
        File dbFile = new File(dbFileName);
        assertFalse(dbFile.exists());

        StatisticDataRepository statisticsRepository = new StatisticDataRepositoryMapDB(dbFileName);
        statisticsRepository.openForRead();

        DatasetMetaData metaData = statisticsRepository.getMetaData();
        assertNotNull(metaData);

        Set<String> statisticNames = statisticsRepository.getStatisticNames();
        assertTrue(statisticNames.size() > 0);
        String testStatisticName = statisticNames.iterator().next();

        Set<Long> allCellsWithData = statisticsRepository.getCellsWithData(testStatisticName);
        assertTrue(allCellsWithData.size() > 0);
        Long testCellId = allCellsWithData.iterator().next();

        LOG.debug("Trying to write to statistic "  + testStatisticName + ", cell " + testCellId);

        StatisticData statistics = statisticsRepository.getStatisticData(testStatisticName, testCellId);
        statisticsRepository.putStatisticData(testStatisticName, testCellId, statistics);
        assertTrue(false); // Should never get to here
    }

    @Test
    public void testInMemoryDumpToDiskOnClose() throws Exception {
        String tmpFilePath = getTempFilePath();
        String dbFileName = tmpFilePath + "/" + UUID.randomUUID() + ".statistics";
        LOG.debug("testInMemoryDumpToDiskOnClose: dbFileName = " + dbFileName);
        File dbFile = new File(dbFileName);
        assertFalse(dbFile.exists());

        // Create in-memory database
        StatisticDataRepository statisticsRepository = new StatisticDataRepositoryMapDB(dbFileName);
        statisticsRepository.openForWrite(true);
        assertFalse(dbFile.exists());
        writeTestDataToRepository(statisticsRepository);
        assertFalse(dbFile.exists());
        statisticsRepository.close();
        assertTrue(dbFile.exists());

        // Re-open database from disk to check contents
        StatisticDataRepositoryMapDB statisticsRepository1 = new StatisticDataRepositoryMapDB(dbFileName);
        statisticsRepository1.openForRead();

        final long testCellId = (NUM_CELLS / 2) + 7;
        final int key1 = N1 - 1, key2 = N2 - 2;

        ShipTypeAndSizeStatisticData statistics = (ShipTypeAndSizeStatisticData) statisticsRepository1.getStatisticData(TEST_STATISTIC_NAME, testCellId);
        assertEquals((Integer)(   1 %100), statistics.getValue(1, 1, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertEquals((Integer)((7*4)%100), statistics.getValue(7, 4, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertEquals((Integer)((6*3)%100), statistics.getValue(6, 3, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
        assertEquals((Integer)((key1 * key2) % 100), statistics.getValue(key1, key2, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT));
    }

    @Test
    public void testPrepareBackupDBFile() throws IOException {
        // Prepare test data
        String tmpFilePath = getTempFilePath();
        String databaseName = UUID.randomUUID().toString();

        String dbFileName = tmpFilePath + "/" + databaseName + ".statistics";
        String dbBackupFileName = tmpFilePath + "/" + databaseName + ".backup.statistics";
        String dbPrevBackupFileName = tmpFilePath + "/" + databaseName + ".backup.previous.statistics";

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
        dbBackupFile = StatisticDataRepositoryMapDB.prepareBackupDBFileFor(dbFile);
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
        dbBackupFile = StatisticDataRepositoryMapDB.prepareBackupDBFileFor(dbFile);
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

    private static void writeTestDataToRepository(StatisticDataRepository statisticsRepository) {
        LOG.info("Generating " + N_PROD + " test data... ");
        for (long cellId = 0; cellId < NUM_CELLS; cellId++) {
            ShipTypeAndSizeStatisticData statistics = ShipTypeAndSizeStatisticData.create();

            for (int key1 = 0; key1 <= N1; key1++) {
                for (int key2 = 0; key2 <= N2; key2++) {
                    statistics.setValue(key1, key2, ShipTypeAndSizeStatisticData.STAT_SHIP_COUNT, (key1 * key2) % 100);
                }
            }

            statisticsRepository.putStatisticData(TEST_STATISTIC_NAME, cellId, statistics);

            if (cellId % ONE_PCT_OF_CELLS == 0) {
                LOG.info(100 * cellId / NUM_CELLS + "%");
                //printMemInfo();
            }
        }
        LOG.info("done.");

        LOG.info("storing dataset metadata.");
        DatasetMetaData datasetMetaData = new DatasetMetaData(123.0, 60);
        statisticsRepository.putMetaData(datasetMetaData);
        LOG.info("done.");
    }

}
