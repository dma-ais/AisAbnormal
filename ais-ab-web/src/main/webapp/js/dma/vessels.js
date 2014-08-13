/**
 * This javascript module handles loading, processing, display and user interaction with statistic data.
 */

var vesselModule = {

    init: function () {
    },

    getAllEvents: function() {
        var events = [];
        var statistics = mapModule.getVesselLayer().features;
        $.each(statistics, function (i, statistic) {
            if (statistic && statistic.fid && statistic.fid.match("^event-")) {
                events.push(statistic);
            }
        });
        return events;
    },

    addEvent: function(event) {
        var extent = eventModule.computeEventExtent(event);

        if (mapModule.getVesselLayer().getFeatureByFid("event-" + event.id) == null)  {
            vesselModule.addEventBox(event, extent);
            vesselModule.addBehaviors(event);
        } else {
            console.log("Event id " + event.id + " already added to map.");
        }

        var zoomExtent = eventModule.expandBounds(extent, 20000);

        mapModule.zoomTo(zoomExtent);
    },

    addEventBox: function(event, extent) {
        // Box
        var corners = new Array();
        var nw = new OpenLayers.LonLat(extent.left, extent.top);
        corners.push(new OpenLayers.Geometry.Point(nw.lon, nw.lat).transform(mapModule.projectionWGS84, mapModule.map.getProjectionObject()));
        var ne = new OpenLayers.LonLat(extent.right, extent.top);
        corners.push(new OpenLayers.Geometry.Point(ne.lon, ne.lat).transform(mapModule.projectionWGS84, mapModule.map.getProjectionObject()));
        var se = new OpenLayers.LonLat(extent.right, extent.bottom);
        corners.push(new OpenLayers.Geometry.Point(se.lon, se.lat).transform(mapModule.projectionWGS84, mapModule.map.getProjectionObject()));
        var sw = new OpenLayers.LonLat(extent.left, extent.bottom);
        corners.push(new OpenLayers.Geometry.Point(sw.lon, sw.lat).transform(mapModule.projectionWGS84, mapModule.map.getProjectionObject()));
        var eventBoxStyle = {
            strokeColor: 'white',
            strokeWidth: 3,
            strokeDashstyle: "solid",
            strokeOpacity: 1.0,
            graphicZIndex: 0,
            pointRadius: 6,
            pointerEvents: "visiblePainted",
            title: "Event",
            fillOpacity: 0.5
        };
        var eventBoxGeometry = new OpenLayers.Geometry.LinearRing(corners);
        var eventBoxFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Polygon([eventBoxGeometry]),
            {
                jsonEvent: event
            }, eventBoxStyle);
        eventBoxFeature.fid = "event-" + event.id;

        // Label
        var labelPointGeometry = new OpenLayers.Geometry.Point(extent.left, extent.top).transform(mapModule.projectionWGS84, mapModule.map.getProjectionObject());
        var labelStyle = {
        };
        var labelFeature = new OpenLayers.Feature.Vector(labelPointGeometry, null, labelStyle);
        labelFeature.fid='eventLabel-'+event.id;

        // Add to layer
        mapModule.getVesselLayer().addFeatures([eventBoxFeature, labelFeature]);

        // Add user output (labels, tooltips, etc.)
        vesselModule.addEventBoxLabel(event);
    },

    addBehaviors: function(event) {
        var behaviours = event.behaviours;
        $.each(behaviours, function (b, behaviour) {
            var trackingPoints = behaviour.trackingPoints;
            var vessel = behaviour.vessel;

            // Ellipses
            if (event.safetyZoneOfPrimaryVessel) {
                var latitude = event.safetyZoneOfPrimaryVessel.centerLatitude;
                var longitude = event.safetyZoneOfPrimaryVessel.centerLongitude;
                var semiMajorMeters = event.safetyZoneOfPrimaryVessel.majorSemiAxisLength;
                var semiMinorMeters = event.safetyZoneOfPrimaryVessel.minorSemiAxisLength;
                var majorAxisHeading = event.safetyZoneOfPrimaryVessel.majorAxisHeading;
                vesselModule.addEllipse(latitude, longitude, semiMajorMeters, semiMinorMeters, majorAxisHeading, 2);
            }
            if (event.extentOfSecondaryVessel) {
                var latitude = event.extentOfSecondaryVessel.centerLatitude;
                var longitude = event.extentOfSecondaryVessel.centerLongitude;
                var semiMajorMeters = event.extentOfSecondaryVessel.majorSemiAxisLength;
                var semiMinorMeters = event.extentOfSecondaryVessel.minorSemiAxisLength;
                var majorAxisHeading = event.extentOfSecondaryVessel.majorAxisHeading;
                vesselModule.addEllipse(latitude, longitude, semiMajorMeters, semiMinorMeters, majorAxisHeading, 1);
            }

            // Track symbol
            var trackingPoint = trackingPoints[trackingPoints.length - 1];

            console.log("Adding track symbol in " + trackingPoint.latitude + " " + trackingPoint.longitude);
            vesselModule.addTrackSymbol(trackingPoint.latitude, trackingPoint.longitude, trackingPoint.trueHeading, vessel.toBow, vessel.toStern, vessel.toPort, vessel.toStarboard);

            var trackSymbolFeature = new OpenLayers.Feature.Vector(
                new OpenLayers.Geometry.Point(
                    trackingPoint.longitude, trackingPoint.latitude).transform(mapModule.projectionWGS84, mapModule.projectionSphericalMercator),
                {
                    name: vessel.name,
                    callsign: vessel.callsign,
                    imo: vessel.imo,
                    mmsi: vessel.mmsi
                },
                {
                    externalGraphic: 'img/vessel_red.png',
                    graphicHeight: 10,
                    graphicWidth: 20,
                    graphicXOffset:-5,
                    graphicYOffset:-5,
                    graphicZIndex: 2,
                    rotation: trackingPoint.courseOverGround - 90
                }
            );
            trackSymbolFeature.fid = 'trackSymbol-'+event.id+'-'+vessel.mmsi;
            mapModule.getVesselLayer().addFeatures([trackSymbolFeature]);

            // Track history
            var points = new Array();
            $.each(trackingPoints, function (i, trackingPoint) {
                var point = new OpenLayers.Geometry.Point(trackingPoint.longitude, trackingPoint.latitude);
                point.transform(mapModule.projectionWGS84, mapModule.projectionSphericalMercator);
                points.push(point);
            });
            var trackHistory = new OpenLayers.Geometry.LineString(points);

            var historyStyle = {
                strokeColor: 'red',
                strokeOpacity: 1,
                strokeWidth: 2,
                graphicZIndex: 2
            };

            var trackHistoryFeature = new OpenLayers.Feature.Vector(trackHistory, null, historyStyle);
            mapModule.getVesselLayer().addFeatures([trackHistoryFeature]);

            // Tracking point markers
            $.each(trackingPoints, function (i, trackingPoint) {
                var point = new OpenLayers.Geometry.Point(trackingPoint.longitude, trackingPoint.latitude);
                point.transform(mapModule.projectionWGS84, mapModule.projectionSphericalMercator);
                var markerStyle = { strokeColor: 'orange', strokeWidth: 2, fillOpacity: 1.0, pointRadius: 3, graphicZIndex: 3};
                if (trackingPoint.positionInterpolated == true) {
                    markerStyle.strokeColor = 'grey';
                }
                if (trackingPoint.eventCertainty == 'RAISED') {
                    markerStyle.fillColor = 'red';
                } else if (trackingPoint.eventCertainty == 'UNCERTAIN') {
                    markerStyle.fillColor = 'yellow';
                } else if (trackingPoint.eventCertainty == 'LOWERED') {
                    markerStyle.fillColor = 'green';
                } else {
                    markerStyle.fillColor = 'blue';
                }
                var markerFeature = new OpenLayers.Feature.Vector(point, {type: "circle"}, markerStyle);
                markerFeature.fid = 'trackingPoint-'+event.id+'-'+trackingPoint.id;
                mapModule.getVesselLayer().addFeatures([markerFeature]);
            });

            // Add user output (labels, tooltips, etc.)
            vesselModule.addTrackSymbolTooltip(event, behaviour.vessel);
            $.each(trackingPoints, function (i, trackingPoint) {
                vesselModule.addTrackingPointTooltip(event, trackingPoint);
            });
        });
    },

    addEventBoxLabel: function(event) {
        var eventBoxLabelFeature = mapModule.getVesselLayer().getFeatureByFid('eventLabel-'+event.id);
        if (eventBoxLabelFeature != null) {
            var eventType = eventModule.camelCaseToSentenceCase(event.eventType);
            var labelText = "Event " + event.id + ": " + eventType;
            labelText += "\n" + event.description;
            labelText += "\nRaised: " + eventModule.formatTimestamp(event.startTime);
            if (event.endTime) {
                labelText += "\nLowered: " + eventModule.formatTimestamp(event.endTime);
            }
            eventBoxLabelFeature.style = {
                label: labelText,
                labelAlign: "lt",
                fontSize: "8pt",
                fontColor: "lightgrey",
                labelXOffset: 10,
                labelYOffset: -10
            };
        }
    },

    addTrackingPointTooltip: function(event, trackingPoint) {
        var trackingPointFeature = mapModule.getVesselLayer().getFeatureByFid('trackingPoint-'+event.id+'-'+trackingPoint.id);
        if (trackingPointFeature != null) {
            trackingPointFeature.style.title =
                eventModule.formatTimestamp(trackingPoint.timestamp) + "\n"
                + OpenLayers.Util.getFormattedLonLat(trackingPoint.latitude, 'lat', 'dms') + " "
                + OpenLayers.Util.getFormattedLonLat(trackingPoint.longitude, 'lon', 'dms') + "\n"
                + "SOG: " + trackingPoint.speedOverGround + " kts "
                + "COG: " + trackingPoint.courseOverGround + " deg\n"
                + (trackingPoint.positionInterpolated == true ? "Interpolated position":"Reported position");

        }
    },

    addTrackSymbolTooltip: function(event, vessel) {
        var trackSymbolFeature = mapModule.getVesselLayer().getFeatureByFid('trackSymbol-'+event.id+'-'+vessel.mmsi);
        if (trackSymbolFeature != null) {
            var tooltip = vessel.name
                + "\n" + vessel.callsign
                + "\nIMO " + vessel.imo
                + "\nMMSI " + vessel.mmsi;


            tooltip += "\n"

            if (event.shipLength) {
                var shipSizeBucket = parseInt(event.shipLength);
                tooltip += "\nShip size: " + shipSizeBucket + " (" + statisticsModule.categories['size'][shipSizeBucket] + ")"
            }

            if (event.shipType) {
                var shipTypeBucket = parseInt(event.shipType);
                tooltip += "\nShip type: " + shipTypeBucket + " (" + statisticsModule.categories['type'][shipTypeBucket] + ")"
            }

            if (event.speedOverGround) {
                var sogBucket = parseInt(event.speedOverGround);
                tooltip += "\nSpeed: " + sogBucket + " (" + statisticsModule.categories['sog'][sogBucket] + ")"
            }

            if (event.courseOverGround) {
                var cogBucket = parseInt(event.courseOverGround);
                tooltip += "\nCourse: " + cogBucket + " (" + statisticsModule.categories['cog'][cogBucket] + ")"
            }

            trackSymbolFeature.style.title = tooltip;
        }
    },

    /**
     * Create an OpenLayers KML geometry to symbolize a ship at the given position, at the given trueHeading and with the
     * given dimensions.
     *
     * @param placemark Parent node
     * @param lat Ship's positional latitude in degrees.
     * @param lon Ship's positional longitude in degrees.
     * @param heading Ship's trueHeading in degrees; 0 being north, 90 being east.
     * @param toBow Distance in meters from ship's position reference to ship's bow.
     * @param toStern Distance in meters from ship's position reference to ship's stern.
     * @param toPort Distance in meters from ship's position reference to port side at maximum beam.
     * @param toStarbord Distance in meters from ship's position reference to starboard side at maximum beam.
     * @return
     */
    addTrackSymbol: function(lat, lon, heading, toBow /* A */, toStern /* B */, toPort /* C */, toStarbord /* D */) {
        var degreesPerMeterLatitude  = 1 / vesselModule.calcLengthOfDegreeLatitudeInMeters(lat);
        var degreesPerMeterLongitude = 1 / vesselModule.calcLengthOfDegreeLongitudeInMeters(lat);

        // If the ship dimensions are not found then create a small ship
        if (toBow < 0 || toStern < 0 || toBow+toStern <= 0) {
            toBow = 20;
            toStern = 4;
        }
        if (toPort < 0 || toStarbord < 0 || toPort+toStarbord <= 0) {
            toPort = (toBow + toStern) / 6.5;
            toStarbord = toPort;
        }

        var szA = toBow * degreesPerMeterLongitude;
        var szB = toStern * degreesPerMeterLongitude;
        var szC = toPort * degreesPerMeterLatitude;
        var szD = toStarbord * degreesPerMeterLatitude;


        // The ship consists of 5 points which are stored in 'points'.
        // To begin with the points are in meters
        var points = [];
        points.push(new OpenLayers.Geometry.Point(lon - szB, lat + szC).transform(mapModule.projectionWGS84, mapModule.projectionSphericalMercator));                       // stern port
        points.push(new OpenLayers.Geometry.Point(lon - szB + 0.85*(szA + szB), lat + szC).transform(mapModule.projectionWGS84, mapModule.projectionSphericalMercator));
        points.push(new OpenLayers.Geometry.Point(lon + szA, lat + szC - (szC + szD)/2.0).transform(mapModule.projectionWGS84, mapModule.projectionSphericalMercator));      // bow
        points.push(new OpenLayers.Geometry.Point(lon - szB + 0.85*(szA + szB), lat - szD).transform(mapModule.projectionWGS84, mapModule.projectionSphericalMercator));
        points.push(new OpenLayers.Geometry.Point(lon - szB, lat - szD).transform(mapModule.projectionWGS84, mapModule.projectionSphericalMercator));                      // stern starboard

        // Convert ship coordinates into geographic coordinates and an OpenLayers geometry
        var linearRing = new OpenLayers.Geometry.LinearRing(points);
        var lineStyle = { strokeColor: 'white', fillColor: 'white', strokeWidth: 2.0, fillOpacity: 0.5, pointRadius: 1, graphicZIndex: 1};
        var polygonFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Polygon([linearRing]), null, lineStyle);

        var originPoint = new OpenLayers.Geometry.Point(lon, lat).transform(mapModule.projectionWGS84, mapModule.projectionSphericalMercator);
        polygonFeature.geometry.rotate(90 - heading, originPoint);

        mapModule.getVesselLayer().addFeatures([polygonFeature]);
    },

    deg2RadConvFactor: 2.0 * Math.PI/360.0,
    deg2rad: function(deg) {
        return deg * vesselModule.deg2RadConvFactor;
    },

    calcLengthOfDegreeLatitudeInMeters: function(latitudeDegrees) {
        // http://www.csgnetwork.com/degreelenllavcalc.html

        // Convert latitude to radians
        var lat = vesselModule.deg2rad(latitudeDegrees);

        // Set up "Constants"
        var m1 = 111132.92;		// latitude calculation term 1
        var m2 = -559.82;		// latitude calculation term 2
        var m3 = 1.175;			// latitude calculation term 3
        var m4 = -0.0023;		// latitude calculation term 4

        // Calculate the length of a degree of latitude in meters
        var latlen = m1 + (m2 * Math.cos(2 * lat)) + (m3 * Math.cos(4 * lat)) + (m4 * Math.cos(6 * lat));

        return latlen;
    },

    calcLengthOfDegreeLongitudeInMeters: function(latitudeDegrees) {
        // http://www.csgnetwork.com/degreelenllavcalc.html

        // Convert latitude to radians
        var lat = vesselModule.deg2rad(latitudeDegrees);

        // Set up "Constants"
        var p1 = 111412.84;		// longitude calculation term 1
        var p2 = -93.5;			// longitude calculation term 2
        var p3 = 0.118;			// longitude calculation term 3

        // Calculate the length of a degree of longitude in meters
        var longlen = (p1 * Math.cos(lat)) + (p2 * Math.cos(3 * lat)) + (p3 * Math.cos(5 * lat));

        return longlen;
    },

    addEllipse: function(latitude, longitude, semiMajorMeters, semiMinorMeters, rotation, lineThickness) {
        var degreesPerMeterLatitude  = 1 / vesselModule.calcLengthOfDegreeLatitudeInMeters(latitude);
        var degreesPerMeterLongitude = 1 / vesselModule.calcLengthOfDegreeLongitudeInMeters(latitude);

        var semiMajorDegrees = semiMajorMeters * degreesPerMeterLatitude;    // = a
        var semiMinorDegrees = semiMinorMeters * degreesPerMeterLongitude;   // = b

        /*
         *   Snippets for later improvements of drawing the ellipse:
         */
        /*
            var originMeters = new OpenLayers.Geometry.Point(longitude, latitude).transform(mapModule.projectionWGS84, mapModule.projectionSphericalMercator);
            var bboxMeters = new OpenLayers.Geometry.Point(originMeters.x + semiMajorMeters, originMeters.y + semiMinorMeters);
            var bboxDegrees = bboxMeters.transform(mapModule.projectionSphericalMercator, mapModule.projectionWGS84);   // to degrees
            a = bboxDegrees.x - longitude;
            b = bboxDegrees.y - latitude;
        */
        /*
             http://www.movable-type.co.uk/scripts/latlong.html
            Formula:
            φ2 = asin( sin(φ1)*cos(d/R) + cos(φ1)*sin(d/R)*cos(θ) )
            λ2 = λ1 + atan2( sin(θ)*sin(d/R)*cos(φ1), cos(d/R)−sin(φ1)*sin(φ2) )
            where	φ is latitude, λ is longitude, θ is the bearing (in radians, clockwise from north),
                    d is the distance travelled, R is the earth’s radius (d/R is the angular distance, in radians)
        */

        var list = [];
        for (var t = 0 * Math.PI; t < 2 * Math.PI; t += 0.01 ) {
            var r = semiMajorDegrees*semiMinorDegrees / Math.sqrt(Math.pow(semiMinorDegrees*Math.cos(t), 2) + Math.pow(semiMajorDegrees*Math.sin(t), 2));
            var x = latitude + r*Math.cos(t);
            var y = longitude + r*Math.sin(t);
            var p = new OpenLayers.Geometry.Point(y, x).transform(mapModule.projectionWGS84, mapModule.projectionSphericalMercator); // to display projection
            list.push(p);
        };

        var linear_ring = new OpenLayers.Geometry.LinearRing(list);
        var lineStyle = { strokeColor: 'white', strokeWidth: lineThickness, fillOpacity: 0.0, pointRadius: 1, graphicZIndex: 1};
        var polygonFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Polygon([linear_ring]), null, lineStyle);
        var originPoint = new OpenLayers.Geometry.Point(longitude, latitude).transform(mapModule.projectionWGS84, mapModule.projectionSphericalMercator);
        polygonFeature.geometry.rotate(360 - rotation, originPoint);
        mapModule.getVesselLayer().addFeatures([polygonFeature])
    }

};
