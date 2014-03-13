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
import dk.dma.ais.abnormal.stat.db.data.StatisticData;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.StoreHeap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import static java.nio.file.Files.move;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class StatisticDataRepositoryMapDB implements StatisticDataRepository {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticDataRepositoryMapDB.class);
    static {
        LOG.debug("StatisticDataRepositoryMapDB loaded.");
    }
    {
        LOG.debug(this.getClass().getSimpleName() + " created (" + this + ").");
    }

    private static final String FILENAME_SUFFIX = ".statistics";

    private static final String COLLECTION_METADATA = "metadata";
    private static final String KEY_METADATA = "metadata";

    private DB db;

    private File dbFile;
    private boolean readOnly;
    private boolean dumpToDiskOnClose;

    private int lastPercentageWrittenToLog;

    private final ReentrantLock backupToDiskLock = new ReentrantLock();
    private Date nextBackupToDisk;

    public StatisticDataRepositoryMapDB(String dbFileName) throws Exception {

        if (! dbFileName.endsWith(FILENAME_SUFFIX)) {
            dbFileName = dbFileName.concat(FILENAME_SUFFIX);
        }

        // Assert that stated file is usable
        dbFile = new File(dbFileName);
        String canonicalPath = dbFile.getCanonicalPath();// Check that path is valid
        LOG.debug("Using file " + canonicalPath);

        if (!readOnly && dbFile.exists()) {
            LOG.warn("File " + dbFile.getName() + " already exists!");
        }
        if (!readOnly && dbFile.exists() && !dbFile.canWrite()) {
            LOG.warn("Cannot write to file " + dbFile.getName());
        }
        if (readOnly && dbFile.exists() && !dbFile.canRead()) {
            LOG.error("Cannot read from file " + dbFile.getName());
        }
        if (dbFile.exists()) {
            LOG.debug("Will reuse existing file: " + canonicalPath);
        } else {
            LOG.debug("Will create new file: " + canonicalPath);
        }
    }

    @Override
    public void openForRead() {
        this.readOnly = true;
        this.db = openDiskDatabase(dbFile, this.readOnly);
        LOG.debug("File successfully opened for read by MapDB.");
    }

    @Override
    public void openForWrite(boolean cacheInMemoryDumpToDiskOnClose) {
        this.readOnly = false;
        this.dumpToDiskOnClose = cacheInMemoryDumpToDiskOnClose;

        if (cacheInMemoryDumpToDiskOnClose) {
            this.db = openInMemoryOnHeapDatabase();
            scheduleNextBackupDBToDisk();
        } else {
            this.db = openDiskDatabase(dbFile, this.readOnly);
        }

        LOG.debug("Database successfully opened for write by MapDB.");
    }

    @Override
    public void close() {
        LOG.info("Attempting to commit statistic data repository.");
        if (!readOnly) {
            db.commit();
        }
        LOG.info("Statistic data repository committed.");

        if (this.dumpToDiskOnClose) {
            LOG.info("Dump in-memory data to disk.");
            DB onDisk = openDiskDatabase(dbFile, false);
            copyToDatabase(onDisk);
            /*
            LOG.info("Compacting data file.");
            onDisk.compact(); // necessary?
            LOG.info("Completed compacting data file.");
            */
            onDisk.commit();
            onDisk.close();
            LOG.info("Dump in-memory data to disk: Done.");
        }

        LOG.info("Attempting to close statistic data repository.");
        db.close();
        LOG.info("Statistic data repository closed.");
    }

    private void copyToDatabase(DB toDatabase) {
        // Pump.copy(db, onDisk);
        // Causes java.lang.ArrayIndexOutOfBoundsException
        // at java.lang.System.arraycopy(Native Method)
        // at org.mapdb.BTreeKeySerializer.leadingValuePackRead(BTreeKeySerializer.java:189)
        // on read
        // Seems like an unfinished feature?
        // https://github.com/jankotek/MapDB/issues/208

        // Copy metadata to other database
        putMetaData(toDatabase, getMetaData());

        // Copy statistic data to other database
        Set<String> statisticNames = getStatisticNames();

        // Bookkeeping for progress output
        long i = 0;
        long estimatedTotalNumberOfCellsWithData = -1;

        for (String statisticName : statisticNames) {
            Set<Long> cellsWithData = getCellsWithData(statisticName);
            if (estimatedTotalNumberOfCellsWithData < 0) {
                estimatedTotalNumberOfCellsWithData = statisticNames.size() * cellsWithData.size();
            }
            for (Long cellId : cellsWithData) {
                StatisticData statistics = getStatisticData(statisticName, cellId);
                putStatisticData(toDatabase, statisticName, cellId, statistics);
                progressOutput(i++, estimatedTotalNumberOfCellsWithData);
            }
        }
    }

    /**
     * Not reentrant / MT-safe.
     * @param currentIndex
     * @param estimatedTotalNumberOfCellsWithData
     */
    private void progressOutput(long currentIndex, long estimatedTotalNumberOfCellsWithData) {
        int percentageComplete =  (int) (100.0 * ((float) currentIndex) / ((float) estimatedTotalNumberOfCellsWithData));
        if ((percentageComplete % 10 == 0) && percentageComplete != lastPercentageWrittenToLog) {
            lastPercentageWrittenToLog = percentageComplete;
            LOG.info("Dump in-memory data to disk: " + percentageComplete + "% complete.");
        }
    }

    private void backupDBToDisk() {
        LOG.debug("Preparing to backup database to disk.");

        this.backupToDiskLock.lock();
        try {
            File backupDBFile = prepareBackupDBFileFor(dbFile);
            if (backupDBFile == null) {
                LOG.error("Failed to prepare DB backup file. Cannot backup database to disk.");
                return;
            }

            DB backupDB = openDiskDatabase(backupDBFile, false);
            copyToDatabase(backupDB);
            backupDB.commit();
            backupDB.close();

            LOG.info("Database successfully backed up to to disk (\"" + backupDBFile.getName() + "\").");
        } finally {
            this.backupToDiskLock.unlock();
        }
    }

    /**
     * Prepare a new xxx.backup.statistics file that can be used by MapDB to store a copy of the database. If a xxx.backup.statistics
     * already exist it will be renamed to xxx.backup.previous.statistics.
     * @return  The file that MapDB can use to store its data in.
     */
     static File prepareBackupDBFileFor(File dbFile) {
        // Calc name of backup file
        StringBuilder tmp = new StringBuilder(dbFile.getPath());
        int n = tmp.lastIndexOf(FILENAME_SUFFIX);
        tmp.replace(n, n + FILENAME_SUFFIX.length(), ".backup" + FILENAME_SUFFIX);
        String backupFileName = tmp.toString();
        LOG.debug("backupFileName: " + backupFileName);

        File backupFile = new File(backupFileName);
        File backupFileP = new File(backupFileName+".p");
        if (backupFile.exists()) {
            LOG.debug("Previous backup database exists.");
            String previousBackupFileName = backupFileName.replaceFirst(".backup.", ".backup.previous.");
            File previousBackupFile = new File(previousBackupFileName);
            File previousBackupFileP = new File(previousBackupFileName+".p");
            if (previousBackupFile.exists()) {
                LOG.debug("Will delete previous backup file.");
                boolean prevBackupFileDeleted = previousBackupFile.delete();
                if (!prevBackupFileDeleted) {
                    LOG.error("Could not delete previous backup file: " + previousBackupFile.getAbsolutePath());
                    return null;
                }
                boolean prevBackupFilePDeleted = previousBackupFileP.delete();
                if (!prevBackupFilePDeleted) {
                    LOG.error("Could not delete previous backup .p file: " + previousBackupFileP.getAbsolutePath());
                    return null;
                }
            }
            LOG.debug("Will rename previous backup file from " + backupFile.getName() + " to " + previousBackupFile.getName());
            boolean backupFileRenamed = backupFile.renameTo(previousBackupFile);
            if (!backupFileRenamed) {
                LOG.error("Failed to rename backup file from " + backupFile.getName() + " to " + previousBackupFile.getName());
                return null;
            }
            boolean backupFilePRenamed = backupFileP.renameTo(previousBackupFileP);
            if (!backupFilePRenamed) {
                LOG.error("Failed to rename backup .p file from " + backupFile.getName() + " to " + previousBackupFileP.getName());
                return null;
            }
            backupFile = new File(backupFileName);
        }

        return backupFile;
    }

    @Override
    public Set<String> getStatisticNames() {
        Map<String, Object> statistics = db.getAll();
        Set<String> allKeys = statistics.keySet();

        // Filter so only statistics are returned (not metadata etc.)
        Set<String> statisticNames = new LinkedHashSet<>();
        for (String key : allKeys) {
            if (! key.equals(COLLECTION_METADATA)) {
                statisticNames.add(key);
            }
        }

        return statisticNames;
    }

    @Override
    public long getNumberOfCells(String statisticName) {
        BTreeMap<Object, Object> allCellDataForStatistic = db.createTreeMap(statisticName).makeOrGet();
        long numberOfCells = allCellDataForStatistic.sizeLong();
        return numberOfCells;
    }

    @Override
    public DatasetMetaData getMetaData() {
        BTreeMap<String, DatasetMetaData> allMetadata;
        if (readOnly) {
            allMetadata = (BTreeMap<String, DatasetMetaData>) db.getAll().get(COLLECTION_METADATA);
        } else {
            allMetadata = db.createTreeMap(COLLECTION_METADATA).makeOrGet();
        }
        return allMetadata.get(KEY_METADATA);
    }

    @Override
    public void putMetaData(DatasetMetaData datasetMetadata) {
        putMetaData(db, datasetMetadata);
    }

    private void putMetaData(DB db, DatasetMetaData datasetMetadata) {
        BTreeMap<String, DatasetMetaData> allMetadata = db.createTreeMap(COLLECTION_METADATA).makeOrGet();
        allMetadata.put(KEY_METADATA, datasetMetadata);
        db.commit();
    }

    @Override
    public StatisticData getStatisticData(String statisticName, long cellId) {
        BTreeMap<Object, Object> allCellDataForStatistic;

        if (readOnly) {
            allCellDataForStatistic = (BTreeMap<Object, Object>) db.getAll().get(statisticName);
        } else {
            allCellDataForStatistic = db.createTreeMap(statisticName).makeOrGet();
        }

        StatisticData statistics = null;
        if (allCellDataForStatistic == null) {
            LOG.error("No data exists for statistic " + statisticName);
        } else {
            statistics = (StatisticData) allCellDataForStatistic.get(cellId);
        }

        return statistics;
    }

    @Override
    public void putStatisticData(String statisticName, long cellId, StatisticData statistics) {
        putStatisticData(db, statisticName, cellId, statistics);

        // Check if it is time for a memory backup to disk
        if (this.dumpToDiskOnClose && isBackupToDiskScheduled()) {
           try {
               backupToDiskLock.lock();
               if (isBackupToDiskScheduled()) {
                   backupDBToDisk();
                   scheduleNextBackupDBToDisk();
               }
            } finally {
                backupToDiskLock.unlock();
            }
        }
    }

    private void putStatisticData(DB db, String statisticName, long cellId, StatisticData statistics) {
        BTreeMap<Object, Object> allCellDataForStatistic = db.createTreeMap(statisticName).makeOrGet();
        allCellDataForStatistic.put(cellId, statistics);
    }

    @Override
    public Set<Long> getCellsWithData(String statisticName) {
        BTreeMap<Long, StatisticData> allCellDataForStatistic;

        if (readOnly) {
            allCellDataForStatistic = (BTreeMap<Long, StatisticData>) db.getAll().get(statisticName);
        } else {
            allCellDataForStatistic = db.createTreeMap(statisticName).makeOrGet();
        }

        return allCellDataForStatistic.keySet();
    }

    @Override
    public StatisticData getStatisticDataForRandomCell(String statisticName) {
        BTreeMap<Long, StatisticData> allCellDataForStatistic;

        if (readOnly) {
            allCellDataForStatistic = (BTreeMap<Long, StatisticData>) db.getAll().get(statisticName);
        } else {
            allCellDataForStatistic = db.createTreeMap(statisticName).makeOrGet();
        }

        return allCellDataForStatistic.get(allCellDataForStatistic.firstKey());
    }

    private boolean isBackupToDiskScheduled() {
        return nextBackupToDisk.getTime() < System.currentTimeMillis();
    }

    private void scheduleNextBackupDBToDisk() {
        backupToDiskLock.lock();
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, 24);
            this.nextBackupToDisk =  calendar.getTime();
            LOG.info("Next backup from memory to disk scheduled for " + this.nextBackupToDisk);
        } finally {
            this.backupToDiskLock.unlock();
        }
    }

    @SuppressWarnings("unused")
    private static DB openInMemoryOffHeapDatabase() {
        DB db = DBMaker.newDirectMemoryDB().make();   // Serialize to off-heap
        LOG.debug("Opened memory-based off-heap database.");
        return db;
    }

    private static DB openInMemoryOnHeapDatabase() {
        DB db = new DB(new StoreHeap());            // On-heap; subject to garbage collection
        LOG.debug("Opened memory-based on-heap database.");
        return db;
    }

    private static DB openDiskDatabase(File dbFile, boolean readOnly) {
        DB db = null;

        if (readOnly) {
            db = openDiskDatabaseForRead(dbFile);
        } else {
            try {
                db = openDiskDatabaseForWrite(dbFile);
            } catch (IOError e) {
                LOG.error(e.getMessage(), e);
                LOG.info("Trying to move away potentially corrupted file; then retrying to open database file for read/write.");
                try {
                    File dbFileP = new File(dbFile.getParent() + File.separator + dbFile.getName() + ".p");

                    File dbFileCorrupt = new File(dbFile.getParent() + File.separator + dbFile.getName() + ".corrupt");
                    File dbFilePCorrupt = new File(dbFileP.getParent() + File.separator + dbFileP.getName() + ".corrupt");

                    LOG.info("Renaming file " + dbFile.getName() + " to " + dbFileCorrupt.getName());

                    move(dbFile.toPath(), dbFileCorrupt.toPath(), REPLACE_EXISTING);
                    move(dbFileP.toPath(), dbFilePCorrupt.toPath(), REPLACE_EXISTING);

                    db = openDiskDatabaseForWrite(dbFile);
                } catch (IOException e1) {
                    LOG.error(e1.getMessage(), e1);
                    LOG.info("Failed 2nd attempt to open " + dbFile.getName() + " for read/write.; proceeding ahead for best effort.");
                }
            }
        }

        return db;
    }

    private static DB openDiskDatabaseForRead(File dbFile) {
        LOG.debug("Trying to open " + dbFile.getAbsolutePath() + " for read only.");

        DB db;
        db = DBMaker
                .newFileDB(dbFile)
                .transactionDisable()
                .closeOnJvmShutdown()
                .readOnly()
                .make();

        LOG.info("Opened disk-based database (\"" + dbFile.getName() + "\") for read only.");

        return db;
    }

    private static DB openDiskDatabaseForWrite(File dbFile) {
        LOG.debug("Trying to open " + dbFile.getAbsolutePath() + " for read/write.");

        DB db;
        db = DBMaker
                .newFileDB(dbFile)
                .transactionDisable()
                .cacheDisable()
                .closeOnJvmShutdown()
                .make();

        LOG.info("Opened disk-based database (\"" + dbFile.getName() + "\") for read/write.");

        return db;
    }

}
