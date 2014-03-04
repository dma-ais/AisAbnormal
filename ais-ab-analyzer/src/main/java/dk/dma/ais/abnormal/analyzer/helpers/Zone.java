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

import dk.dma.enav.model.geometry.Position;

import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.sin;
import static java.lang.StrictMath.sqrt;
import static java.lang.StrictMath.toRadians;

/**
 * This class holds the defining parameters for the safety zone surrounding a vessel.
 * All values are Cartesian (x,y).
 */
public final class Zone {
    /**
     * The geodetic point on Earth corresponding to (x,y) = (0,0)
     */
    private final Position geodeticReference;
    /**
     * X coordinate of center.
     */
    private final double x;
    /**
     * Y coordinate of center.
     */
    private final double y;
    /**
     * Length of half axis in direction theta
     */
    private final double alpha;
    /**
     * Length of half axis in direction orthogonal to theta
     */
    private final double beta;
    /**
     * Direction of half axis alpha measured in degrees; 0 degrees is parallel with the increasing direction of the X axis.
     */
    private final double thetaDeg;

    public Zone(Position geodeticReference, double x, double y, double alpha, double beta, double thetaDeg) {
        this.geodeticReference = geodeticReference;
        this.x = x;
        this.y = y;
        this.alpha = alpha;
        this.beta = beta;
        this.thetaDeg = thetaDeg;
    }

    public Position getGeodeticReference() {
        return geodeticReference;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAlpha() {
        return alpha;
    }

    public double getBeta() {
        return beta;
    }

    public double getThetaDeg() {
        return thetaDeg;
    }

    /**
     * Returns true if two safety zones intersect.
     *
     * @param otherZone the other safety zone.
     * @return
     */
    public boolean intersects(Zone otherZone) {
        final double h1x = cos(toRadians(thetaDeg));
        final double h1y = sin(toRadians(thetaDeg));

        final double h2x = cos(toRadians(otherZone.thetaDeg));
        final double h2y = sin(toRadians(otherZone.thetaDeg));

        final double vx = otherZone.x - x;
        final double vy = otherZone.y - y;

        final double d = sqrt(vx * vx + vy * vy);

        boolean intersects = true;

        final double SMALL_NUM = 0.1;

        if (d > SMALL_NUM) {
            final double cosb1 = (h1x * vx + h1y * vy) / (sqrt(h1x * h1x + h1y * h1y) * d);
            final double sinb1 = (h1x * vy - h1y * vx) / (sqrt(h1x * h1x + h1y * h1y) * d);
            final double d1 = sqrt((alpha * alpha * beta * beta) / (alpha * alpha * sinb1 * sinb1 + beta * beta * cosb1 * cosb1));
            final double cosb2 = (h2x * vx + h2y * vy) / (sqrt(h2x * h2x + h2y * h2y) * d);
            final double sinb2 = (h2x * vy - h2y * vx) / (sqrt(h2x * h2x + h2y * h2y) * d);
            final double d2 = sqrt((otherZone.alpha * otherZone.alpha * otherZone.beta * otherZone.beta) / (otherZone.alpha * otherZone.alpha * sinb2 * sinb2 + otherZone.beta * otherZone.beta * cosb2 * cosb2));
            if (d - d1 - d2 < 0.0)
                intersects = true;
            else
                intersects = false;

        }

        return intersects;
    }

    public double getMajorAxisGeodeticHeading() {
        return CoordinateTransformer.cartesian2compass(thetaDeg);
    }
}
