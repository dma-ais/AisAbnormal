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

import static java.lang.StrictMath.asin;
import static java.lang.StrictMath.atan;
import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.sin;
import static java.lang.StrictMath.sqrt;
import static java.lang.StrictMath.toDegrees;
import static java.lang.StrictMath.toRadians;

/**
 * Inserts a plane in a point(lon0,lat0) somewhere on the earth. Earth is here a perfect sphere
 * It is then possible to use 2d geometry within some km of the central point.
 */
public class CoordinateTransformer {
    /**
     * Earth radius in metres
     */
    private final static double RADIUS = 6356752.3;

    /**
     * Longitude of the central point in degrees
     */
    private final double lon0;

    /**
     * Latitude of the central point in degrees
     */
    private final double lat0;

    /**
     * Longitude of the central point in radians
     */
    private final double lon0Rad;

    /**
     * Latitude of the central point in radians
     */
    private final double lat0Rad;

    public CoordinateTransformer(double centralLongitude, double centralLatitude) {
        this.lon0 = centralLongitude;
        this.lat0 = centralLatitude;
        this.lon0Rad = toRadians(centralLongitude);
        this.lat0Rad = toRadians(centralLatitude);
    }

    public double lon2x(double lon, double lat)
    {
        double lonRad = toRadians(lon);
        double latRad = toRadians(lat);

        double x=0.0;
        double denom = (1.0 + sin(lat0Rad) * sin(latRad) + cos(lat0Rad) * cos(latRad) * cos(lonRad - lon0Rad));

        if (denom != 0.0) {
            x = ((2.0* RADIUS) / denom) * cos(latRad) * sin(lonRad - lon0Rad);
        }

        return x;
    }

    public double lat2y(double lon, double lat) {
        double lonRad = toRadians(lon);
        double latRad = toRadians(lat);

        double y=0.0;
        double denom = (1.0 + sin(lat0Rad) * sin(latRad) + cos(lat0Rad) * cos(latRad) * cos(lonRad - lon0Rad));

        if (denom != 0.0) {
            y = ((2.0* RADIUS) / denom) * (cos(lat0Rad) * sin(latRad) - sin(lat0Rad) * cos(latRad) * cos(lonRad - lon0Rad));
        }

        return y;
    }

    public double x2Lon(double x, double y) {
        double ro = sqrt(x*x + y*y);
        double c = 2.0 * atan(ro / (2 * RADIUS));
        double denom = (ro * cos(lat0Rad) * cos(c) - y * sin(lat0Rad) * sin(c));

        double lon=lon0;
        if (denom != 0.0) {
            lon = lon0Rad + atan((x * sin(c)) / denom);
            lon = toDegrees(lon);
        }

        return lon;
    }

    public double y2Lat(double x, double y)
    {
        double lat=lat0;
        double ro = sqrt(x*x + y*y);
        if (ro!=0.0)
        {
            double c = 2.0 * atan(ro / (2.0 * RADIUS));
            lat = asin(cos(c) * sin(lat0Rad) + (y * sin(c) * cos(lat0Rad)) / ro);
            lat = toDegrees(lat);
        }
        return lat;
    }
}
