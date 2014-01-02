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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by tbsalling on 02/01/14.
 */
public class DatasetMetaDataTest {

    DatasetMetaData metaData;

    @Before
    public void setUp() throws Exception {
        metaData = new DatasetMetaData(0.567842, 5);
    }

    @Test
    public void testGetFormatVersion() throws Exception {
        assertEquals(1, (short) metaData.getFormatVersion());
    }

    @Test
    public void testGetGridResolution() throws Exception {
        assertEquals(0.567842, metaData.getGridResolution(), 1e-10);
    }

    @Test
    public void testGetDownsampling() throws Exception {
        assertEquals((Integer) 5, metaData.getDownsampling());
    }
}
