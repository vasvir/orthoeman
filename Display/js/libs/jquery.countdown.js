/**
 * @name        jQuery Countdown Plugin
 * @author        Martin Angelov
 * @version     1.0
 * @url            http://tutorialzine.com/2011/12/countdown-jquery/
 * @license        MIT License
 */

(function ($) {

    // Number of seconds in every time division
    var days = 24 * 60 * 60,
        hours = 60 * 60,
        minutes = 60;

    function pad(a, b) {
        return(1e15 + a + "").slice(-b)
    }

    var timeoutIDTick, timeoutIDTack;
    var smallTimeSpan = $("#smalltime");

    var methods = {
        init: function (prop) {

            var options = $.extend({
                callback: function () {
                },
                timestamp: 0
            }, prop);

            var left, d, h, m, s, positions;

            var update = function (newValue) {
                options.timestamp = newValue;
            }

            // Initialize the plugin
            innerInit(this, options);

            positions = this.find('.position');


            (function tick() {

                // Time left
                left = Math.floor((options.timestamp - (new Date())) / 1000);

                if (left < 0) {
                    left = 0;
                }

                // Number of days left
                d = Math.floor(left / days);
                updateDuo(0, 1, d);
                left -= d * days;

                // Number of hours left
                h = Math.floor(left / hours);
                updateDuo(2, 3, h);
                left -= h * hours;

                // Number of minutes left
                m = Math.floor(left / minutes);
                updateDuo(4, 5, m);
                left -= m * minutes;

                // Number of seconds left
                s = left;
                updateDuo(6, 7, s);

                // Calling an optional user supplied callback
                options.callback(d, h, m, s);

                // Scheduling another call of this function in 1s
                timeoutIDTick = setTimeout(tick, 1000);
            })();

            (function tack() {
                var left_str = pad(d, 1) + "d " + pad(h, 2) + "h " + pad(m, 2) + "m";
                smallTimeSpan.html(left_str);
                timeoutIDTack = setTimeout(tack, s*1000);
            })();


            // This function updates two digit positions at once
            function updateDuo(minor, major, value) {
                switchDigit(positions.eq(minor), Math.floor(value / 10) % 10);
                switchDigit(positions.eq(major), value % 10);
            }

            return this;
        },
        destroy: function () {
            clearTimeout(timeoutIDTack);
            clearTimeout(timeoutIDTick);
            this.empty();
            smallTimeSpan.empty();
            return this;
        },
        stop: function () {
            clearTimeout(timeoutIDTack);
            clearTimeout(timeoutIDTick);
        }
    };


    // Creating the plugin
    $.fn.countdown = function (method) {

        if (methods[method]) {
            return methods[method].apply(this, Array.prototype.slice.call(arguments, 1));
        } else if (typeof method === 'object' || !method) {
            return methods.init.apply(this, arguments);
        } else {
            $.error('Method ' + method + ' does not exist on jQuery.tooltip');
        }

    };


    function innerInit(elem, options) {
        elem.addClass('countdownHolder');

        // Creating the markup inside the container
        $.each(['Days', 'Hours', 'Minutes', 'Seconds'], function (i) {
            var boxName;
            if (this == "Days") {
                boxName = "DAYS";
            }
            else if (this == "Hours") {
                boxName = "HRS";
            }
            else if (this == "Minutes") {
                boxName = "MNTS";
            }
            else {
                boxName = "SECS";
            }
            $('<div class="count' + this + '">' +
                '<span class="position">' +
                '<span class="digit static">0</span>' +
                '</span>' +
                '<span class="position">' +
                '<span class="digit static">0</span>' +
                '</span>' +
                '<span class="boxName">' +
                '<span class="' + this + '">' + boxName + '</span>' +
                '</span>'
            ).appendTo(elem);

            if (this != "Seconds") {
                elem.append('<span class="points">:</span><span class="countDiv countDiv' + i + '"></span>');
            }
        });

    }

    // Creates an animated transition between the two numbers
    function switchDigit(position, number) {

        var digit = position.find('.digit')

        if (digit.is(':animated')) {
            return false;
        }

        if (position.data('digit') == number) {
            // We are already showing this number
            return false;
        }

        position.data('digit', number);

        var replacement = $('<span>', {
            'class': 'digit',
            css: {
                top: 0,
                opacity: 0
            },
            html: number
        });

        // The .static class is added when the animation
        // completes. This makes it run smoother.

        digit
            .before(replacement)
            .removeClass('static')
            .animate({top: 0, opacity: 0}, 'fast', function () {
                digit.remove();
            })

        replacement
            .delay(100)
            .animate({top: 0, opacity: 1}, 'fast', function () {
                replacement.addClass('static');
            });
    }
})(jQuery);
