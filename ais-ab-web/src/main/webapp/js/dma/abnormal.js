var dmaAbnormalApp = {

    map: null,

    isGridLayerVisible: false,

    init: function() {
        map = new OpenLayers.Map("map");
        //map.addLayer(new OpenLayers.Layer.OSM("map", "http://homer/osmtiles/${z}/${x}/${y}.png"));
        map.addLayer(new OpenLayers.Layer.OSM());

        var zoom = new OpenLayers.Control.Zoom();
        map.addControl(zoom);
        zoom.activate();

        this.registerZoomEventHandler();
        this.zoomToDenmark();
    },

    zoomToDenmark: function() {
        // http://dev.openlayers.org/apidocs/files/OpenLayers/Projection-js.html
        var fromProjection = new OpenLayers.Projection("EPSG:4326"); // transform from WGS 1984
        var toProjection = new OpenLayers.Projection("EPSG:900913"); // to Spherical Mercator Projection

        // set the map centre on the middle of Denmark
        var lat = 56;
        var lon = 12;
        var zoom = 7;
        map.setCenter(new OpenLayers.LonLat(lon,lat).transform(fromProjection,toProjection), zoom)
    },

    registerZoomEventHandler: function() {
        map.events.register('zoomend', map, function(evt) {
            if (map.zoom >= 15) {
                if (dmaAbnormalApp.isGridLayerVisible == false) {
                    console.log("Turning on GridLayer.")
                    dmaAbnormalApp.showGridLayer();
                    dmaAbnormalApp.isGridLayerVisible = true;
                } else {
                    console.log("GridLayer is already visible.")
                }
            } else {
                if (dmaAbnormalApp.isGridLayerVisible == true) {
                    console.log("Turning off GridLayer.")
                    dmaAbnormalApp.hideGridLayer();
                    dmaAbnormalApp.isGridLayerVisible = false;
                } else {
                    console.log("GridLayer is already hidden.")
                }
            }
        });
    },

    showGridLayer: function() {
        var layer = map.getLayersByName("DMA grid layer")[0];
        if (layer) {
            layer.display(true);
        } else {
            dmaAbnormalApp.constructGridLayer();
        }
    },

    hideGridLayer: function() {
        var layer = map.getLayersByName("DMA grid layer")[0];
        if (layer) {
            layer.display(false);
        }
    },

    constructGridLayer: function() {
        // allow testing of specific renderers via "?renderer=Canvas", etc
        var renderer = OpenLayers.Util.getParameters(window.location.href).renderer;
        renderer = (renderer) ? [renderer] : OpenLayers.Layer.Vector.prototype.renderers;

        // we want opaque external graphics and non-opaque internal graphics
        var layer_style = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default']);
        layer_style.fillOpacity = 0.2;
        layer_style.graphicOpacity = 1;

        var vectorLayer = new OpenLayers.Layer.Vector("DMA grid layer", {
            style: layer_style,
            renderers: renderer
        });

        dmaAbnormalApp.addGridToLayer(vectorLayer);

        map.addLayer(vectorLayer);
    },

    addGridToLayer: function(layer) {
        var point = new OpenLayers.Geometry.Point(1335709.8481391, 7558410.9340409);

        // create a line feature from a list of points
        var pointList = [];                                                              /*
         * Green style
         */
        var style_green = {
            strokeColor: "#00FF00",
            strokeWidth: 3,
            strokeDashstyle: "dashdot",
            pointRadius: 6,
            pointerEvents: "visiblePainted",
            title: "this is a green line"
        };

        var newPoint = point;
        for(var p=0; p<15; ++p) {
            newPoint = new OpenLayers.Geometry.Point(newPoint.x + Math.random(1)*1e5,
                newPoint.y + Math.random(1)*1e5);
            pointList.push(newPoint);
        }
        pointList.push(new OpenLayers.Geometry.Point(1335709.8481391, 7558410.9340409));
        var lineFeature = new OpenLayers.Feature.Vector(
            new OpenLayers.Geometry.LineString(pointList),null,style_green);

        layer.addFeatures([lineFeature]);

        var cell = new OpenLayers.Bounds(1335709.8481391,7558410.9340409,1385709.8481391,7588410.9340409).toGeometry();
        layer.addFeatures([cell]);
        dmaAbnormalApp.addCells(layer);
    },

    addCells: function(layer) {
        console.log("Adding cells");
        for (var lon = 12.0; lon < 12.50; lon += 0.05) {
            for (var lat = 56.0; lat < 56.50; lat += 0.05) {
                var north = lat;
                var east  = lon;
                var south = lat - 0.049;
                var west  = lon - 0.049;
                console.log("added cell(layer," + north + "," + east + "," + south + "," + west);
                dmaAbnormalApp.addCell(layer, north, east, south, west);
            }
        }
    },

    addCell: function(layer, north, east, south, west) {
        // http://localhost:8080/abnormal/featuredata/cell?north=55&east=11&south=54.91&west=10.91
        var cellCoords = new Array();

        point = new OpenLayers.Geometry.Point(west, north);
        point.transform(new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject());
        cellCoords.push(point);

        point = new OpenLayers.Geometry.Point(east, north);
        point.transform(new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject());
        cellCoords.push(point);

        point = new OpenLayers.Geometry.Point(east, south);
        point.transform(new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject());
        cellCoords.push(point);

        point = new OpenLayers.Geometry.Point(west, south);
        point.transform(new OpenLayers.Projection("EPSG:4326"), map.getProjectionObject());
        cellCoords.push(point);

        var cellStyle = {
            strokeColor: "#aaaaaa",
            strokeWidth: 2,
            strokeDashstyle: "solid",
            pointRadius: 6,
            pointerEvents: "visiblePainted", // http://www.w3.org/TR/SVG11/interact.html#PointerEventsProperty
            title: "cell",
            fillOpacity: 0.25

        };

        var cell = new OpenLayers.Geometry.LinearRing(cellCoords);
        cellFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Polygon([cell]), null, cellStyle);
        layer.addFeatures([cellFeature]);
    }

}

