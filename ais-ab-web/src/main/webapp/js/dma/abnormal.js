var dmaAbnormalApp = {

    map: null,
    isGridLayerVisible: false,
    projectionWGS84: null,
    projectionSphericalMercator: null,

    init: function () {
        this.projectionWGS84 = new OpenLayers.Projection("EPSG:4326");
        this.projectionSphericalMercator = new OpenLayers.Projection("EPSG:900913");

        this.map = new OpenLayers.Map("map");
        //this.map.addLayer(new OpenLayers.Layer.OSM("map", "http://homer/osmtiles/${z}/${x}/${y}.png"));
        this.map.addLayer(new OpenLayers.Layer.OSM());

        var zoom = new OpenLayers.Control.Zoom();
        this.map.addControl(zoom);
        zoom.activate();

        this.userOutputMetadata();
        this.userOutputClearCellData();

        this.registerEventHandlers();
        this.zoomToDenmark();
    },

    zoomToDenmark: function () {
        // set the map centre on the middle of Denmark
        var lat = 56;
        var lon = 12;
        var zoom = 7;
        dmaAbnormalApp.map.setCenter(new OpenLayers.LonLat(lon, lat).transform(this.projectionWGS84, this.projectionSphericalMercator), zoom)
    },

    registerEventHandlers: function () {
        dmaAbnormalApp.map.events.register('move', map, this.onMapMove);
        dmaAbnormalApp.map.events.register('moveend', map, this.onMapMoveEnd);
        dmaAbnormalApp.map.events.register('mousemove', map, this.onMapMouseMove);

        $('#cells-force-load').click(this.onForceLoadCells);
    },

    onMapMove: function (evt) {
        dmaAbnormalApp.userOutputUpdateViewPortInfo();
    },

    onMapMoveEnd: function (evt) {
        if (dmaAbnormalApp.map.zoom >= 12) {
            if (dmaAbnormalApp.isGridLayerVisible == false) {
                console.log("Turning on GridLayer.");
                dmaAbnormalApp.showGridLayer();
            } else {
                dmaAbnormalApp.loadCells();
                console.log("GridLayer is already visible.")
            }
        } else {
            console.log("Turning off GridLayer.");
            dmaAbnormalApp.hideGridLayer();
        }
    },

    onMapMouseMove: function (evt) {
        dmaAbnormalApp.userOutputUpdateCursorPos(evt.xy);
    },

    onForceLoadCells: function (evt) {
        dmaAbnormalApp.showGridLayer();
    },

    showGridLayer: function () {
        var gridLayer = dmaAbnormalApp.map.getLayersByName("DMA grid layer")[0];
        // Display grid layer
        if (gridLayer) {
            gridLayer.display(true);
        } else {
            dmaAbnormalApp.constructGridLayer();
        }
        // Load cells and data (required grid layer to be constructed)
        dmaAbnormalApp.loadCells();
        // Book-keeping
        dmaAbnormalApp.isGridLayerVisible = true;
        $('#cell-layer-display-status').html('Cell layer visible.');
    },

    hideGridLayer: function () {
        var gridLayer = dmaAbnormalApp.map.getLayersByName("DMA grid layer")[0];
        if (gridLayer) {
            gridLayer.display(false);

            // Book-keeping
            dmaAbnormalApp.isGridLayerVisible = false;
            $('#cell-layer-display-status').html('Cell layer hidden.');
        }
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
            eventListeners: dmaAbnormalApp.gridLayerFeatureListeners
        });

        dmaAbnormalApp.map.addLayer(gridLayer);
    },

    loadCells: function () {
        $('#cell-layer-load-status').html('Loading cells...');

        var viewport = dmaAbnormalApp.map.getExtent();

        var nw = new OpenLayers.Geometry.Point(viewport.left, viewport.top);
        nw.transform(dmaAbnormalApp.map.getProjectionObject(), dmaAbnormalApp.projectionWGS84);
        var se = new OpenLayers.Geometry.Point(viewport.right, viewport.bottom);
        se.transform(dmaAbnormalApp.map.getProjectionObject(), dmaAbnormalApp.projectionWGS84);

        var cellResourceService = "/abnormal/featuredata/cell";
        $.getJSON(cellResourceService, {
            north: nw.y,
            east: se.x,
            south: se.y,
            west: nw.x
        }).done(function (cells) {
                var gridLayer = dmaAbnormalApp.map.getLayersByName("DMA grid layer")[0];
                var numCellsAdded = 0;
                $.each(cells, function (i, cell) {
                    var cellAlreadyLoadded = gridLayer.getFeatureByFid(cell.cellId);
                    if (!cellAlreadyLoadded) {
                        dmaAbnormalApp.addCell(gridLayer, cell);
                        numCellsAdded++;
                    }
                });
                $('#cell-layer-load-status').html(cells.length + ' cells loaded, ' + numCellsAdded + " added to map.");
            }).fail(function (jqXHR, textStatus) {
                $('#cell-layer-load-status').html("Cell load failed: " + textStatus);
            });
    },

    addCell: function (layer, cell) {
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
            title: "Cell " + cell.cellId,
            fillOpacity: 0.25

        };

        var cellGeometry = new OpenLayers.Geometry.LinearRing(cellCoords);
        cellFeature = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Polygon([cellGeometry]), cell, cellStyle);
        cellFeature.fid = cell.cellId;
        layer.addFeatures([cellFeature]);
    },

    userOutputUpdateCursorPos: function (screenpos) {
        var cursorPos = dmaAbnormalApp.map.getLonLatFromPixel(screenpos);

        var p = new OpenLayers.Geometry.Point(cursorPos.lon, cursorPos.lat);
        p.transform(dmaAbnormalApp.map.getProjectionObject(), dmaAbnormalApp.projectionWGS84);

        var lat = OpenLayers.Util.getFormattedLonLat(p.y, 'lat');
        var lon = OpenLayers.Util.getFormattedLonLat(p.x, 'lon');

        $('#cursorpos').html("<p>(" + lat + "," + lon + ")</p>");
    },

    userOutputUpdateViewPortInfo: function () {
        var viewport = dmaAbnormalApp.map.getExtent();

        var nw = new OpenLayers.Geometry.Point(viewport.left, viewport.top);
        nw.transform(dmaAbnormalApp.map.getProjectionObject(), dmaAbnormalApp.projectionWGS84);
        var se = new OpenLayers.Geometry.Point(viewport.right, viewport.bottom);
        se.transform(dmaAbnormalApp.map.getProjectionObject(), dmaAbnormalApp.projectionWGS84);

        var north = OpenLayers.Util.getFormattedLonLat(nw.y, 'lat');
        var west = OpenLayers.Util.getFormattedLonLat(nw.x, 'lon');
        var south = OpenLayers.Util.getFormattedLonLat(se.y, 'lat');
        var east = OpenLayers.Util.getFormattedLonLat(se.x, 'lon');

        $('#viewport').html("<p>(" + north + "," + west + ")<br/>(" + south + "," + east + ")</p>");
    },

    userOutputClearCellData: function () {
        var cellData = $('div#cell-data div.cell-data-contents');
        cellData.empty();
        cellData.append("<h5>No cell selected.</h5>");
    },

    userOutputShowCellData: function (cell) {
        var north = OpenLayers.Util.getFormattedLonLat(cell.north, 'lat');
        var east = OpenLayers.Util.getFormattedLonLat(cell.east, 'lon');
        var south = OpenLayers.Util.getFormattedLonLat(cell.south, 'lat');
        var west = OpenLayers.Util.getFormattedLonLat(cell.west, 'lon');

        var cellData = $('div#cell-data div.cell-data-contents');

        cellData.empty();
        cellData.append("<h5>Cell id " + cell.cellId + " (" + north + "," + west + ") - (" + south + "," + east + ")</h5>");

        $.each(cell.featureData, function (i, fd) {
            var featureName = fd.featureName;
            var featureType = fd.featureDataType;

            cellData.append('<br/><b>' + featureName + '</b><br/>');

            if (featureType == 'FeatureData2Key') {
                var tableHtml = "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"dataTable\" style=\”width: 260px;\” id=\"featureTable\">";
                tableHtml += "<thead>";
                tableHtml += "<tr>";
                tableHtml += "<th>" + fd.meaningOfKey1 + " &#92; " + fd.meaningOfKey2 + "</th>";
                for (var i = 1; i < 10; i++) {
                    tableHtml += "<th>" + i + "</th>";
                }
                tableHtml += "</tr>";
                tableHtml += "</thead>";
                tableHtml += "<tbody>";
                for (var i1 = 1; i1 < 10; i1++) {
                    tableHtml += "<tr>";
                    tableHtml += "<td>" + i1 + "</td>";
                    for (var i2 = 1; i2 < 10; i2++) {
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

    userOutputMetadata: function () {
        // http://localhost:8080/abnormal/featuredata/featureset/
        var featuresetResourceService = "/abnormal/featuredata/featureset/";
        $.getJSON(featuresetResourceService)
            .done(function (featureset) {
                $('#gridsize > .data').html(featureset[0].gridResolution * 40075000.0 / 360 + ' m');
                $('#downsampling > .data').html(featureset[0].downsampling + ' secs');

                var html = "<ul>";
                var featureNames = featureset[1].featureNames;
                $.each(featureNames, function (i, featureName) {
                    html += "<li>" + featureName + "</li>";
                });
                html += "</ul>";
                $('#feature-names').after(html);
            })
            .fail(function (jqXHR, textStatus) {
                console.error(textStatus);
            });
    },

    gridLayerFeatureListeners: {
        featureclick: function (e) {
            var feature = e.feature;
            var cell = feature.data;

            dmaAbnormalApp.userOutputShowCellData(cell);

            return false;
        },
        nofeatureclick: function (e) {
            console.log(e.object.name + " says: No feature clicked.");
        },
        featureover: function (e) {
            console.log(e.object.name + " says: " + e.feature.id + " hovered.");
        },
        featureout: function (e) {
            console.log(e.object.name + " says: " + e.feature.id + " left.");
        }
    }

};
