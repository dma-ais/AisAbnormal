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
        $('#event-search-by-id').click(eventModule.findEventById);
        $('#event-search-by-other').click(eventModule.findEventByCriteria);

        $('#search-event-type').empty();
        $('#search-event-type').append('<option value="any">Any</option>');
        var eventTypeRequest = eventModule.eventResourceService + "/type"
        $.getJSON(eventTypeRequest).done(function (eventTypes) {
            $.each(eventTypes, function (idx, eventType) {
                var desc = eventModule.camelCaseToSentenceCase(eventType);
                $('#search-event-type').append('<option value="' + eventType + '">' + desc + '</option>');
            });
        }).fail(function (jqXHR, textStatus) {
            console.error("Failed to load event types: " + textStatus);
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
        var curr_month = d.getMonth();
        curr_month++;
        var curr_year = d.getFullYear();
        var curr_hour = d.getHours();
        if (curr_hour < 10)
        {
            curr_hour = "0" + curr_hour;
        }
        var curr_min = d.getMinutes();
        if (curr_min < 10)
        {
            curr_min = "0" + curr_min;
        }
        var curr_sec = d.getSeconds();
        if (curr_sec < 10)
        {
            curr_sec = "0" + curr_sec;
        }
        return curr_date + "-" + curr_month + "-" + curr_year + " " + curr_hour + ":" + curr_min + ":" + curr_sec;
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
        var eventStart = eventModule.formatTimestamp(event.startTime);
        var eventType = event.eventType.replace('Event','');
        var shipType = event.behaviours[0].vessel.type;
        var shipLength = event.behaviours[0].vessel.toBow + event.behaviours[0].vessel.toStern;
        var shipName = event.behaviours[0].vessel.name;

        var searchResultHtml  = "<tr id='event-search-result-event-" + event.id + "'>";
        searchResultHtml += "<td class='glyphicon-1'><span id='result-remove-" + event.id + "' class='glyphicon glyphicon-remove-sign' data-toggle='tooltip' title='Permanently suppress event'></span></td>";
        searchResultHtml += "<td class='text-right'>" + event.id + "</td>";
        searchResultHtml += "<td>" + eventModule.camelCaseToSentenceCase(eventType); + "</td>";
        searchResultHtml += "<td>" + eventStart + "</td>";
        searchResultHtml += "<td class='text-right'>" + shipType + "</td>";
        searchResultHtml += "<td class='text-right'>" + shipLength + "</td>";
        searchResultHtml += "<td>" + shipName + "</td>";
        searchResultHtml += "<td class='glyphicon-1'><span id='result-show-" + event.id + "' class='glyphicon glyphicon-globe' data-toggle='tooltip' title='Show event on map'></span></td>";
        searchResultHtml += "</tr>";

        $('#event-search-modal .search-results .search-data tbody').append(searchResultHtml);

        $("#event-search-modal .search-results #result-show-" + event.id).on("click", function() {
            eventModule.visualizeEvent(event);
            $('#event-search-modal').modal('hide');
        });

        $("#event-search-modal .search-results #result-remove-" + event.id).on("click", function() {
            eventModule.suppressEvent(event);
        });

        eventModule.searchResults.push(event);
    },

    findEventByCriteria: function () {
        eventModule.clearSearchResults();
        eventModule.setSearchStarted("Searching...");

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

        var eventRequest = eventModule.eventResourceService + "?" + $.param(queryParams);

        $.getJSON(eventRequest).done(function (events) {
            $.each(events, function (idx, event) {
                eventModule.addSearchResult(event);
            });
            $("table#event-search-results").tablesorter(
                {
                    theme : 'bootstrap',
                    headerTemplate: '{content} {icon}',
                    widgets: ['zebra','columns', 'uitheme'],
                    headers: {
                        '.no-sort' : {
                            sorter: false
                        }
                    }
                }
            );
            eventModule.setSearchCompleted("Found " + events.length + " matching events.");
        }).fail(function (jqXHR, textStatus) {
            eventModule.setSearchCompleted("Search error: " + textStatus);
        });
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
                eventModule.setSearchCompleted("Search error: " + textStatus);
            });
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
            console.error("Error: " + textStatus);
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
            eventModule.setSearchCompleted("Search error: " + textStatus);
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
