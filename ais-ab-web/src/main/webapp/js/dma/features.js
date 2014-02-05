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
            if (fd.featureDataType == 'ThreeKeyMap') {
                cell.totalShipCount[fd.featureName] = 0;
                $.each(fd.data, function(key1, value1) {
                    $.each(value1, function(key2, statistic) {
                        cell.totalShipCount[fd['featureName']] += parseInt(statistic.shipCount);
                    })
                })
            } else if (fd.featureDataType == 'FourKeyMap') {
                cell.totalShipCount[fd.featureName] = 0;
                $.each(fd.data, function(key1, value1) {
                    $.each(value1, function(key2, value2) {
                        $.each(value2, function(key3, statistic) {
                            cell.totalShipCount[fd['featureName']] += parseInt(statistic.shipCount);
                        })
                    })
                })
            }
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
        var totalShipCount = cell.totalShipCount["ShipTypeAndSizeFeatureData"];
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

    userOutputCreateThreeKeyMap: function (parentNode, fd, totalShipCount) {
        var meaningOfKey1 = fd.meaningOfKey1.replace('ship','');
        var meaningOfKey2 = fd.meaningOfKey2.replace('ship','');

        var tableHtml = "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"dataTable\" style=\”width: 260px;\” id=\"feature-table\">";
        tableHtml += "<thead>";
        tableHtml += "<tr>";
        tableHtml += "<th>" + meaningOfKey1 + " &#92; " + meaningOfKey2 + "</th>";
        for (var i = 0; i < 10; i++) {
            tableHtml += "<th>" + parseInt(i+1) + "</th>";
        }
        tableHtml += "</tr>";
        tableHtml += "</thead>";
        tableHtml += "<tbody>";
        $.each(fd.data, function(key1, value1) {
            tableHtml += "<tr>";
            var i = parseInt(key1) + 1;
            tableHtml += "<td>" + i + "</td>";
            for (var key2 = 0; key2 < 10; key2++) {
                tableHtml += "<td>";
                if (value1) {
                    var value2 = value1[key2];
                    if (value2) {
                        var pd = 100 * value2.shipCount / totalShipCount;
                        var stats = (Math.round(pd * 100) / 100).toFixed(2);
                        if (stats) {
                            tableHtml += stats + "%";
                        }
                    }
                }
                tableHtml += "</td>";
            }
            tableHtml += "</tr>";
        });
        tableHtml += "</tbody>";
        tableHtml += "</table>";

        parentNode.append(tableHtml);
        parentNode.append("<div>Total ship count is " + totalShipCount + ".</div>");

        parentNode.find('#feature-table').dataTable({
            "bFilter": false,
            "bInfo": false,
            "bPaginate": false,
            "bLengthChange": false,
            "bSort": false,
            "bAutoWidth": false
        });
    },

    userOutputCreateFourKeyMap: function (parentNode, fd, totalShipCount) {
        var meaningOfKey1 = fd.meaningOfKey1.replace('ship','');
        var meaningOfKey2 = fd.meaningOfKey2.replace('ship','');
        var meaningOfKey3 = fd.meaningOfKey3;

        var tableHtml = "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"dataTable\" style=\”width: 260px;\” id=\"feature-table\">";
        tableHtml += "<thead>";
        tableHtml += "<tr>";
        tableHtml += "<th>" + meaningOfKey1 + "</th>"
        tableHtml += "<th>" + meaningOfKey2 + "</th>"
        tableHtml += "<th>" + meaningOfKey3 + "</th>"
        tableHtml += "<th>" + "shipCount" + "</th>"
        tableHtml += "<th>" + "pd" + "</th>"
        tableHtml += "</tr>";
        tableHtml += "</thead>";
        tableHtml += "<tbody>";

        $.each(fd.data, function(key1, value1) {
            $.each(value1, function(key2, value2) {
                $.each(value2, function(key3, statistic) {
                    if (statistic) {
                        var pd = 100 * statistic.shipCount / totalShipCount;
                        var stats = (Math.round(pd * 100) / 100).toFixed(2);
                        if (stats) {
                            tableHtml +=
                                "<tr>" +
                                    "<td>" + (parseInt(key1)+parseInt(1)) + "</td>" +
                                    "<td>" + (parseInt(key2)+parseInt(1)) + "</td>" +
                                    "<td>" + (parseInt(key3)+parseInt(1)) + "</td>" +
                                    "<td>" + statistic.shipCount + "</td>" +
                                    "<td>" + stats + "%</td>" +
                                    "</tr>";
                        }
                    }
                })
            })
        })

        tableHtml += "</tbody>";
        tableHtml += "</table>";

        parentNode.append(tableHtml);
        parentNode.append("<div>Total ship count is " + totalShipCount + ".</div>");

        parentNode.find('#feature-table').dataTable({
            "bFilter": false,
            "bInfo": false,
            "bPaginate": false,
            "bLengthChange": false,
            "bSort": false,
            "bAutoWidth": false
        });
    },

    userOutputCreateCellDataTabs: function (cellDataDomNode, cell) {
        cellDataDomNode.append('<div class="tabs" id="cell-data-tabs"><ul></ul></div>');

        cell.featureData = cell.featureData.sort(
            function(a, b) {
                if (a.featureName > b.featureName) {
                    return 1;
                } else if (a.featureName < b.featureName) {
                    return -1;
                } else {
                    return 0;
                };
            }
        );

        $.each(cell.featureData, function (i, fd) {
            var featureName = fd.featureName;
            var featureType = fd.featureDataType;
            var totalShipCount = cell.totalShipCount[featureName];

            $("#cell-data-tabs ul").append('<li><a id="tab-' + featureName.replace('FeatureData','') + '" href="#cell-data-tab-' + i + '">' + featureName.replace('FeatureData','') + '</a></li>');
            $("#cell-data-tabs").append('<div id="cell-data-tab-' + i + '"></div>');

            var tabRoot = $("#cell-data-tabs #cell-data-tab-" + i);

            if (featureType == 'ThreeKeyMap') {
                featureModule.userOutputCreateThreeKeyMap(tabRoot, fd, totalShipCount);
            } else if (featureType == 'FourKeyMap') {
                featureModule.userOutputCreateFourKeyMap(tabRoot, fd, totalShipCount);
            } else {
                tabRoot.append("Cannot display cell data of type " + featureType + ".");
            }
        });

        $('.tabs#cell-data-tabs').tabs();
    },

    userOutputShowCellData: function (cell) {
        var north = OpenLayers.Util.getFormattedLonLat(cell.north, 'lat');
        var east = OpenLayers.Util.getFormattedLonLat(cell.east, 'lon');
        var south = OpenLayers.Util.getFormattedLonLat(cell.south, 'lat');
        var west = OpenLayers.Util.getFormattedLonLat(cell.west, 'lon');

        var cellData = $('div#cell-data div.cell-data-contents');

        cellData.empty();
        cellData.append("<h5>Cell id " + cell.cellId + " (" + north + "," + west + ") - (" + south + "," + east + ")</h5>");

        featureModule.userOutputCreateCellDataTabs(cellData, cell);
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
