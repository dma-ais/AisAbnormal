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

        var eventStart = '';
        if (searchResult.startTime > 0) {
            var time = new Date(0);
            time.setUTCSeconds(searchResult.startTime / 1000);
            eventStart = eventModule.formatDate(time);
        }

        var eventEnd = '';
        if (searchResult.endTime > 0) {
            var time = new Date(0);
            time.setUTCSeconds(searchResult.endTime / 1000);
            eventEnd = eventModule.formatDate(time);
        }

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

    computeEventExtent: function(event) {
        var e=-180, n=-90, w=180, s=90;

        var positions = event.behaviour.positions;
            $.each(positions, function (idx, position) {
                if (position.longitude > e) {
                    e = position.longitude;
                }
                if (position.longitude < w) {
                    w = position.longitude;
                }
                if (position.latitude < s) {
                    s = position.latitude;
                }
                if (position.latitude > n) {
                    n = position.latitude;
                }
            });

        return [w, n, e, s];
    },

    visualizeEvent: function(event) {
        var extent = eventModule.computeEventExtent(event);
        mapModule.zoomTo(extent[0],extent[1],extent[2],extent[3]);
        vesselModule.addBehavior(event.behaviour);
    }
};
