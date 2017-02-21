/* Copyright (c) 2011 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dk.dma.ais.abnormal.analyzer.services;

import com.google.inject.Inject;
import dk.dma.ais.abnormal.analyzer.analysis.ShipTypeAndSizeAnalysis;
import dk.dma.enav.model.geometry.CoordinateSystem;
import dk.dma.enav.model.geometry.Ellipse;
import dk.dma.enav.model.geometry.Position;
import dk.dma.enav.util.CoordinateConverter;
import dk.dma.enav.util.geometry.Point;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_SAFETYZONES_SAFETY_ELLIPSE_BEHIND;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_SAFETYZONES_SAFETY_ELLIPSE_BREADTH;
import static dk.dma.ais.abnormal.analyzer.config.Configuration.CONFKEY_SAFETYZONES_SAFETY_ELLIPSE_LENGTH;
import static dk.dma.enav.util.compass.CompassUtils.compass2cartesian;
import static java.lang.Math.max;

/**
 * This class holds static methods needed to calculate safety zones (ellipses) and elliptically approximated extents
 * for vessels.
 *
 * @author Thomas Borg Salling <tbsalling@tbsalling.dk>
 */
public class SafetyZoneService {

    private static final Logger LOG = LoggerFactory.getLogger(ShipTypeAndSizeAnalysis.class);

    private final double safetyEllipseLength;
    private final double safetyEllipseBreadth;
    private final double safetyEllipseBehind;

    @Inject
    public SafetyZoneService(Configuration configuration) {
        safetyEllipseLength = configuration.getDouble(CONFKEY_SAFETYZONES_SAFETY_ELLIPSE_LENGTH);
        safetyEllipseBreadth = configuration.getDouble(CONFKEY_SAFETYZONES_SAFETY_ELLIPSE_BREADTH);
        safetyEllipseBehind = configuration.getDouble(CONFKEY_SAFETYZONES_SAFETY_ELLIPSE_BEHIND);

        LOG.debug("Using safetyEllipseLength = " + safetyEllipseLength);
        LOG.debug("Using safetyEllipseBreadth = " + safetyEllipseBreadth);
        LOG.debug("Using safetyEllipseBehind = " + safetyEllipseBehind);
    }

    /**
     * Compute the an elliptic zone which roughly corresponds to the vessel's physical extent.
     *
     * @param position The reported position of the vessel.
     * @param hdg heading measured in compass degrees.
     * @param loa The vessel's length-overall (in meters).
     * @param beam The vessel's beam (in meters).
     * @param dimStern Distance from GPS antenna to vessel's stern (in meters).
     * @param dimStarboard Distance from GPS antenne to vessel's starboard beam (in meters).
     * @return an Ellipse approximately covering the vessel's extent.
     */
    public Ellipse vesselExtent(Position position, float hdg, float loa, float beam, float dimStern, float dimStarboard) {
        return vesselExtent(position, position, hdg, loa, beam, dimStern, dimStarboard);
    }

    /**
     * Compute the an elliptic zone which roughly corresponds to the vessel's physical extent.
     *
     * The cartesian center is required to compare for intersection of the return ellipse when other ellipses. If no
     * such comparison is ever made, the alternative method with no geodetic center can be used.
     *
     * @param geodeticReference the geodetic reference needed to compare the returned Ellipse with other Ellipses.
     * @param position The reported position of the vessel.
     * @param hdg heading measured in compass degrees.
     * @param loa The vessel's length-overall (in meters).
     * @param beam The vessel's beam (in meters).
     * @param dimStern Distance from GPS antenna to vessel's stern (in meters).
     * @param dimStarboard Distance from GPS antenne to vessel's starboard beam (in meters).
     * @return an Ellipse approximately covering the vessel's extent.
     */
    public Ellipse vesselExtent(Position geodeticReference, Position position, float hdg, float loa, float beam, float dimStern, float dimStarboard) {
        return createEllipse(geodeticReference, position, hdg, loa, beam, dimStern, dimStarboard, 1.0, 1.0, 0.5);
    }

    /**
     * Compute the safety zone of track. This is roughly equivalent to the elliptic area around the vessel
     * which its navigator would observe for safety reasons to avoid imminent collisions.
     *
     * The cartesian center is required to compare for intersection of the return ellipse when other ellipses. If no
     * such comparison is ever made, the alternative method with no geodetic center can be used.
     *
     * @param geodeticReference the geodetic reference needed to compare the returned Ellipse with other Ellipses.
     * @param position The reported position of the vessel.
     * @param cog Course over ground in compass degrees.
     * @param sog Speed over ground in knots.
     * @param loa The vessel's length-overall (in meters).
     * @param beam The vessel's beam (in meters).
     * @param dimStern Distance from GPS antenna to vessel's stern (in meters).
     * @param dimStarboard Distance from GPS antenne to vessel's starboard beam (in meters).
     * @return an Ellipse approximately covering the vessel's extent.
     */
    public Ellipse safetyZone(Position geodeticReference, Position position, float cog, float sog, float loa, float beam, float dimStern, float dimStarboard) {
        final double v = 1.0;  /* TODO should depend on sog */
        final double l1 = max(safetyEllipseLength*v, 1.0 + safetyEllipseBehind*v*2.0);
        final double b1 = max(safetyEllipseBreadth*v, 1.5);
        final double xc = -safetyEllipseBehind*v + 0.5*l1;
        return createEllipse(geodeticReference, position, cog, loa, beam, dimStern, dimStarboard, l1, b1, xc);
    }


    /**
     * Compute an ellipse surrounding.
     *
     * The position, offset, orientation and scale of the ellipse will follow characteristics of properties of a track.
     *
     * @param geodeticReference geodetic reference point for geographic->cartesian mappings
     * @param position Initial position of ellipse center.
     * @param direction Direction of ellipse's major axis (in compass degrees).
     * @param loa Length of the related vessel's major axis (in meters).
     * @param beam Length of the ellipse's minor axis (in meters)
     * @param dimStern Offset of ellipse's center along major axis (in meters).
     * @param dimStarboard Offset of ellipse's center along minor axis (in meters).
     * @param l1
     * @param b1
     * @param xc
     * @return
     */
    private Ellipse createEllipse(Position geodeticReference, Position position, float direction, float loa, float beam, float dimStern, float dimStarboard, double l1, double b1, double xc) {
        // Compute direction of half axis alpha
        final double thetaDeg = compass2cartesian(direction);

        // Transform latitude/longitude to cartesian coordinates
        final double centerLatitude = geodeticReference.getLatitude();
        final double centerLongitude = geodeticReference.getLongitude();
        final CoordinateConverter CoordinateConverter = new CoordinateConverter(centerLongitude, centerLatitude);

        final double trackLatitude = position.getLatitude();
        final double trackLongitude = position.getLongitude();
        final double x = CoordinateConverter.lon2x(trackLongitude, trackLatitude);
        final double y = CoordinateConverter.lat2y(trackLongitude, trackLatitude);

        // Compute center of ellipse
        final Point pt0 = new Point(x, y);
        Point pt1 = new Point(pt0.getX() - dimStern + loa*xc, pt0.getY() + dimStarboard - beam/2.0);
        pt1 = pt1.rotate(pt0, thetaDeg);

        // Compute length of half axis alpha
        final double alpha = loa*l1/2.0;

        // Compute length of half axis beta
        final double beta = beam*b1/2.0;

        return new Ellipse(geodeticReference, pt1.getX(), pt1.getY(), alpha, beta, thetaDeg, CoordinateSystem.CARTESIAN);
    }

}
