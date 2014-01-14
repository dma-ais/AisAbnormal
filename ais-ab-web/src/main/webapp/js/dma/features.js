/**
 * This javascript module handles loading, processing, display and user interaction with feature data.
 */

var featureModule = {

    init: function () {
        this.userOutputClearCellData();
        this.userOutputMetadata();
    },

    loadCells: function () {
        $('#cell-layer-load-status').html('Loading cells...');

        var viewport = mapModule.map.getExtent();

        var nw = new OpenLayers.Geometry.Point(viewport.left, viewport.top);
        nw.transform(mapModule.map.getProjectionObject(), mapModule.projectionWGS84);
        var se = new OpenLayers.Geometry.Point(viewport.right, viewport.bottom);
        se.transform(mapModule.map.getProjectionObject(), mapModule.projectionWGS84);

        var cellResourceService = "/abnormal/rest/cell";
        $.getJSON(cellResourceService, {
            north: nw.y,
            east: se.x,
            south: se.y,
            west: nw.x
        }).done(function (cells) {
                $('#cell-layer-load-status').html('Processing cells...');
                var numCellsAdded = 0;
                var gridLayer = mapModule.getGridLayer();
                $.each(cells, function (i, cell) {
                    var cellAlreadyLoadded = gridLayer.getFeatureByFid(cell.cellId);
                    if (!cellAlreadyLoadded) {
                        featureModule.preProcessCell(cell);
                        featureModule.addCell(cell);
                        numCellsAdded++;
                    }
                });
                $('#cell-layer-load-status').html(cells.length + ' cells loaded, ' + numCellsAdded + " added to map.");
            }).fail(function (jqXHR, textStatus) {
                $('#cell-layer-load-status').html("Cell load failed: " + textStatus);
            });
    },

    preProcessCell: function (cell) {
        // add totalcounts to cell
        cell.totalShipCount = [];
        $.each(cell.featureData, function (fdidx, fd) {
            cell.totalShipCount[fd.featureName] = 0;
            $.each(fd.data, function(key1, value1) {
                $.each(value1, function(key2, statistic) {
                    cell.totalShipCount[fd['featureName']] += parseInt(statistic.shipCount);
                })
            })
        });
    },

    addCell: function (cell) {
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

        var strokeColor = "#aaaaaa";
        var totalShipCount = cell.totalShipCount["ShipTypeAndSizeData"];
        if (totalShipCount >= 1000) {
            strokeColor = "#66cc66";
        }
        var fillOpacity = 0.05 + (Math.min(totalShipCount/500, 1))*0.9;

        var strokeOpacity = fillOpacity;

        var cellStyle = {
            strokeColor: strokeColor,
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

            if (featureType == 'FeatureDataTwoKey') {
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
            tableHtml = "<div>(total ship count is " + cell.totalShipCount[featureName] + ")</div>" + tableHtml;
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
        var featuresetResourceService = "/abnormal/rest/featureset/";
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
    }

};
