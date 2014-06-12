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

package dk.dma.ais.abnormal.analyzer;

import dk.dma.ais.filter.GeoMaskFilter;
import dk.dma.enav.model.geometry.BoundingBox;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AbnormalAnalyzerAppModuleTest {

    @Test
    public void canReadXmlResourceForGeoMaskFilter() throws Exception {
        AbnormalAnalyzerAppModule appModule = new AbnormalAnalyzerAppModule(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        GeoMaskFilter geoMaskFilter = appModule.provideGeoMaskFilter();
        List<BoundingBox> boundingBoxes = geoMaskFilter.getSuppressedBoundingBoxes();

        assertNotNull(boundingBoxes);
        assertEquals(58, boundingBoxes.size());

        assertEquals(56.13767740, boundingBoxes.get(0).getMinLat(), 1e-8);
        assertEquals(56.17285020, boundingBoxes.get(0).getMaxLat(), 1e-8);
        assertEquals(10.20465090, boundingBoxes.get(0).getMinLon(), 1e-8);
        assertEquals(10.26770390, boundingBoxes.get(0).getMaxLon(), 1e-8);

        assertEquals(55.55393080, boundingBoxes.get(1).getMinLat(), 1e-8);
        assertEquals(55.56227290, boundingBoxes.get(1).getMaxLat(), 1e-8);
        assertEquals( 9.72816210, boundingBoxes.get(1).getMinLon(), 1e-8);
        assertEquals( 9.76877229, boundingBoxes.get(1).getMaxLon(), 1e-8);
    }
}