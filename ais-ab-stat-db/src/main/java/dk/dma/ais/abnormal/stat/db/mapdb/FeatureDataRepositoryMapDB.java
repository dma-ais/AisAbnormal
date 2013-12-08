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
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Pump;
import org.mapdb.StoreHeap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class FeatureDataRepositoryMapDB implements FeatureDataRepository {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureDataRepositoryMapDB.class);
    static {
        LOG.debug("FeatureDataRepositoryMapDB loaded.");
    }
    {
        LOG.debug(this.getClass().getSimpleName() + " created (" + this + " ).");
    }

    private static final String FILENAME_SUFFIX = ".featureData";

    private static final String COLLECTION_METADATA = "metadata";
    private static final String KEY_METADATA = "metadata";

    private DB db;

    private File dbFile;
    private boolean readOnly;
    private boolean dumpToDiskOnClose;

    public FeatureDataRepositoryMapDB(String dbFileName) throws Exception {

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

        db = DBMaker
                .newFileDB(dbFile)
                .transactionDisable()
                .fullChunkAllocationEnable()
                .randomAccessFileEnable()
                        //.asyncWriteDisable()
                .cacheDisable()
                .closeOnJvmShutdown()
                .readOnly()
                .make();

        LOG.debug("File successfully opened for read by MapDB.");
    }

    @Override
    public void openForWrite(boolean cacheInMemoryDumpToDiskOnClose) {
        this.readOnly = false;
        this.dumpToDiskOnClose = cacheInMemoryDumpToDiskOnClose;

        if (cacheInMemoryDumpToDiskOnClose) {
            //LOG.error("cacheInMemoryDumpToDiskOnClose not yet supported.");
            //throw new UnsupportedOperationException("cacheInMemoryDumpToDiskOnClose not yet supported.");
            //db = DBMaker.newDirectMemoryDB().make();    // Serialize off-heap
            db = new DB(new StoreHeap());               // Use heap; subject to GC
            LOG.debug("Opened memory-based database.");
        } else {
            db = DBMaker
                    .newFileDB(dbFile)
                    .transactionDisable()
                    .fullChunkAllocationEnable()
                    .randomAccessFileEnable()
                    //.asyncWriteDisable()
                    .cacheDisable()
                    .closeOnJvmShutdown()
                    .make();
            LOG.debug("Opened disk-based database.");
        }

        LOG.debug("Database successfully opened for write by MapDB.");
    }

    @Override
    public void close() {
        LOG.info("Attempting to commit feature data repository.");
        if (!readOnly) {
            db.commit();
        }
        LOG.info("Feature data repository committed.");

        if (this.dumpToDiskOnClose) {
            LOG.info("Dump in-memory data to disk.");
            DB onDisk = DBMaker
                    .newFileDB(dbFile)
                    .transactionDisable()
                    .fullChunkAllocationEnable()
                    .randomAccessFileEnable()
                            //.asyncWriteDisable()
                    .cacheDisable()
                    .closeOnJvmShutdown()
                    .make();

            Pump.copy(db, onDisk);
            onDisk.close();
            LOG.info("Dump in-memory data to disk: Done.");
        }

        // LOG.info("Attempting to compact feature data repository.");
        // db.compact();
        // LOG.info("Feature data repository compacted.");

        LOG.info("Attempting to close feature data repository.");
        db.close();
        LOG.info("Feature data repository closed.");
    }

    @Override
    public Set<String> getFeatureNames() {
        Map<String, Object> features = db.getAll();
        Set<String> allKeys = features.keySet();

        // Filter so only features are returned (not metadata etc.)
        Set<String> featureNames = new LinkedHashSet<>();
        for (String key : allKeys) {
            if (! key.equals(COLLECTION_METADATA)) {
                featureNames.add(key);
            }
        }

        return featureNames;
    }

    @Override
    public long getNumberOfCells(String featureName) {
        BTreeMap<Object, Object> allCellDataForFeature = db.createTreeMap(featureName).makeOrGet();
        long numberOfCells = allCellDataForFeature.sizeLong();
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
        BTreeMap<String, DatasetMetaData> allMetadata = db.createTreeMap(COLLECTION_METADATA).makeOrGet();
        allMetadata.put(KEY_METADATA, datasetMetadata);
        db.commit();
    }

    @Override
    public FeatureData getFeatureData(String featureName, long cellId) {
        BTreeMap<Object, Object> allCellDataForFeature;

        if (readOnly) {
            allCellDataForFeature = (BTreeMap<Object, Object>) db.getAll().get(featureName);
        } else {
            allCellDataForFeature = db.createTreeMap(featureName).makeOrGet();
        }

        FeatureData featureData = null;
        if (allCellDataForFeature == null) {
            LOG.error("No data exists for feature " + featureName);
        } else {
            featureData = (FeatureData) allCellDataForFeature.get(cellId);
        }

        return featureData;
    }

    @Override
    public void putFeatureData(String featureName, long cellId, FeatureData featureData) {
        BTreeMap<Object, Object> allCellDataForFeature = db.createTreeMap(featureName).makeOrGet();
        allCellDataForFeature.put(cellId, featureData);
    }

    @Override
    public Set<Long> getCellsWithData(String featureName) {
        BTreeMap<Long, FeatureData> allCellDataForFeature;

        if (readOnly) {
            allCellDataForFeature = (BTreeMap<Long, FeatureData>) db.getAll().get(featureName);
        } else {
            allCellDataForFeature = db.createTreeMap(featureName).makeOrGet();
        }

        return allCellDataForFeature.keySet();
    }

    @Override
    public Set<Long> getAllCellsWithData() {
        Set<Long> cellsWithData = new HashSet<>();

        Set<String> featureNames = getFeatureNames();
        for (String featureName : featureNames) {
            Set<Long> cellsWithForFeature = getCellsWithData(featureName);
            cellsWithData.addAll(cellsWithForFeature);
        }

        return cellsWithData;
    }

}
