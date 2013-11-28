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

        /*
        // create a polygon feature from a linear ring of points
        var pointList = [];
        for(var p=0; p<6; ++p) {
            var a = p * (2 * Math.PI) / 7;
            var r = Math.random(1) + 1;
            var newPoint = new OpenLayers.Geometry.Point(point.x + (r * Math.cos(a)),
                point.y + (r * Math.sin(a)));
            pointList.push(newPoint);
        }
        pointList.push(pointList[0]);

        var linearRing = new OpenLayers.Geometry.LinearRing(pointList);
        var polygonFeature = new OpenLayers.Feature.Vector(
            new OpenLayers.Geometry.Polygon([linearRing]));
        */

        map.addLayer(vectorLayer);
        vectorLayer.addFeatures([lineFeature]);
    }
}

