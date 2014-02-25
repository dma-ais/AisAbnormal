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

import static org.junit.Assert.assertEquals;

/**
 * Created by tbsalling on 25/02/14.
 */
public class PointTest {
    @Test
    public void testRotate() {
        Point p0 = new Point(1.0, 3.0);
        Point p = new Point(1.0, 4.0);

        p = p.rotate(p0, 90.0);
        assertEquals(0.0, p.getX(), 1e-6);
        assertEquals(3.0, p.getY(), 1e-6);

        p = p.rotate(p0, 90.0);
        assertEquals(1.0, p.getX(), 1e-6);
        assertEquals(2.0, p.getY(), 1e-6);

        p = p.rotate(p0, 90.0);
        assertEquals(2.0, p.getX(), 1e-6);
        assertEquals(3.0, p.getY(), 1e-6);

        p = p.rotate(p0, 90.0);
        assertEquals(1.0, p.getX(), 1e-6);
        assertEquals(4.0, p.getY(), 1e-6);

        p = p.rotate(p0, 45.0);
        assertEquals(0.292893, p.getX(), 1e-6);
        assertEquals(3.707106, p.getY(), 1e-6);
    }

}
