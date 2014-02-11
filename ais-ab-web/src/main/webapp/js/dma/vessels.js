/**
 * This javascript module handles loading, processing, display and user interaction with feature data.
 */

var vesselModule = {

    init: function () {
    },

    addEvent: function(event) {
        var extent = eventModule.computeEventExtent(event);

        if (mapModule.getVesselLayer().getFeatureByFid("event-" + event.id) == null)  {
            vesselModule.addEventBox(event, extent);
            vesselModule.addBehavior(event);
        } else {
            console.log("Event id " + event.id + " already added to map.");
        }

        var zoomExtent = eventModule.expandBounds(extent, 1000);

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
            pointRadius: 6,
            pointerEvents: "visiblePainted",
            title: "Event",
            fillOpacity: 0.5
        };
        var eventBoxGeometry = new OpenLayers.Geometry.LinearRing(corners);
        var eventBoxFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Polygon([eventBoxGeometry]), null, eventBoxStyle);
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

    addBehavior: function(event) {
        var behaviour = event.behaviour;
        var trackingPoints = behaviour.trackingPoints;
        var vessel = behaviour.vessel;

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
            strokeWidth: 2
        };

        var trackHistoryFeature = new OpenLayers.Feature.Vector(trackHistory, null, historyStyle);
        mapModule.getVesselLayer().addFeatures([trackHistoryFeature]);

        // Tracking point markers
        $.each(trackingPoints, function (i, trackingPoint) {
            var point = new OpenLayers.Geometry.Point(trackingPoint.longitude, trackingPoint.latitude);
            point.transform(mapModule.projectionWGS84, mapModule.projectionSphericalMercator);
            var markerStyle = { strokeColor: 'orange', fillOpacity: 0.5, pointRadius: 3 };
            if (trackingPoint.positionInterpolated == true) {
                markerStyle.strokeColor = 'grey';
            }
            var markerFeature = new OpenLayers.Feature.Vector(point, {type: "circle"}, markerStyle);
            markerFeature.fid = 'trackingPoint-'+event.id+'-'+trackingPoint.id;
            mapModule.getVesselLayer().addFeatures([markerFeature]);
        });

        // Track symbol
        var trackingPoint = trackingPoints[trackingPoints.length - 1];
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
                    rotation: trackingPoint.courseOverGround - 90
                }
        );
        trackSymbolFeature.fid = 'trackSymbol-'+event.id+'-'+vessel.mmsi;
        mapModule.getVesselLayer().addFeatures([trackSymbolFeature]);

        // Add user output (labels, tooltips, etc.)
        vesselModule.addTrackSymbolTooltip(event, behaviour.vessel);
        $.each(trackingPoints, function (i, trackingPoint) {
            vesselModule.addTrackingPointTooltip(event, trackingPoint);
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

            var eventType = event.eventType;
            if (eventType == "ShipSizeOrTypeEvent") {
                var shipSizeBucket = parseInt(event.shipLength);
                var shipTypeBucket = parseInt(event.shipType);
                tooltip += "\n"
                        +  "\nShip type: " + shipTypeBucket + " (" + featureModule.categories['type'][shipTypeBucket] + ")"
                        +  "\nShip size: " + shipSizeBucket + " (" + featureModule.categories['size'][shipSizeBucket] + ")";
            }

            trackSymbolFeature.style.title = tooltip;
        }
    }

};
