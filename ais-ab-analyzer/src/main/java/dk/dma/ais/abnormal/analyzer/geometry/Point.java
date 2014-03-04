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

package dk.dma.ais.abnormal.analyzer.geometry;

import net.jcip.annotations.Immutable;

import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.sin;
import static java.lang.StrictMath.toRadians;

@Immutable
/**
 * This class represent a point expressed in Cartesian coordinates (x,y).
 */
public final class Point {
    private final double x;
    private final double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    /**
     * Rotate this point around point p0 with thetaDeg degrees and return the rotated point.
     *
     * @param p0       the point to rotate around.
     * @param thetaDeg the no. of degrees counter-clockwise to rotate.
     * @return         the rotated point.
     */
    public Point rotate(Point p0, double thetaDeg) {
        final double theta = toRadians(thetaDeg);

        double xr = p0.x + (cos(theta) * (this.x - p0.x) - sin(theta) * (this.y - p0.y));
        double yr = p0.y + (sin(theta) * (this.x - p0.x) + cos(theta) * (this.y - p0.y));

        return new Point(xr, yr);
    }
}
