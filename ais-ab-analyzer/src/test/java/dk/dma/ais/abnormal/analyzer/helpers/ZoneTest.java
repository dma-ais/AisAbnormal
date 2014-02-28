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
package dk.dma.ais.abnormal.analyzer.helpers;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZoneTest {

    @Test
    public void testIntersectsSame() {
        Zone zone1 = new Zone(0.0f, 0.0f, 5.0, 2.5f, 0.0f);
        Zone zone2 = zone1;

        assertTrue(zone1.intersects(zone2));
        assertTrue(zone2.intersects(zone1));
    }

    @Test
    public void testIntersectsNot() {
        Zone zone1 = new Zone( 0.0f, 0.0f, 5.0, 2.5f, 0.0f);
        Zone zone2 = new Zone(10.0f, 0.0f, 4.0, 2.5f, 0.0f);

        assertFalse(zone1.intersects(zone2));
        assertFalse(zone2.intersects(zone1));
    }

    @Test
    public void testIntersects() {
        Zone zone1 = new Zone( 2.0f, 0.0f, 5.0, 2.5f, 0.0f);
        Zone zone2 = new Zone(10.0f, 0.0f, 4.0, 2.5f, 0.0f);

        assertTrue(zone1.intersects(zone2));
        assertTrue(zone2.intersects(zone1));
    }

    @Test
    public void testIntersectsExcel1() {
        Zone zone1 = new Zone( 0.0f,  0.0f, 100.0f,  40.0f,  45.0f);
        Zone zone2 = new Zone(90.0f, 30.0f,  50.0f,  25.0f, 150.0f);

        assertTrue(zone1.intersects(zone2));
        assertTrue(zone2.intersects(zone1));
    }

    @Test
    public void testIntersectsExcel2() {
        Zone zone1 = new Zone( 0.0f,  0.0f, 100.0f,  40.0f,  45.0f);
        Zone zone2 = new Zone(80.0f, 30.0f,  50.0f,  25.0f, 150.0f);

        assertTrue(zone1.intersects(zone2));
        assertTrue(zone2.intersects(zone1));
    }

    @Test
    public void testIntersectsExcel3() {
        Zone zone1 = new Zone( 0.0f,  0.0f, 100.0f,  40.0f,  62.0f);
        Zone zone2 = new Zone(118.0,  0.0f, 100.0f,  50.0f,  30.0f);

        assertTrue(zone1.intersects(zone2));
        assertTrue(zone2.intersects(zone1));
    }

    @Test
    public void testIntersectsExcel1Not() {
        Zone zone1 = new Zone(  0.0f,  0.0f, 100.0f,  40.0f,  45.0f);
        Zone zone2 = new Zone(130.0f, 30.0f,  50.0f,  25.0f, 150.0f);

        assertFalse(zone1.intersects(zone2));
        assertFalse(zone2.intersects(zone1));
    }

}
