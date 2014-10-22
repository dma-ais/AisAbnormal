/**
 * This javascript module handles loading, processing, display and user interaction with event data.
 */

var eventModule = {

    searchResults: new Array(),
    eventResourceService: "/abnormal/rest/event",

    init: function () {
        $("#events-remove").click(function() {
            eventModule.removeAllEvents();
        });
    },

    onEventSearchModalLoaded: function() {
        $('.tabs#event-search-tabs').tabs();
        $('#search-event-24h').click(eventModule.setEventSearchPeriod24h);
        $('#event-search-by-id').click(eventModule.findEventById);
        $('#event-search-by-other').click(eventModule.findEventByCriteria);

        $('div#event-search-warning').hide();

        $('#search-event-type').empty();
        $('#search-event-type').append('<option value="any">Any</option>');
        var eventTypeRequest = eventModule.eventResourceService + "/type"
        $.getJSON(eventTypeRequest).done(function (eventTypes) {
            $.each(eventTypes, function (idx, eventType) {
                var desc = eventModule.camelCaseToSentenceCase(eventType);
                $('#search-event-type').append('<option value="' + eventType + '">' + desc + '</option>');
            });
        }).fail(function (jqXHR, textStatus) {
            console.error("Failed to load event types: " + textStatus + " " + jqXHR.status + " " + jqXHR.statusText);
        });

        $("#event-search-modal .search-results .search-show-all").hide();
        $("#event-search-modal .search-results .search-show-all").click(function() {
            eventModule.visualizeAllSearchResults();
            $('#event-search-modal').modal('hide');
        });

        $('#events-shown').dataTable({
            "bFilter": false,
            "bInfo": false,
            "bPaginate": false,
            "bLengthChange": true,
            "bSort": true,
            "bAutoWidth": false,
            "aoColumnDefs": [
                {   "sWidth": "20px", "aTargets": [0] },
                {   "sWidth": "212px", "aTargets": [1] },
                {   "sWidth": "8px", "aTargets": [2] }
            ]
        });
    },

    camelCaseToSentenceCase: function(camelCase) {
        return camelCase.replace('Event','').replace(/([A-Z])/g, ' $1').substr(1).replace(/ \w\S*/g, function(txt){return txt.substr(0).toLowerCase();});
    },

    removeAllEvents: function() {
        mapModule.getVesselLayer().removeAllFeatures();
        eventModule.synchronizeEventsOnMapWithTable();
    },

    formatTimestamp: function (t) {
        var res = "";
        if (t > 0) {
            var time = new Date(0);
            time.setUTCSeconds(t / 1000);
            res = eventModule.formatDate(time);
        }
        return res;
    },

    formatDate: function(d) {
        var curr_date = d.getDate();
        if (curr_date < 10)
            curr_date = "0" + curr_date;
        var curr_month = d.getMonth();
        curr_month++;
        if (curr_month < 10)
            curr_month = "0" + curr_month;
        var curr_year = d.getFullYear();
        var curr_hour = d.getHours();
        if (curr_hour < 10)
            curr_hour = "0" + curr_hour;
        var curr_min = d.getMinutes();
        if (curr_min < 10)
            curr_min = "0" + curr_min;
        return curr_date + "/" + curr_month + "/" + curr_year + " " + curr_hour + ":" + curr_min;
    },

    parseDate: function(dateString) {
        var match = /^(\d?\d)\/(\d?\d)\/(\d{4}) (\d?\d):(\d\d)/.exec(dateString);
        var dom = Number(match[1]);
        var mon = Number(match[2])-1;
        var year = Number(match[3]);
        var hour = Number(match[4]);
        var min = Number(match[5]);
        return new Date(year, mon, dom, hour, min);
    },

    setSearchStarted: function(statusText) {
        $("#event-search-modal .search-results .search-status").empty();
        $("#event-search-modal .search-results .search-status").append(statusText);
        $("#event-search-modal .search-results .search-show-all").hide();
    },

    setSearchCompleted: function(statusText) {
        $("#event-search-modal .search-results .search-status").empty();
        $("#event-search-modal .search-results .search-status").append(statusText);

        if ($('#event-search-modal .search-results .search-data tbody tr').length < 100) {
            $("#event-search-modal .search-results .search-show-all").show();
        } else {
            $("#event-search-modal .search-results .search-show-all").hide();
        }
    },

    clearSearchResults: function () {
        $("#event-search-modal .search-results .search-status").empty();
        $("#event-search-modal .search-results .search-show-all").hide();

        var searchResults = $('#event-search-modal .search-results .search-data');
        searchResults.empty();

        var tableHtml  = "<table id='event-search-results' class='table'>"
        tableHtml += "<thead><tr>";
        tableHtml += "<td class=\"no-sort\"></td><td>Id</td><td>Type</td><td data-date-format=\"ddmmyyyy\">Start</td><td>Type</td><td>LOA</td><td>Vessel</td><td class=\"no-sort\"></td>";
        tableHtml += "</tr></thead><tbody></tbody>";
        tableHtml += "</table>";

        searchResults.append(tableHtml);

        eventModule.searchResults = new Array();
    },

    addSearchResult: function(event) {
        try {
            var eventStart = eventModule.formatTimestamp(event.startTime);
            var eventType = event.eventType.replace('Event', '');
            var shipType = event.behaviours[0].vessel.type;
            var shipLength = event.behaviours[0].vessel.toBow + event.behaviours[0].vessel.toStern;
            var shipName = event.behaviours[0].vessel.name;

            var searchResultHtml = "<tr id='event-search-result-event-" + event.id + "'>";
            searchResultHtml += "<td class='glyphicon-1'><span id='result-remove-" + event.id + "' class='glyphicon glyphicon-remove-sign' data-toggle='tooltip' title='Permanently suppress event'></span></td>";
            searchResultHtml += "<td class='text-right'>" + event.id + "</td>";
            searchResultHtml += "<td>" + eventModule.camelCaseToSentenceCase(eventType);
            +"</td>";
            searchResultHtml += "<td>" + eventStart + "</td>";
            searchResultHtml += "<td class='text-right'>" + shipType + "</td>";
            searchResultHtml += "<td class='text-right'>" + shipLength + "</td>";
            searchResultHtml += "<td>" + shipName + "</td>";
            searchResultHtml += "<td class='glyphicon-1'><span id='result-show-" + event.id + "' class='glyphicon glyphicon-globe' data-toggle='tooltip' title='Show event on map'></span></td>";
            searchResultHtml += "</tr>";

            $('#event-search-modal .search-results .search-data tbody').append(searchResultHtml);

            $("#event-search-modal .search-results #result-show-" + event.id).on("click", function () {
                eventModule.visualizeEvent(event);
                $('#event-search-modal').modal('hide');
            });

            $("#event-search-modal .search-results #result-remove-" + event.id).on("click", function () {
                eventModule.suppressEvent(event);
            });

            eventModule.searchResults.push(event);
        } catch(e) {
            console.log(e);
        }
    },

    findEventByCriteria: function () {
        eventModule.clearSearchResults();

        var queryParams = {
            from: $('#search-event-from').val(),
            to: $('#search-event-to').val(),
            vessel: $('#search-event-vessel').val().replace("*","%")
        };
        if ($('#search-event-type').val().toLowerCase() != 'any') {
            queryParams['type'] = $('#search-event-type').val();
        }
        if ($('input#search-event-inarea').is(':checked')) {
            var bounds = mapModule.getCurrentViewportExtent();
            queryParams['north'] = bounds.top;
            queryParams['east'] = bounds.right;
            queryParams['south'] = bounds.bottom;
            queryParams['west'] = bounds.left;
        }

        var fromEntered = queryParams.from != undefined && $.trim(queryParams.from).length >= 10;
        var toEntered = queryParams.to != undefined && $.trim(queryParams.to).length >= 10;
        var vesselEntered = queryParams.vessel != undefined && $.trim(queryParams.vessel).length >= 1;

        if (vesselEntered || (fromEntered && toEntered)) {
            eventModule.setSearchStarted("Searching...");
            if ($('div#event-search-warning').is(':visible')) {
                $('div#event-search-warning').slideUp("fast");
            }

            var start_time = new Date();

            var eventRequest = eventModule.eventResourceService + "?" + $.param(queryParams);

            $.getJSON(eventRequest).done(function (events) {
                $.each(events, function (idx, event) {
                    eventModule.addSearchResult(event);
                });
                $("table#event-search-results").tablesorter(
                    {
                        theme: 'bootstrap',
                        headerTemplate: '{content} {icon}',
                        widgets: ['zebra', 'columns', 'uitheme'],
                        headers: {
                            '.no-sort': {
                                sorter: false
                            }
                        }
                    }
                );

                var end_time = new Date();

                eventModule.setSearchCompleted("Found " + events.length + " matching events in " + (end_time-start_time) + " msecs.");
            }).fail(function (jqXHR, textStatus) {
                eventModule.setSearchCompleted("Search failed. Try to narrow your search for fewer expected results. [" + jqXHR.status + " " + jqXHR.statusText + "]");
            });
        } else {
            $('div#event-search-warning').empty();
            $('div#event-search-warning').append("<div><b>ERROR! Please enter either 'from'+'to' or 'vessel'.</b></div>");

            if (! $('div#event-search-warning').is(':visible')) {
                    $('div#event-search-warning').slideDown("fast");
            }
        }

    },

    findEventById: function () {
        eventModule.clearSearchResults();
        eventModule.setSearchStarted("Searching...");

        var eventId = $('input#search-event-id').val();
        if (eventId) {
            //http://localhost:8080/abnormal/rest/event/1
            var eventResourceService = "/abnormal/rest/event";
            var eventResource = eventResourceService + "/" + eventId;
            $.getJSON(eventResource).done(function (event) {
                eventModule.addSearchResult(event);
                eventModule.setSearchCompleted("Found " + (event ? "":"no ") + "matching event.");
            }).fail(function (jqXHR, textStatus) {
                eventModule.setSearchCompleted("Search failed [" + jqXHR.status + " " + jqXHR.statusText + "].");
            });
        }
    },

    setEventSearchPeriod24h: function() {
        var from = $('#search-event-from');
        var to = $('#search-event-to');

        if (!from.val().trim()) {
            if (!to.val().trim()) {
                var now = new Date();
                to.val(eventModule.formatDate(now));
                from.val(eventModule.formatDate(new Date(now - 24*60*60*1000)));
            } else {
                var toTime = eventModule.parseDate(to.val());
                from.val(eventModule.formatDate(new Date(toTime.getTime() - 24*60*60*1000)));
            }
        } else {
            if (!to.val().trim()) {
                var fromTime = eventModule.parseDate(from.val());
                to.val(eventModule.formatDate(new Date(fromTime.getTime() + 24*60*60*1000)));
            }
        }
    },

    expandBounds: function(bounds, meters) {
        var nw = new OpenLayers.LonLat(bounds.left, bounds.top);
        var se = new OpenLayers.LonLat(bounds.right, bounds.bottom);

        nw = OpenLayers.Util.destinationVincenty(nw, -45, meters);
        se = OpenLayers.Util.destinationVincenty(se, 135, meters);

        bounds.extend(nw);
        bounds.extend(se);

        return bounds;
    },

    computeEventExtent: function(event) {
        var bounds = new OpenLayers.Bounds();

        $.each(event.behaviours, function (idx, behaviour) {
            $.each(behaviour.trackingPoints, function (idx, trackingPoint) {
                var point = new OpenLayers.LonLat(trackingPoint.longitude, trackingPoint.latitude);
                if (trackingPoint.eventCertainty == 'RAISED' || trackingPoint.eventCertainty == 'UNCERTAIN') {
                    bounds.extend(point);
                }
            });
        });

        return eventModule.expandBounds(bounds, 1000);
    },

    visualizeAllSearchResults: function() {
        $.each(eventModule.searchResults, function(i, event) {
            eventModule.visualizeEvent(event);
        });
    },

    visualizeEventId: function(eventId)  {
        var eventResourceService = "/abnormal/rest/event";
        var eventResource = eventResourceService + "/" + eventId;
        $.getJSON(eventResource).done(function (event) {
            eventModule.visualizeEvent(event);
        }).fail(function (jqXHR, textStatus) {
            console.error("Search failed [" + jqXHR.status + " " + jqXHR.statusText + "].");

        });
    },

    visualizeEvent: function(event) {
        vesselModule.addEvent(event);
        eventModule.synchronizeEventsOnMapWithTable();
    },

    suppressEvent: function(event) {
        var eventRequest = eventModule.eventResourceService + '/' + event.id + '/suppress';

        $.ajax({url: eventRequest, type: 'PUT', contentType: 'text/plain', data: 'true', processData: false}).done(function (events) {
            $('#event-search-result-event-' + event.id).remove();
        }).fail(function (jqXHR, textStatus) {
            console.log(jqXHR);
            eventModule.setSearchCompleted("Error [" + jqXHR.status + " " + jqXHR.statusText + "].");
        });
    },

    synchronizeEventsOnMapWithTable: function() {
        var eventTable = $('#events-shown');
        var events = vesselModule.getAllEvents();

        eventTable.dataTable().fnClearTable();

        $.each(events, function (i, event) {
            eventTable.dataTable().fnAddData([
                event.fid.substring('event-'.length),
                event.data.jsonEvent.behaviours[0].vessel.name,
                "<span id='" + event.fid + "' class='glyphicon glyphicon-film'></span>"
            ]);

            $("#events-shown #" + event.fid).on("click", function() {
                eventModule.visualizeEvent(event.data.jsonEvent);
            });
        });
    }
};
