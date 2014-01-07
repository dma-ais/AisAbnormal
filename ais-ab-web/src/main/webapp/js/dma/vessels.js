/**
 * This javascript module handles loading, processing, display and user interaction with feature data.
 */

var vesselModule = {

    init: function () {
    },

    addBehavior: function(behaviour) {
        var positions = behaviour.positions;
        var points = new Array();
        $.each(positions, function (i, position) {
            var point = new OpenLayers.Geometry.Point(position.longitude, position.latitude);
            point.transform(mapModule.projectionWGS84, mapModule.projectionSphericalMercator);
            points.push(point);
        });

        var line = new OpenLayers.Geometry.LineString(points);

        var style = {
            strokeColor: 'red',
            strokeOpacity: 1,
            strokeWidth: 2

        };

        var lineFeature = new OpenLayers.Feature.Vector(line, null, style);
        mapModule.getVesselLayer().addFeatures([lineFeature]);
    },

    addVessel: function (vessel) {
        var cellCoords = new Array();

        point = new OpenLayers.Geometry.Point(cell.west, cell.north);
        point.transform(mapModule.projectionWGS84, mapModule.map.getProjectionObject());
        cellCoords.push(point);

        point = new OpenLayers.Geometry.Point(cell.east, cell.north);
        point.transform(mapModule.projectionWGS84, mapModule.map.getProjectionObject());
        cellCoords.push(point);

        point = new OpenLayers.Geometry.Point(cell.east, cell.south);
        point.transform(mapModule.projectionWGS84, mapModule.map.getProjectionObject());
        cellCoords.push(point);

        point = new OpenLayers.Geometry.Point(cell.west, cell.south);
        point.transform(mapModule.projectionWGS84, mapModule.map.getProjectionObject());
        cellCoords.push(point);

        var fillOpacity = 0.05 + (Math.min(cell.totalShipCount["ShipTypeAndSizeData"]/100, 1))*0.9;
        var strokeOpacity = fillOpacity;

        var cellStyle = {
            strokeColor: "#aaaaaa",
            strokeWidth: 2,
            strokeDashstyle: "solid",
            strokeOpacity: strokeOpacity,
            pointRadius: 6,
            pointerEvents: "visiblePainted", // http://www.w3.org/TR/SVG11/interact.html#PointerEventsProperty
            title: "Cell " + cell.cellId,
            fillOpacity: fillOpacity
        };

        var cellGeometry = new OpenLayers.Geometry.LinearRing(cellCoords);
        cellFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Polygon([cellGeometry]), cell, cellStyle);
        cellFeature.fid = cell.cellId;

        var gridLayer = mapModule.getGridLayer();
        gridLayer.addFeatures([cellFeature]);
    }


};
