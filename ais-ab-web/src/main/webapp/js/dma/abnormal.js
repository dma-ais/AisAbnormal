// Replace the normal jQuery getScript function with one that supports
// debugging and which references the script files as external resources
// rather than inline.

/**
 * This is the main Javascript DMA Abnormal Behaviour application
 */

var dmaAbnormalApp = {

    init: function () {
        mapModule.init();
        eventModule.init();
        featureModule.init();
        vesselModule.init();
    }

};
