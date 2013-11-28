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

package dk.dma.ais.abnormal.stat.db.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FeatureData2KeyTest {

    @Test
    public void canStoreAndRetrieveSingleStatistic() {
        FeatureData2Key featureData = new FeatureData2Key(this.getClass(), "key1", "key2");

        featureData.setStatistic((short) 3, (short) 1, "testStat", 42);

        assertEquals((Integer) 1, featureData.numberOfLevel1Entries());
        assertEquals(42, featureData.getStatistic((short) 3, (short) 1, "testStat"));
        assertNull(featureData.getStatistic((short) 2, (short) 1, "testStat"));
        assertNull(featureData.getStatistic((short) 3, (short) 2, "testStat"));
        assertNull(featureData.getStatistic((short) 3, (short) 1, "wrongTestStat"));
    }

    @Test
    public void canIncrementInitializedStatistic() {
        FeatureData2Key featureData = new FeatureData2Key(this.getClass(), "key1", "key2");

        featureData.setStatistic((short) 3, (short) 1, "testStat", 42);
        featureData.incrementStatistic((short) 3, (short) 1,"testStat");

        assertEquals(43, featureData.getStatistic((short) 3, (short) 1, "testStat"));
    }

    @Test
    public void canIncrementUninitializedStatistic() {
        FeatureData2Key featureData = new FeatureData2Key(this.getClass(), "key1", "key2");
        featureData.incrementStatistic((short) 3, (short) 1,"testStat");
        assertEquals(1, featureData.getStatistic((short) 3, (short) 1, "testStat"));

        featureData = new FeatureData2Key(this.getClass(), "key1", "key2");
        featureData.incrementStatistic((short) 3, (short) 1,"testStat");
        featureData.incrementStatistic((short) 3, (short) 1,"testStat");
        assertEquals(2, featureData.getStatistic((short) 3, (short) 1, "testStat"));
    }
}
