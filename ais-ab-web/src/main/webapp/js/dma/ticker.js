var tickerModule = {

    init: function() {
        tickerModule.reloadTickerData();
    },

    reloadTickerData: function() {
        $.get('/abnormal/rest/event?numberOfReventEvents=5', function(events) {

            $("#ticker").empty();

            $.each(events, function(i, event) {
                $("#ticker").append(
                    '<li style="display: list-item;">' +
                    tickerModule.eventToString(event) + ' ' +
                    '<span class="glyphicon glyphicon-arrow-up" id="ticker-event-' + event.id + '" data-event-id="' + event.id + '"></span>' +
                    '</li>');

                $("#ticker li > span.glyphicon#ticker-event-" + event.id).click(function(event) {
                    eventModule.visualizeEventId(event.target.dataset.eventId);
                });
            });

            $("#ticker").ticker({
                controls: false, //show controls, to be implemented
                interval: 3000, //interval to show next item
                effect: "fadeIn", // available effects: fadeIn, slideUp, slideDown
                duration: 400 //duration of the change to the next item
            });

            setTimeout(tickerModule.reloadTickerData, 60000);
        });
    },

    eventToString: function(event) {
        var s = "";
        s += eventModule.formatTimestamp(event.startTime) + " ";

        if (event.eventType == "AbnormalShipSizeOrTypeEvent") {
            var bounds = eventModule.computeEventExtent(event);

            s += (event.state == 'ONGOING' ? "Ongoing " : "Past ");
            s += "abnormal presence of " + event.behaviour.vessel.id.name + " ";
            s += "(" + event.behaviour.vessel.id.callsign + ") ";
            s += " near ";
            s += "[";
            s += OpenLayers.Util.getFormattedLonLat(bounds.getCenterLonLat().lat, 'lat', 'dms')
            s += ", ";
            s += OpenLayers.Util.getFormattedLonLat(bounds.getCenterLonLat().lon, 'lon', 'dms')
            s += "] ";
        } else {
            s += "?";
        }


        return s;
    }


}
