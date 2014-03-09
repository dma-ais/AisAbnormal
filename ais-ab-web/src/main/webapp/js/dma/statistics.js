/**
 * This javascript module handles loading, processing, display and user interaction with statistic data.
 */

var statisticsModule = {

    categories: null,

    init: function () {
        this.userOutputClearCellData();
        this.userOutputMetadata();

        // init categories
        var cellResourceService = "/abnormal/rest/cell";
        $.getJSON(cellResourceService, {
            north: 56.00001,
            east: 12.00001,
            south: 56.00000,
            west: 12.00000
        }).done(function (celldata) {
            statisticsModule.categories = celldata.metadata.categories;
        });
    },

    loadCells: function () {
        $('#cell-layer-load-status').html('Loading cells...');
        $("body").css("cursor", "progress");

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
        }).done(function (celldata) {
                statisticsModule.categories = celldata.metadata.categories;

                $('#cell-layer-load-status').html('Processing cells...');
                var numCellsAdded = 0;
                var gridLayer = mapModule.getGridLayer();
                $.each(celldata.cells, function (i, cell) {
                    var cellAlreadyLoadded = gridLayer.getFeatureByFid(cell.cellId);
                    if (!cellAlreadyLoadded) {
                        statisticsModule.preProcessCell(cell);
                        statisticsModule.addCell(cell);
                        numCellsAdded++;
                    }
                });
                $('#cell-layer-load-status').html(celldata.cells.length + ' cells loaded, ' + numCellsAdded + " added to map.");
                $("body").css("cursor", "default");
            }).fail(function (jqXHR, textStatus) {
                $('#cell-layer-load-status').html("Cell load failed: " + textStatus);
                $("body").css("cursor", "default");
            });
    },

    preProcessCell: function (cell) {
        // add totalcounts to cell
        cell.totalShipCount = [];
        $.each(cell.statistics, function (fdidx, fd) {
            if (fd.statisticDataType == 'ThreeKeyMap') {
                cell.totalShipCount[fd.statisticName] = 0;
                $.each(fd.data, function(key1, value1) {
                    $.each(value1, function(key2, statistic) {
                        cell.totalShipCount[fd['statisticName']] += parseInt(statistic.shipCount);
                    })
                })
            } else if (fd.statisticDataType == 'FourKeyMap') {
                cell.totalShipCount[fd.statisticName] = 0;
                $.each(fd.data, function(key1, value1) {
                    $.each(value1, function(key2, value2) {
                        $.each(value2, function(key3, statistic) {
                            cell.totalShipCount[fd['statisticName']] += parseInt(statistic.shipCount);
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
        var totalShipCount = cell.totalShipCount["ShipTypeAndSizeStatisticData"];
        if (totalShipCount >= 1000) {
            strokeColor = "#66cc66";
        }
        var fillOpacity = 0.05 + (Math.min(totalShipCount/500, 1))*0.5;

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
        var meaningOfKey1 = fd.meaningOfKey1;
        var meaningOfKey2 = fd.meaningOfKey2;

        var tableHtml = "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"dataTable\" style=\"width: 260px;\" id=\"statistic-table\">";
        tableHtml += "<thead>";
        tableHtml += "<tr>";
        tableHtml += "<th></th>";
        $.each(statisticsModule.categories[meaningOfKey2], function(key2, value2) {
            var category2 = parseInt(key2);
            var label2 = statisticsModule.categories[meaningOfKey2][category2];
            tableHtml += "<th>" + label2 + "</th>";
        });
        tableHtml += "</tr>";
        tableHtml += "</thead>";
        tableHtml += "<tbody>";
        $.each(statisticsModule.categories[meaningOfKey1], function(key1, value1) {
            tableHtml += "<tr>";
            var category1 = parseInt(key1);
            var label1 = statisticsModule.categories[meaningOfKey1][category1];
            tableHtml += "<td>" + label1 + "</td>";
            $.each(statisticsModule.categories[meaningOfKey2], function(key2, value2) {
                var shipCount = '';
                var stats = '';
                try {
                    shipCount = fd.data[key1][key2].shipCount;
                    var pd = 100 * shipCount / totalShipCount;
                    var stats = (Math.round(pd * 100) / 100).toFixed(2) + '%';
                } catch(e) {
                    // Expected behaviour for statistics with no shipCount
                }
                tableHtml += '<td class="percentage">' + stats + '</td>';
            });
            tableHtml += "</tr>";
        });
        tableHtml += "</tbody>";
        tableHtml += "</table>";

        parentNode.append(tableHtml);
        parentNode.append("<div>Total ship count is " + totalShipCount + ".</div>");

        parentNode.find('#statistic-table').dataTable({
            "bFilter": false,
            "bInfo": false,
            "bPaginate": false,
            "bLengthChange": false,
            "bSort": false,
            "bAutoWidth": false
        });
    },

    userOutputCreateFourKeyMap: function (parentNode, fd, totalShipCount) {
        var meaningOfKey1 = fd.meaningOfKey1;
        var meaningOfKey2 = fd.meaningOfKey2;
        var meaningOfKey3 = fd.meaningOfKey3;

        var tableHtml = "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"dataTable\" style=\”width: 260px;\” id=\"statistic-table\">";
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

        $.each(statisticsModule.categories[meaningOfKey1], function(key1, value1) {
            var category1 = parseInt(key1);
            var label1 = statisticsModule.categories[meaningOfKey1][category1];
            $.each(statisticsModule.categories[meaningOfKey2], function(key2, value2) {
                var category2 = parseInt(key2);
                var label2 = statisticsModule.categories[meaningOfKey2][category2];
                $.each(statisticsModule.categories[meaningOfKey3], function(key3, value3) {
                    var category3 = parseInt(key3);
                    var label3 = statisticsModule.categories[meaningOfKey3][category3];
                    var shipCount = '';
                    var stats = '';
                    try {
                        shipCount = fd.data[key1][key2][key3].shipCount;
                        var pd = 100 * shipCount / totalShipCount;
                        stats = (Math.round(pd * 100) / 100).toFixed(2) + "%";
                    } catch(e) {
                        // Expected behaviour for statistics with no shipCount
                    }
                    tableHtml +=
                            "<tr>" +
                            "<td>" + label1 + "</td>" +
                            "<td>" + label2 + "</td>" +
                            "<td>" + label3 + "</td>" +
                            "<td class='count'>" + shipCount + "</td>" +
                            "<td class='percentage'>" + stats + "</td>" +
                            "</tr>";
                })
            })
        })

        tableHtml += "</tbody>";
        tableHtml += "</table>";

        parentNode.append(tableHtml);
        parentNode.append("<div>Total ship count is " + totalShipCount + ".</div>");

        parentNode.find('#statistic-table').dataTable({
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

        cell.statistics = cell.statistics.sort(
            function(a, b) {
                if (a.statisticName > b.statisticName) {
                    return 1;
                } else if (a.statisticName < b.statisticName) {
                    return -1;
                } else {
                    return 0;
                };
            }
        );

        $.each(cell.statistics, function (i, fd) {
            var statisticName = fd.statisticName;
            var statisticType = fd.statisticDataType;
            var totalShipCount = cell.totalShipCount[statisticName];

            $("#cell-data-tabs ul").append('<li><a id="tab-' + statisticName.replace('StatisticData','') + '" href="#cell-data-tab-' + i + '">' + statisticName.replace('StatisticData','') + '</a></li>');
            $("#cell-data-tabs").append('<div id="cell-data-tab-' + i + '"></div>');

            var tabRoot = $("#cell-data-tabs #cell-data-tab-" + i);

            if (statisticType == 'ThreeKeyMap') {
                statisticsModule.userOutputCreateThreeKeyMap(tabRoot, fd, totalShipCount);
            } else if (statisticType == 'FourKeyMap') {
                statisticsModule.userOutputCreateFourKeyMap(tabRoot, fd, totalShipCount);
            } else {
                tabRoot.append("Cannot display cell data of type " + statisticType + ".");
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

        statisticsModule.userOutputCreateCellDataTabs(cellData, cell);
    },

    userOutputMetadata: function () {
        var StatisticsResourceService = "/abnormal/rest/statistics/";
        $.getJSON(StatisticsResourceService)
            .done(function (Statistics) {
                $('#gridsize > .data').html(Statistics[0].gridResolution * 40075000.0 / 360 + ' m');
                $('#downsampling > .data').html(Statistics[0].downsampling + ' secs');

                var html = "<ul>";
                var statisticNames = Statistics[1].statisticNames;
                $.each(statisticNames, function (i, statisticName) {
                    html += "<li>" + statisticName + "</li>";
                });
                html += "</ul>";
                $('#statistic-names').after(html);
            })
            .fail(function (jqXHR, textStatus) {
                console.error(textStatus);
            });
    }

};
