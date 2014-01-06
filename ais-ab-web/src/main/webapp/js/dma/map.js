/**
 * This javascript module handles loading, processing, display and user interaction with maps and layers.
 */

var mapModule = {

    map: null,
    isGridLayerVisible: false,

    projectionWGS84: null,
    projectionSphericalMercator: null,

    init: function() {
        this.projectionWGS84 = new OpenLayers.Projection("EPSG:4326");
        this.projectionSphericalMercator = new OpenLayers.Projection("EPSG:900913");

        this.map = new OpenLayers.Map("map");
        this.map.addLayer(new OpenLayers.Layer.OSM());

        var zoom = new OpenLayers.Control.Zoom();
        this.map.addControl(zoom);
        zoom.activate();

        this.registerEventHandlers();
        this.zoomToDenmark();
    },

    zoomToDenmark: function() {
        // set the map centre on the middle of Denmark
        var lat = 56;
        var lon = 12;
        var zoom = 7;
        mapModule.map.setCenter(new OpenLayers.LonLat(lon, lat).transform(this.projectionWGS84, this.projectionSphericalMercator), zoom)
    },

    registerEventHandlers: function () {
        mapModule.map.events.register('move', map, this.onMapMove);
        mapModule.map.events.register('moveend', map, this.onMapMoveEnd);
        mapModule.map.events.register('mousemove', map, this.onMapMouseMove);

        $('#cells-force-load').click(this.onForceLoadCells);
    },

    onMapMove: function (evt) {
        mapModule.userOutputUpdateViewPortInfo();
    },

    onMapMoveEnd: function (evt) {
        if (mapModule.map.zoom >= 12) {
            if (mapModule.isGridLayerVisible == false) {
                console.log("Turning on GridLayer.");
                mapModule.showGridLayer();
            } else {
                featureModule.loadCells();
                console.log("GridLayer is already visible.")
            }
        } else {
            console.log("Turning off GridLayer.");
            mapModule.hideGridLayer();
        }
    },

    onMapMouseMove: function (evt) {
        mapModule.userOutputUpdateCursorPos(evt.xy);
    },

    onForceLoadCells: function (evt) {
        mapModule.showGridLayer();
    },

    showGridLayer: function () {
        var gridLayer = mapModule.map.getLayersByName("DMA grid layer")[0];
        // Display grid layer
        if (gridLayer) {
            gridLayer.display(true);
        } else {
            mapModule.constructGridLayer();
        }
        // Load cells and data (required grid layer to be constructed)
        featureModule.loadCells();
        // Book-keeping
        mapModule.isGridLayerVisible = true;
        $('#cell-layer-display-status').html('Cell layer visible.');
    },

    hideGridLayer: function () {
        var gridLayer = mapModule.map.getLayersByName("DMA grid layer")[0];
        if (gridLayer) {
            gridLayer.display(false);

            // Book-keeping
            mapModule.isGridLayerVisible = false;
            $('#cell-layer-display-status').html('Cell layer hidden.');
        }
    },

    getGridLayer: function () {
        return mapModule.map.getLayersByName("DMA grid layer")[0];
    },

    constructGridLayer: function () {
        // allow testing of specific renderers via "?renderer=Canvas", etc
        var renderer = OpenLayers.Util.getParameters(window.location.href).renderer;
        renderer = (renderer) ? [renderer] : OpenLayers.Layer.Vector.prototype.renderers;

        // we want opaque external graphics and non-opaque internal graphics
        var layerStyle = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default']);
        layerStyle.fillOpacity = 0.2;
        layerStyle.graphicOpacity = 1;

        var gridLayer = new OpenLayers.Layer.Vector("DMA grid layer", {
            style: layerStyle,
            renderers: renderer,
            eventListeners: this.gridLayerFeatureListeners
        });

        mapModule.map.addLayer(gridLayer);
    },

    userOutputUpdateCursorPos: function (screenpos) {
        var cursorPos = this.map.getLonLatFromPixel(screenpos);

        var p = new OpenLayers.Geometry.Point(cursorPos.lon, cursorPos.lat);
        p.transform(this.map.getProjectionObject(), this.projectionWGS84);

        var lat = OpenLayers.Util.getFormattedLonLat(p.y, 'lat');
        var lon = OpenLayers.Util.getFormattedLonLat(p.x, 'lon');

        $('#cursorpos').html("<p>(" + lat + "," + lon + ")</p>");
    },

    userOutputUpdateViewPortInfo: function () {
        var viewport = this.map.getExtent();

        var nw = new OpenLayers.Geometry.Point(viewport.left, viewport.top);
        nw.transform(this.map.getProjectionObject(), this.projectionWGS84);
        var se = new OpenLayers.Geometry.Point(viewport.right, viewport.bottom);
        se.transform(this.map.getProjectionObject(), this.projectionWGS84);

        var north = OpenLayers.Util.getFormattedLonLat(nw.y, 'lat');
        var west = OpenLayers.Util.getFormattedLonLat(nw.x, 'lon');
        var south = OpenLayers.Util.getFormattedLonLat(se.y, 'lat');
        var east = OpenLayers.Util.getFormattedLonLat(se.x, 'lon');

        $('#viewport').html("<p>(" + north + "," + west + ")<br/>(" + south + "," + east + ")</p>");
    },

    gridLayerFeatureListeners: {
        featureclick: function (e) {
            var feature = e.feature;
            var cell = feature.data;

            featureModule.userOutputShowCellData(cell);

            return false;
        },
        nofeatureclick: function (e) {
            console.log(e.object.name + " says: No feature clicked.");
        },
        featureover: function (e) {
            // console.log(e.object.name + " says: " + e.feature.id + " hovered.");
        },
        featureout: function (e) {
            // console.log(e.object.name + " says: " + e.feature.id + " left.");
        }
    }

};

