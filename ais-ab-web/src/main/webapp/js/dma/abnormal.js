// Replace the normal jQuery getScript function with one that supports
// debugging and which references the script files as external resources
// rather than inline.
jQuery.extend({
    getScript: function(url, callback) {
        var head = document.getElementsByTagName("head")[0];
        var script = document.createElement("script");
        script.src = url;

        // Handle Script loading
        {
            var done = false;

            // Attach handlers for all browsers
            script.onload = script.onreadystatechange = function(){
                if ( !done && (!this.readyState ||
                    this.readyState == "loaded" || this.readyState == "complete") ) {
                    done = true;
                    if (callback)
                        callback();

                    // Handle memory leak in IE
                    script.onload = script.onreadystatechange = null;
                }
            };
        }

        head.appendChild(script);

        // We handle everything using the script element injection
        return undefined;
    }
});

/**
 * This is the main Javascript DMA Abnormal Behaviour application
 */

var dmaAbnormalApp = {

    init: function () {
        $.getScript("js/OpenLayers.js", function (data, textStatus, jqxhr) {
            $.getScript("js/dma/map.js", function (data, textStatus, jqxhr) {
                $.getScript("js/dma/events.js", function (data, textStatus, jqxhr) {
                    $.getScript("js/dma/features.js", function (data, textStatus, jqxhr) {
                        console.log("All modules of DMA abnormal behaviour webapp loaded.");
                        mapModule.init();
                        eventModule.init();
                        featureModule.init();
                    })
                })
            })
        })
    }

};
