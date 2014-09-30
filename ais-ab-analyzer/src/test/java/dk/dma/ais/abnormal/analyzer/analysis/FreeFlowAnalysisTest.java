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
package dk.dma.ais.abnormal.analyzer.analysis;

import dk.dma.enav.model.geometry.Position;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FreeFlowAnalysisTest {

    @Test
    public void testCenterOfVessel() {
        Position p;

        p = FreeFlowAnalysis.centerOfVessel(Position.create(56, 12), 0f, 0, 0, 0, 0);
        assertEquals(56f, p.getLatitude(), 1e-16);
        assertEquals(12f, p.getLongitude(), 1e-16);

        p = FreeFlowAnalysis.centerOfVessel(Position.create(56, 12), 0f, 50, 50, 10, 10);
        assertEquals(56.000000f, p.getLatitude(), 1e-6);
        assertEquals(12.000000f, p.getLongitude(), 1e-6);

        p = FreeFlowAnalysis.centerOfVessel(Position.create(56, 12), 90f, 50, 50, 10, 10);
        assertEquals(56.000000f, p.getLatitude(), 1e-6);
        assertEquals(12.000000f, p.getLongitude(), 1e-6);

        p = FreeFlowAnalysis.centerOfVessel(Position.create(56, 12), 180f, 50, 50, 10, 10);
        assertEquals(56.000000f, p.getLatitude(), 1e-6);
        assertEquals(12.000000f, p.getLongitude(), 1e-6);

        p = FreeFlowAnalysis.centerOfVessel(Position.create(56, 12), 0f, 25, 75, 10, 10);
        assertEquals(56.000225f, p.getLatitude(), 1e-6);
        assertEquals(12.000000f, p.getLongitude(), 1e-6);

        p = FreeFlowAnalysis.centerOfVessel(Position.create(56, 12), 0f, 75, 25, 10, 10);
        assertEquals(55.999774f, p.getLatitude(), 1e-6);
        assertEquals(12.000000f, p.getLongitude(), 1e-6);

        p = FreeFlowAnalysis.centerOfVessel(Position.create(56, 12), 0f, 50, 50, 5, 15);
        assertEquals(56.000000f, p.getLatitude(), 1e-6);
        assertEquals(12.000081f, p.getLongitude(), 1e-6);

        p = FreeFlowAnalysis.centerOfVessel(Position.create(56, 12), 0f, 50, 50, 15, 5);
        assertEquals(56.000000f, p.getLatitude(), 1e-6);
        assertEquals(11.999919f, p.getLongitude(), 1e-6);
    }
}
