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
    final static long numCells = (long) 10e3;

    final static long onePctOfNumCells = numCells / 100;
    final static short n1 = 10;
    final static short n2 = 10;
    final static long nprod = numCells*n1*n2;

    final static long testCellId = (numCells / 2) + 7;

    private static String dbFileName;

    @BeforeClass
    public static void writeSomeTestData() throws Exception {
        String tmpFilePath = getTempFilePath();
        dbFileName = tmpFilePath + "/" + UUID.randomUUID();
        LOG.info("dbFileName: " + dbFileName);

        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName);

        LOG.info("Generating " + nprod + " test data... ");
        for (long cellId = 0; cellId < numCells; cellId++) {
            FeatureData featureData = new FeatureData();

            for (short key1 = 0; key1 < n1; key1++) {
                for (short key2 = 0; key2 < n2; key2++) {
                    featureData.setStatistic(key1, key2, "t", (key1*key2) % 100);
                }
            }

            featureDataRepository.put("FeatureDataRepositoryMapDBTest", cellId, featureData);

            if (cellId % onePctOfNumCells == 0) {
                LOG.info(100 * cellId / numCells + "%");
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
        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName);

        FeatureData featureData = featureDataRepository.get("FeatureDataRepositoryMapDBTest", 1);
        assertEquals(n1, (int) featureData.getNumberOfLevel1Entries());
        assertEquals(1, featureData.getStatistic((short) 1, (short) 1, "t"));
        assertEquals(49, featureData.getStatistic((short) 7, (short) 7, "t"));
        assertEquals(((n1 - 1) * (n2 - 2) % 100), featureData.getStatistic((short) (n1 - 1), (short) (n2 - 2), "t"));
        assertNull(featureData.getStatistic((short) (n1 + 1), (short) 4, "t"));
        assertNull(featureData.getStatistic((short) 1, (short) (n2 + 2), "t"));
        assertNull(featureData.getStatistic((short) 8, (short) 9, "wrongt"));
    }

    @Test
    public void testGetFeatureDataCell2() throws Exception {
        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName);

        FeatureData featureData = featureDataRepository.get("FeatureDataRepositoryMapDBTest", testCellId);
        assertEquals(n1, (int) featureData.getNumberOfLevel1Entries());
        assertEquals(1, featureData.getStatistic((short) 1, (short) 1, "t"));
        assertEquals(49, featureData.getStatistic((short) 7, (short) 7, "t"));
        assertEquals(((n1-1)*(n2-2) % 100), featureData.getStatistic((short) (n1-1), (short) (n2-2), "t"));
        assertNull(featureData.getStatistic((short) (n1+1), (short) 4, "t"));
        assertNull(featureData.getStatistic((short) 1, (short) (n2+2), "t"));
        assertNull(featureData.getStatistic((short) 8, (short) 9, "wrongt"));
    }

    @Test
    public void testUpdateFeatureData() throws Exception {
        FeatureDataRepository featureDataRepository1 = new FeatureDataRepositoryMapDB(dbFileName);
        FeatureData featureData1 = featureDataRepository1.get("FeatureDataRepositoryMapDBTest", testCellId);
        assertEquals(n1, (int) featureData1.getNumberOfLevel1Entries());
        assertEquals(1, featureData1.getStatistic((short) 1, (short) 1, "t"));
        assertEquals(49, featureData1.getStatistic((short) 7, (short) 7, "t"));
        assertEquals(((n1-1)*(n2-2) % 100), featureData1.getStatistic((short) (n1-1), (short) (n2-2), "t"));
        featureDataRepository1.close();
        featureDataRepository1 = null;

        FeatureDataRepository featureDataRepository2 = new FeatureDataRepositoryMapDB(dbFileName);
        FeatureData featureData2 = featureDataRepository2.get("FeatureDataRepositoryMapDBTest", testCellId);
        featureData2.setStatistic((short) 7, (short) 7, "t", 2157);
        assertEquals(2157, featureData2.getStatistic((short) 7, (short) 7, "t"));
        featureDataRepository2.put("FeatureDataRepositoryMapDBTest", testCellId, featureData2);
        featureDataRepository2.close();
        featureDataRepository2 = null;

        FeatureDataRepository featureDataRepository3 = new FeatureDataRepositoryMapDB(dbFileName);
        FeatureData featureData3 = featureDataRepository3.get("FeatureDataRepositoryMapDBTest", testCellId);
        featureData3.setStatistic((short) 7, (short) 7, "t", 2157);
        assertEquals(2157, featureData3.getStatistic((short) 7, (short) 7, "t"));
        featureDataRepository3.put("FeatureDataRepositoryMapDBTest", testCellId, featureData3);
        featureDataRepository3.close();
        featureDataRepository3 = null;
        assertEquals(2157, featureData3.getStatistic((short) 7, (short) 7, "t"));
    }

    @Test
    public void testFeatureNames() throws Exception {
        LOG.info("Opening datastore");
        FeatureDataRepository featureDataRepository = new FeatureDataRepositoryMapDB(dbFileName);
        LOG.info("Done.");

        LOG.info("Gettings feature names");
        Set<String> featureNames = featureDataRepository.getFeatureNames();
        for (String featureName : featureNames) {
            LOG.info("   Feature name: " + featureName);
        }
        LOG.info("Found " + featureNames.size() + " feature names.");

        assertEquals(1, featureNames.size());
        assertEquals("FeatureDataRepositoryMapDBTest", featureNames.toArray(new String[0])[0]);
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