/*!
 * jQuery ticker - v0.1 - 7/9/2011
 *
 * Version: 0.1, Last updated: 7/9/2011
 * Requires: jQuery v1.3.2+
 *
 * Copyright (c) 2011 Radek Pleskac www.radekpleskac.com
 * Dual licensed under the MIT and GPL licenses.
 * http://www.opensource.org/licenses/mit-license.php
 * http://www.gnu.org/licenses/gpl.html
 *
 * Examples http://www.radekpleskac.com.com/projects/jquery-ticker/
 * jQuery plugin to turn an unordered list <ul> into a simple ticker, displaying one list item at a time.
 *
*/

(function($){

	$.fn.ticker = function(options) {

		$.fn.ticker.defaults =  {
			controls: false, //show controls, to be implemented
			interval: 3000, //interval to show next item
			effect: "fadeIn", // available effects: fadeIn, slideUp, slideDown
			duration: 400 //duration of the change to the next item
		};

		var o = $.extend({}, $.fn.ticker.defaults, options);

		if (!this.length)
			return;

		return this.each(function() {

			var $ul = $(this),
				$items = $ul.find("li"),
				index = 0,
				paused = false,
				time;

			function start() {
				time = setInterval(function() {
					if (!paused)
						changeItem();
				}, o.interval);
			}

			function changeItem() {

				var $current = $items.eq(index);
				index++;
				if (index == $items.length)
					index = 0;
				var $next =  $items.eq(index);

				if (o.effect == "fadeIn") {
					$current.fadeOut(function() {
						$next.fadeIn();
					});
				}
				else if (o.effect == "slideUp" || o.effect == "slideDown") {
					var h = $ul.height();
					var d = (o.effect == "slideUp") ? 1 : -1;
					$current.animate({
						top: -h * d + "px"
					}, o.duration, function() { $(this).hide(); });
					$next.css({
						"display": "block",
						"top": h * d + "px"
					});
					$next.animate({
						top: 0
					}, o.duration);
				}

			}

			function bindEvents() {
				$ul.hover(function() {
					paused = true;
				},function() {
					paused = false;
				});
			}

			function init() {
				$items.not(":first").hide();
				if (o.effect == "slideUp" || o.effect == "slideDown") {
					$ul.css("position", "relative");
					$items.css("position", "absolute");
				}

				bindEvents();
				start();
			}

			init();

		});

	};

})(jQuery);