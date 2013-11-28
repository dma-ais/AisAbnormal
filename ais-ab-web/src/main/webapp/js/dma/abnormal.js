var dmaAbnormalApp = {
    map: null,
    isGridLayerVisible: false,
    projectionWGS84: null,
    projectionSphericalMercator: null,

    init: function() {
        this.projectionWGS84 = new OpenLayers.Projection("EPSG:4326");
        this.projectionSphericalMercator = new OpenLayers.Projection("EPSG:900913");

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
        // set the map centre on the middle of Denmark
        var lat = 56;
        var lon = 12;
        var zoom = 7;
        map.setCenter(new OpenLayers.LonLat(lon,lat).transform(this.projectionWGS84,this.projectionSphericalMercator), zoom)
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
        var cells = dmaAbnormalApp.loadCells(layer);
        dmaAbnormalApp.addCells(layer, cells);
    },

    loadCells: function() {
        // http://localhost:8080/abnormal/featuredata/cell?north=55&east=11&south=54.91&west=10.91
        var cells = new Array();

        for (var lon = 12.0; lon < 12.50; lon += 0.05) {
            for (var lat = 56.0; lat < 56.50; lat += 0.05) {
                var cell = {
                    north: lat,
                    east:  lon,
                    south: lat - 0.049,
                    west:  lon - 0.049
                }
                cells.push(cell);
            }
        }
        return cells;
    },

    addCells: function(layer, cells) {
        console.log("Adding cells");
        for (c in cells) {
            var cell = cells[c];
            dmaAbnormalApp.addCell(layer, cell.north, cell.east, cell.south, cell.west);
        }
    },

    addCell: function(layer, north, east, south, west) {
        var cellCoords = new Array();

        point = new OpenLayers.Geometry.Point(west, north);
        point.transform(dmaAbnormalApp.projectionWGS84, map.getProjectionObject());
        cellCoords.push(point);

        point = new OpenLayers.Geometry.Point(east, north);
        point.transform(dmaAbnormalApp.projectionWGS84, map.getProjectionObject());
        cellCoords.push(point);

        point = new OpenLayers.Geometry.Point(east, south);
        point.transform(dmaAbnormalApp.projectionWGS84, map.getProjectionObject());
        cellCoords.push(point);

        point = new OpenLayers.Geometry.Point(west, south);
        point.transform(dmaAbnormalApp.projectionWGS84, map.getProjectionObject());
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

