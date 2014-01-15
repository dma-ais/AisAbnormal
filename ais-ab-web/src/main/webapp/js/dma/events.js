/**
 * This javascript module handles loading, processing, display and user interaction with event data.
 */

var eventModule = {

    searchResults: new Array(),

    init: function () {
        $('#event-search-modal-wrapper').load("event-search-modal.html", function () {
            $('.tabs#event-search-tabs').tabs();
            $('#event-search-by-id').click(eventModule.findEventById);
            $('#event-search-by-other').click(eventModule.findEventByCriteria);

            $("#event-search-modal .search-results .search-show-all").hide();
            $("#event-search-modal .search-results .search-show-all").click(function() {
                eventModule.visualizeAllSearchResults();
                $('#event-search-modal').modal('hide');
            });
        });

        $("#events-remove").click(function() {
            eventModule.removeAllEvents();
        });
    },

    removeAllEvents: function() {
        mapModule.getVesselLayer().removeAllFeatures();
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

        var tableHtml  = "<table class='table'>"
        tableHtml += "<thead><tr>";
        tableHtml += "<td>Action</td><td>Id</td><td>State</td><td>Start</td><td>End</td><td>Vessel</td>";
        tableHtml += "</tr></thead><tbody></tbody>";
        tableHtml += "</table>";

        searchResults.append(tableHtml);

        eventModule.searchResults = new Array();
    },

    addSearchResult: function(event) {

        var eventStart = eventModule.formatTimestamp(event.startTime);
        var eventEnd = eventModule.formatTimestamp(event.endTime);

        var searchResultHtml  = "<tr>";
        searchResultHtml += "<td><span id='result-" + event.id + "' class='glyphicon glyphicon-film'></span></td>";
        searchResultHtml += "<td>" + event.id + "</td>";
        searchResultHtml += "<td>" + event.state + "</td>";
        searchResultHtml += "<td>" + eventStart + "</td>";
        searchResultHtml += "<td>" + eventEnd + "</td>";
        searchResultHtml += "<td>" + event.behaviour.vessel.name + "</td>";
        searchResultHtml += "</tr>";

        $('#event-search-modal .search-results .search-data tbody').append(searchResultHtml);

        $("#event-search-modal .search-results #result-" + event.id).on("click", function() {
            eventModule.visualizeEvent(event);
            $('#event-search-modal').modal('hide');
        });

        eventModule.searchResults.push(event);
    },

    findEventByCriteria: function () {
        eventModule.clearSearchResults();
        eventModule.setSearchStarted("Searching...");

        var eventResourceService = "/abnormal/rest/event";
        var queryParams = {
            from: $('input#search-event-from').val(),
            to: $('input#search-event-to').val(),
            type: $('input#search-event-type').val(),
            vessel: '%' + $('input#search-event-vessel').val().replace("*","%") + '%'
        };
        if ($('input#search-event-inarea').is(':checked')) {
            var bounds = mapModule.getCurrentViewportExtent();
            queryParams['north'] = bounds.top;
            queryParams['east'] = bounds.right;
            queryParams['south'] = bounds.bottom;
            queryParams['west'] = bounds.left;
        }

        var eventRequest = eventResourceService + "?" + $.param(queryParams);

        $.getJSON(eventRequest).done(function (events) {
            $.each(events, function (idx, event) {
                eventModule.addSearchResult(event);
            });
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
                eventModule.setSearchCompleted("Found " + (event ? "":"no ") + "matching event.");
                eventModule.addSearchResult(event);
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

        var trackingPoints = event.behaviour.trackingPoints;
        $.each(trackingPoints, function (idx, trackingPoint) {
            var point = new OpenLayers.LonLat(trackingPoint.longitude, trackingPoint.latitude);
            bounds.extend(point);
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
    }
};
