/**
 * This javascript module handles loading, processing, display and user interaction with feature data.
 */

var vesselModule = {

    historyStyle: {
        strokeColor: 'red',
        strokeOpacity: 1,
        strokeWidth: 2
    },

    markerStyle: {
        strokeColor: 'orange'
    },

    init: function () {
    },

    addEvent: function(event) {
        vesselModule.addBehavior(event.behaviour);
    },

    addBehavior: function(behaviour) {
        var trackingPoints = behaviour.trackingPoints;

        // Track history
        var points = new Array();
        $.each(trackingPoints, function (i, trackingPoint) {
            var point = new OpenLayers.Geometry.Point(trackingPoint.longitude, trackingPoint.latitude);
            point.transform(mapModule.projectionWGS84, mapModule.projectionSphericalMercator);
            points.push(point);
        });
        var trackHistory = new OpenLayers.Geometry.LineString(points);

        var trackHistoryFeature = new OpenLayers.Feature.Vector(trackHistory, null, vesselModule.historyStyle);
        mapModule.getVesselLayer().addFeatures([trackHistoryFeature]);

        // Tracking point markers
        $.each(trackingPoints, function (i, trackingPoint) {
            var point = new OpenLayers.Geometry.Point(trackingPoint.longitude, trackingPoint.latitude);
            point.transform(mapModule.projectionWGS84, mapModule.projectionSphericalMercator);
            var markerGeometry = OpenLayers.Geometry.Polygon.createRegularPolygon(point, 30, 20);
            var markerFeature = new OpenLayers.Feature.Vector(markerGeometry, null, vesselModule.markerStyle);
            mapModule.getVesselLayer().addFeatures([markerFeature]);
        });

        // Track symbol
        var trackingPoint = trackingPoints[trackingPoints.length - 1];
        var trackSymbolFeature = new OpenLayers.Feature.Vector(
            new OpenLayers.Geometry.Point(trackingPoint.longitude, trackingPoint.latitude).transform(mapModule.projectionWGS84, mapModule.projectionSphericalMercator),
            {description:'This is the value of<br>the description attribute'} ,
            {externalGraphic: 'img/vessel_red.png', graphicHeight: 10, graphicWidth: 20, graphicXOffset:-5, graphicYOffset:-5, rotation: trackingPoint.courseOverGround - 90}
        );
        mapModule.getVesselLayer().addFeatures([trackSymbolFeature]);
    }

};
