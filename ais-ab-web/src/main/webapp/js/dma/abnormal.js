var dmaAbnormalApp = {

    map: null,
    isGridLayerVisible: false,
    projectionWGS84: null,
    projectionSphericalMercator: null,

    init: function() {
        this.projectionWGS84 = new OpenLayers.Projection("EPSG:4326");
        this.projectionSphericalMercator = new OpenLayers.Projection("EPSG:900913");

        this.map = new OpenLayers.Map("map");
        //this.map.addLayer(new OpenLayers.Layer.OSM("map", "http://homer/osmtiles/${z}/${x}/${y}.png"));
        this.map.addLayer(new OpenLayers.Layer.OSM());

        var zoom = new OpenLayers.Control.Zoom();
        this.map.addControl(zoom);
        zoom.activate();

        this.userOutputClearCellData();

        this.registerZoomEventHandler();
        this.zoomToDenmark();
    },

    zoomToDenmark: function() {
        // set the map centre on the middle of Denmark
        var lat = 56;
        var lon = 12;
        var zoom = 7;
        dmaAbnormalApp.map.setCenter(new OpenLayers.LonLat(lon,lat).transform(this.projectionWGS84,this.projectionSphericalMercator), zoom)
    },

    registerZoomEventHandler: function() {
        dmaAbnormalApp.map.events.register('zoomend', map, function(evt) {
            if (dmaAbnormalApp.map.zoom >= 15) {
                if (dmaAbnormalApp.isGridLayerVisible == false) {
                    console.log("Turning on GridLayer.")
                    dmaAbnormalApp.showGridLayer();
                    dmaAbnormalApp.isGridLayerVisible = true;
                } else {
                    console.log("GridLayer is already visible.")
                }
            } else {
                console.log("Turning off GridLayer.")
                // TODO - turned off for test only:
                // dmaAbnormalApp.hideGridLayer();
                dmaAbnormalApp.isGridLayerVisible = false;
            }
        });
    },

    showGridLayer: function() {
        var gridLayer = dmaAbnormalApp.map.getLayersByName("DMA grid layer")[0];
        if (gridLayer) {
            gridLayer.display(true);
        } else {
            dmaAbnormalApp.constructGridLayer();
        }
    },

    hideGridLayer: function() {
        var gridLayer = dmaAbnormalApp.map.getLayersByName("DMA grid layer")[0];
        if (gridLayer) {
            gridLayer.display(false);
        }
    },

    constructGridLayer: function() {
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
            eventListeners: dmaAbnormalApp.gridLayerFeatureListeners
        });

        dmaAbnormalApp.loadCells(gridLayer);
        dmaAbnormalApp.map.addLayer(gridLayer);
    },

    loadCells: function() {
        // http://localhost:8080/abnormal/featuredata/cell?north=55&east=11&south=54.91&west=10.91
        var cellResourceService = "/abnormal/featuredata/cell";
        $.getJSON( cellResourceService, {
            north: 56.1,
            east: 12.1,
            south: 56.0,
            west: 12.0
        }).done(function( cells ) {
                var gridLayer = dmaAbnormalApp.map.getLayersByName("DMA grid layer")[0];
                $.each(cells, function( i, cell ) {
                    dmaAbnormalApp.addCell(gridLayer, cell);
                });
            });
    },

    addCell: function(layer, cell) {
        var cellCoords = new Array();

        point = new OpenLayers.Geometry.Point(cell.west, cell.north);
        point.transform(dmaAbnormalApp.projectionWGS84, dmaAbnormalApp.map.getProjectionObject());
        cellCoords.push(point);

        point = new OpenLayers.Geometry.Point(cell.east, cell.north);
        point.transform(dmaAbnormalApp.projectionWGS84, dmaAbnormalApp.map.getProjectionObject());
        cellCoords.push(point);

        point = new OpenLayers.Geometry.Point(cell.east, cell.south);
        point.transform(dmaAbnormalApp.projectionWGS84, dmaAbnormalApp.map.getProjectionObject());
        cellCoords.push(point);

        point = new OpenLayers.Geometry.Point(cell.west, cell.south);
        point.transform(dmaAbnormalApp.projectionWGS84, dmaAbnormalApp.map.getProjectionObject());
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

        var cellGeometry = new OpenLayers.Geometry.LinearRing(cellCoords);
        cellFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Polygon([cellGeometry]), cell, cellStyle);
        layer.addFeatures([cellFeature]);
    },

    userOutputClearCellData: function() {
        var cellData = $('div#cell-data div.cell-data-contents');
        cellData.empty();
        cellData.append("<h5>No cell selected.</h5>");
    },

    userOutputShowCellData: function(cell) {
        var north = OpenLayers.Util.getFormattedLonLat(cell.north, 'lat');
        var east  = OpenLayers.Util.getFormattedLonLat(cell.east, 'lon');
        var south = OpenLayers.Util.getFormattedLonLat(cell.south, 'lat');
        var west  = OpenLayers.Util.getFormattedLonLat(cell.west, 'lon');

        var cellData = $('div#cell-data div.cell-data-contents');

        cellData.empty();
        cellData.append("<h5>Cell id " + cell.cellId + " (" + north + "," + west + ") - (" + south + "," + east + ")</h5>");

        $.each(cell.featureData, function(i, fd ) {
            var featureName = fd.featureName;
            var featureType = fd.featureDataType;

            cellData.append('<br/><b>' + featureName + '</b><br/>');

            if (featureType == 'FeatureData2Key') {
                var tableHtml = "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"dataTable\" style=\”width: 260px;\” id=\"featureTable\">";
                tableHtml += "<thead>";
                tableHtml += "<tr>";
                tableHtml += "<th>" + fd.meaningOfKey1 + " &#92; " + fd.meaningOfKey2 + "</th>";
                for (var i=1; i<10; i++) {
                    tableHtml += "<th>" + i + "</th>";
                }
                tableHtml += "</tr>";
                tableHtml += "</thead>";
                tableHtml += "<tbody>";
                for (var i1=1; i1<10; i1++) {
                    tableHtml += "<tr>";
                    tableHtml += "<td>" + i1 + "</td>";
                    for (var i2=1; i2<10; i2++) {
                        tableHtml += "<td>";
                        var data = fd.data;
                        if (data) {
                            var k1 = data[i1];
                            if (k1) {
                                var k2 = data[i1][i2];
                                if (k2) {
                                    var stats = data[i1][i2].shipCount;
                                    if (stats) {
                                        tableHtml += stats;
                                    }
                                }
                            }
                        }
                        tableHtml += "</td>";
                    }
                    tableHtml += "</tr>";
                }
                tableHtml += "</tbody>";
                tableHtml += "</table>";
            }
            cellData.append(tableHtml);
        });

        $('#featureTable').dataTable({
            "bFilter": false,
            "bInfo": false,
            "bPaginate": false,
            "bLengthChange": false,
            "bSort": false,
            "bAutoWidth": false
        });

    },

    gridLayerFeatureListeners: {
        featureclick: function(e) {
            var feature = e.feature;
            var cell = feature.data;

            dmaAbnormalApp.userOutputShowCellData(cell);

            return false;
        },
        nofeatureclick: function(e) {
            console.log(e.object.name + " says: No feature clicked.");
        },
        featureover: function(e) {
            console.log(e.object.name + " says: " + e.feature.id + " hovered.");
        },
        featureout: function(e) {
            console.log(e.object.name + " says: " + e.feature.id + " left.");
        }
    }

}
