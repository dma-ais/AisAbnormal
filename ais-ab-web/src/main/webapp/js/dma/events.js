/**
 * This javascript module handles loading, processing, display and user interaction with event data.
 */

var eventModule = {
    init: function () {
        $('#event-search-modal-wrapper').load("event-search-modal.html", function () {
            $('.tabs#event-search-tabs').tabs();
            $('#event-search-by-id').click(eventModule.findEventById);
            $('#event-search-by-other').click(eventModule.findEventByCriteria);
        });
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

    setSearchStatus: function(statusText) {
        $("#event-search-modal .search-results .search-status").empty();
        $("#event-search-modal .search-results .search-status").append(statusText);
    },

    clearSearchResults: function () {
        eventModule.setSearchStatus("");

        var searchResults = $('#event-search-modal .search-results .search-data');

        searchResults.empty();

        var tableHtml  = "<table class='table search-results'>"
        tableHtml += "<thead><tr>";
        tableHtml += "<td>Action</td><td>Id</td><td>State</td><td>Start</td><td>End</td><td>Vessel</td>";
        tableHtml += "</tr></thead><tbody></tbody>";
        tableHtml += "</table>";

        searchResults.append(tableHtml);
    },

    addSearchResult: function(searchResult) {
        var searchResults = $('#event-search-modal table.search-results tbody');

        var eventStart = eventModule.formatTimestamp(searchResult.startTime);
        var eventEnd = eventModule.formatTimestamp(searchResult.endTime);

        var searchResultHtml  = "<tr>";
        searchResultHtml += "<td><span id='result-" + searchResult.id + "' class='glyphicon glyphicon-film'></span></td>";
        searchResultHtml += "<td>" + searchResult.id + "</td>";
        searchResultHtml += "<td>" + searchResult.state + "</td>";
        searchResultHtml += "<td>" + eventStart + "</td>";
        searchResultHtml += "<td>" + eventEnd + "</td>";
        searchResultHtml += "<td>" + searchResult.behaviour.vessel.id.name + "</td>";
        searchResultHtml += "</tr>";

        searchResults.append(searchResultHtml);

        $("#event-search-modal .search-results #result-" + searchResult.id).on("click", function() {
            eventModule.visualizeEvent(searchResult);
            $('#event-search-modal').modal('hide');
        });
    },

    findEventByCriteria: function () {
        eventModule.clearSearchResults();
        eventModule.setSearchStatus("Searching...");

        var eventResourceService = "/abnormal/rest/event";
        var queryParams = {
            from: $('input#search-event-from').val(),
            to: $('input#search-event-to').val(),
            type: $('input#search-event-type').val(),
            vessel: '%' + $('input#search-event-vessel').val().replace("*","%") + '%'
        };
        var eventRequest = eventResourceService + "?" + $.param(queryParams);

        $.getJSON(eventRequest).done(function (events) {
            eventModule.setSearchStatus("Found " + events.length + " matching events.");
            $.each(events, function (idx, event) {
                eventModule.addSearchResult(event);
            });
        }).fail(function (jqXHR, textStatus) {
            eventModule.setSearchStatus("Search error: " + textStatus);
        });
    },

    findEventById: function () {
        eventModule.clearSearchResults();
        eventModule.setSearchStatus("Searching...");

        var eventId = $('input#search-event-id').val();
        if (eventId) {
            //http://localhost:8080/abnormal/rest/event/1
            var eventResourceService = "/abnormal/rest/event";
            var eventResource = eventResourceService + "/" + eventId;
            $.getJSON(eventResource).done(function (event) {
                eventModule.setSearchStatus("Found " + (event ? "":"no ") + "matching event.");
                eventModule.addSearchResult(event);
            }).fail(function (jqXHR, textStatus) {
                eventModule.setSearchStatus("Search error: " + textStatus);
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

    visualizeEvent: function(event) {
        vesselModule.addEvent(event);
    }
};
