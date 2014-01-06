/**
 * This javascript module handles loading, processing, display and user interaction with event data.
 */

var eventModule = {
    init: function () {
        $('#event-search-modal-wrapper').load("event-search-modal.html", function () {
            $('#event-search-by-id').click(eventModule.findEventById);
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

    findEventById: function () {
        var eventId = $('input#search-event-id').val();
        if (eventId) {
            //http://localhost:8080/abnormal/rest/event/1
            var eventResourceService = "/abnormal/rest/event";
            var eventResource = eventResourceService + "/" + eventId;
            $.getJSON(eventResource).done(function (event) {
                var eventStart = new Date(0);
                eventStart.setUTCSeconds(event.startTime / 1000);

                var eventEnd = new Date(0);
                eventEnd.setUTCSeconds(event.endTime / 1000);

                var tableHtml  = "<table class='table search-results'>"
                    tableHtml += "<thead><tr>";
                    tableHtml += "<td>Action</td><td>Id</td><td>State</td><td>Start</td><td>End</td><td>Vessel</td>";
                    tableHtml += "</tr></thead>";
                    tableHtml += "<tbody><tr>";
                    tableHtml += "<td><span id='result-0' class='glyphicon glyphicon-film'></span></td>";
                    tableHtml += "<td>" + event.id + "</td>";
                    tableHtml += "<td>" + event.state + "</td>";
                    tableHtml += "<td>" + eventModule.formatDate(eventStart) + "</td>";
                    tableHtml += "<td>" + eventModule.formatDate(eventEnd) + "</td>";
                    tableHtml += "<td>" + event.behaviour.vessel.id.name + "</td>";
                    tableHtml += "</tr></tbody>";
                    tableHtml += "</table>";

                var events = $('div#event-search-modal div.search-results');
                events.empty();
                events.append(tableHtml);
                $("#event-search-modal .search-results #result-0").on("click", function() {
                    eventModule.visualizeEvent(event);
                    $('#event-search-modal').modal('hide');
                });

            }).fail(function (jqXHR, textStatus) {
                console.log(textStatus);
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
    }
};
