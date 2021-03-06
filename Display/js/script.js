﻿/* Author: Konstantinos Zagoris
 The script logic for ORTHO e-Man


 TODO cruise mode
 TODO make quiz and rangequiz obey the show results/do not show the
 TODO Doc: screencast for Display Tool usage.

 */


var OrthoVariables = {
    maxPages: 5,
    CurPage: 1,
    HeightFromBottom: 120, //$('#navigation').height() - $('footer').height();
    origCanvas: [], // is array with [0] original image, [1] imageurl, [2] stage [3] brightness [4] contrast, [5] is invert
    // scale:1, //official scale
    scalePage: [], //scale for each page.
    zoomPage: [], //zoom for each page
    JsonUrl: "sslayer.php",
    LessonData: "",
    buttonState: [],
    isLessonDisable: false,
    isLessonComplete: false,
    line: {
        "pressed": false,
        startx: -1,
        starty: -1,
        "prevline": null,
        "previnfotip": null,
        id: 0
    },
    msg_info: [],
    msg_curIndex: 0,
    linemindistance: 10,
    clickcatch: false,
    lessonAnswers: [],
    lessonPage: -1,
    InitialQueryString: [],
    lessonLoaded: [],
    MaxHotSpots: [],
    spinControls: [],
    PageTracking: [],
    ColorRight: "#047816",
    ColorRightEdge: "#285935",
    ColorWrong: "#9C2100",
    ColorWrongEdge: "#592835",
    ColorInfo:"#041678",
    ColorInfoEdge: "#283559",
    zoomMouse: { isdown: false, x: -1, y: -1},
    disableturn: true,
    isOverAngleCircle: false,
    loadedPreviousAnswer: [],
    finalGrade: 0,
    queue: { imgLoaded: [], funcQueue: [] }
};


$(document).ready(function () {
    //_V_.options.flash.swf = "libs/video-js/video-js.swf";
    $.pnotify.defaults.styling = "jqueryui";
    $.pnotify.defaults.history = false;
    $("#NextTest").data("fire", true);
    $.prettyLoader({
        animation_speed: 'normal',
        bind_to_ajax: true,
        delay: false,
        loader: 'img/ajax-loader.gif'
    });
    document.onselectstart = function () {
        return false;
    };
    $("#closeResults").click(function () {
        $("#overlay").fadeOut('slow', function () {
            $(this).removeClass("waiting").addClass("overlay_hidden");
        });
        $("#shadow_pageresults2").fadeOut('slow');
        $("#pageresults2").fadeOut('slow');
    });
    OrthoVariables.InitialQueryString = getUrlVars();
    OrthoVariables.disableturn = OrthoVariables.InitialQueryString["DisablePaging"] || OrthoVariables.disableturn;
    //console.log("id:" + OrthoVariables.InitialQueryString["id"]);
    $.getJSON(OrthoVariables.JsonUrl, {
        "action": 4, "orthoeman_id": OrthoVariables.InitialQueryString.id, "name": OrthoVariables.InitialQueryString.name
    }, function (data) {
        var count = parseInt(data.countAnswers);
        if (count > 0) {
            initializeOrthoeMAN();
        }
        else {
            var timeLeft = data.timeout;
            var getTimeLeftString = function (left) {
                var days = 24 * 60 * 60,
                    hours = 60 * 60,
                    minutes = 60;
                var pad  = function(a, b) {
                    return(1e15 + a + "").slice(-b)
                };
                if (left < 0) {
                    left = 0;
                }

                // Number of days left
                var d = Math.floor(left / days);
                left -= d * days;

                // Number of hours left
                var h = Math.floor(left / hours);
                left -= h * hours;

                // Number of minutes left
                var m = Math.floor(left / minutes);
                left -= m * minutes;

                return d + "d " + pad(h, 2) + "h " + pad(m, 2) + "m";
            };

            $("#dialogRemainingTime").html(getTimeLeftString(timeLeft));

            $("#dialog").dialog({
                resizable: true,
                height: 235,
                width: 400,
                modal: true,
                buttons: {
                    "Start Lesson": function () {
                        $(this).dialog("close");
                        initializeOrthoeMAN();
                    },
                    "Cancel": function () {
                        $(this).dialog("close");
                    }
                }
            });
        }
    });
});


function initializeOrthoeMAN() {
    $.getJSON(OrthoVariables.JsonUrl, {
        "action": 1, "orthoeman_id": OrthoVariables.InitialQueryString.id, "name": OrthoVariables.InitialQueryString.name
    }, function (data) {
        OrthoVariables.LessonData = data;
        OrthoVariables.maxPages = 2 * (OrthoVariables.LessonData.Page.length + 1);
        $("#lesson").html($("#LessonTemplate").render(OrthoVariables.LessonData));
        //initialize the buttonstates
        for (var i = 0; i < OrthoVariables.LessonData.Page.length; i++) {
            OrthoVariables.buttonState[i] = {
                "b": false,
                "c": false,
                "l": false,
                "t": false,
                "h": false
            };
            OrthoVariables.scalePage[i] = 1;
            OrthoVariables.zoomPage[i] = 1;
            OrthoVariables.PageTracking[i] = {
                status: "pending",
                grade: parseInt(OrthoVariables.LessonData.Page[i].attributes.Grade),
                nextpass: false,
                submitbutton: false,
                theory: false
            };
            OrthoVariables.lessonAnswers[i] = {
                quiz: [],
                hotspots: []
            };
            // enable the function queues
            OrthoVariables.queue.imgLoaded[i] = false;
            OrthoVariables.queue.funcQueue[i] = new Queue();
        }
        displayFunctions();
        LoadImages("0");
        loadSpinControl("0");
        if (OrthoVariables.LessonData.Page[0] != undefined) {
            if (!(OrthoVariables.LessonData.Page[0].Widget[0].type === "image" || OrthoVariables.LessonData.Page[0].Widget[1].type === "image")) {
                loadPreviousAnswers(0);
            }
        }
        //setTracking("0");


        ApplyRoundtoPages(1, 3);
        //DisableButtonLink("SubmitAnswer");

        EnableButtonLink("NextTest");
        // Check for theory  video/text or image/text and text/text
        for (var i = 0; i < OrthoVariables.LessonData.Page.length; i++) {
            if (OrthoVariables.LessonData.Page[i].Widget[0].type === "video" || OrthoVariables.LessonData.Page[i].Widget[1].type === "video") {
                if (OrthoVariables.LessonData.Page[i].Widget[0].type === "text" || OrthoVariables.LessonData.Page[i].Widget[1].type === "text") {
                    OrthoVariables.PageTracking[i].nextpass = true;
                    OrthoVariables.PageTracking[i].theory = true;
                }

            }
            else if (OrthoVariables.LessonData.Page[i].Widget[0].type === "image" || OrthoVariables.LessonData.Page[i].Widget[1].type === "image") {
                if (OrthoVariables.LessonData.Page[i].Widget[0].type === "text" || OrthoVariables.LessonData.Page[i].Widget[1].type === "text") {
                    var hasHotSpots = true;
                    $.each(OrthoVariables.LessonData.Images, function () {
                        if (this.id === i) {
                            if (this.MaxSpots === 0) {
                                hasHotSpots = false;
                            }
                        }

                    });

                    if (!hasHotSpots) {
                        OrthoVariables.PageTracking[i].nextpass = true;
                        OrthoVariables.PageTracking[i].theory = true;
                    }
                }
            }
            else if (OrthoVariables.LessonData.Page[i].Widget[0].type === "text" && OrthoVariables.LessonData.Page[i].Widget[1].type === "text") {
                OrthoVariables.PageTracking[i].nextpass = true;
                OrthoVariables.PageTracking[i].theory = true;
            }
        }

        $('#counter').countdown('init', {
            callback: function (d, h, m, s) {
                checkCountDown(d, h, m, s);
            },
            timestamp: (new Date()).getTime() + OrthoVariables.LessonData.Timeout * 1000
        }).hide();
        $('#counter_small').click(function () {
            $(this).hide();
            $('#counter').fadeIn("slow").delay(3000).fadeOut("slow", function () {
                $("#counter_small").show();
            });
        });
        checkIsFinished();
        $("#curPage").html(parseInt(OrthoVariables.CurPage / 2));
        $("#totalPage").html(parseInt(OrthoVariables.maxPages / 2));

        if (OrthoVariables.LessonData["final"] === false) {
            var maxpage = -1;
            $.each(OrthoVariables.LessonData.Tracking, function (key, value) {
                if (key > maxpage) maxpage = parseInt(key);
            });

            if (maxpage > -1) {
                maxpage = 2 * (maxpage + 1);
                for (i = 2; i <= maxpage; i += 2) {
                    pageIsTurned(i);
                }
                $("#lesson").turn('page', maxpage);
            }

        }
        $('#counter_small').hide();
        $('#counter').stop(true).fadeIn("slow").delay(5000).fadeOut(1000, function () {
            $("#counter_small").show();
        });


    });
}


function checkIsFinished() {
    if (OrthoVariables.LessonData["final"] === true) {
        lessonFinalized();
    }
}

function disableLesson() {
    if (OrthoVariables.isLessonComplete === false) {
        $("#overlay").removeClass("overlay_hidden").addClass("waiting");
        $("#overlay_msg").css('display', 'block');
        $("#shadow_overlay_msg").css('display', 'block');
        OrthoVariables.isLessonDisable = true;
    }
}

function normalizeGrades() {
    var finalGrade = 0;
    for (var i = 0; i < OrthoVariables.PageTracking.length; i++) {
        if (OrthoVariables.PageTracking[i].theory === false) {

            if (OrthoVariables.LessonData.Tracking[i] !== undefined) {
                var answer = JSON.parse(OrthoVariables.LessonData.Tracking[i].answer);
                OrthoVariables.PageTracking[i].status = answer.myanswer.Answer;
                OrthoVariables.PageTracking[i].grade = parseFloat(answer.grade);
            }
            finalGrade += OrthoVariables.PageTracking[i].grade;
        }
    }
    OrthoVariables.finalGrade = (finalGrade < 0) ? 0 : Math.round(finalGrade);
}

function lessonFinalized(showPage) {
    showPage = !!(typeof showPage === "undefined");
    OrthoVariables.isLessonComplete = true;

    normalizeGrades();
    $('#counter').countdown('stop');
    if (showPage) {
        $("#pageresults2>div").html($("#EndPageTemplate").render(OrthoVariables));
        $("#overlay").removeClass("overlay_hidden").addClass("waiting");
        $("#shadow_pageresults2").fadeIn('slow');
        //$("#pageresults2").fadeIn('slow');
    }
}

function checkCountDown(d, h, m, s) {
    var secsLeft = d * 86400 + h * 3600 + m * 60 + s;
    if (secsLeft <= 0) {
        var question = {
            action: 3,
            orthoeman_id: OrthoVariables.InitialQueryString.id
        };
        $.ajax({
            url: OrthoVariables.JsonUrl,
            data: question
        }).done(function (data) {
                if (parseInt(data) <= 0) {
                    $('#counter').countdown('destroy');
                    disableLesson();
                }
            });
    }
}

function updateCounter() {

    var question = {
        action: 3,
        orthoeman_id: OrthoVariables.InitialQueryString.id
    };
    $.ajax({
        url: OrthoVariables.JsonUrl,
        data: question
    }).done(function (data) {
            //console.log(data);
            $('#counter').countdown('destroy');
            $('#counter').countdown('init', {
                callback: function (d, h, m, s) {
                    checkCountDown(d, h, m, s);
                },
                timestamp: (new Date()).getTime() + data * 1000
            }).hide();
        });
}


function getUrlVars() {
    "use strict";
    var vars = [], hash;
    var hashes = window.location.href.replace("#", "").slice(window.location.href.indexOf('?') + 1).split('&');
    for (var i = 0; i < hashes.length; i++) {
        hash = hashes[i].split('=');
        if (hash[0] === "name" || hash[0] === "id" || hash[0] === "DisablePaging") {
            vars.push(hash[0]);
            vars[hash[0]] = hash[1];
        }
    }
    return vars;
}

function LoadVideo(Page) {
    for (var wid in OrthoVariables.LessonData.Page[Page].Widget) {
        if (OrthoVariables.LessonData.Page[Page].Widget[wid].type === "video") {
            /*$("#video_" + OrthoVariables.LessonData.Page[Page].Widget[wid].Video.id).mediaelementplayer({
                enableAutosize: true,
                pauseOtherPlayers: true,
                features: ['playpause', 'progress', 'duration', 'volume']
            });*/
            //OrthoVariables.lessonLoaded[parseInt(Page)];
            CheckResizeLimits(Page);
        }
    }
}

function loadSpinControl(Page) {
    if (Page < OrthoVariables.LessonData.Page.length && OrthoVariables.spinControls[Page] === undefined) {
        for (var wid in OrthoVariables.LessonData.Page[Page].Widget) {
            if (OrthoVariables.LessonData.Page[Page].Widget[wid].type === "input") {
                var wInput = OrthoVariables.LessonData.Page[Page].Widget[wid].Input;

                var spinCtrl = new SpinControl();
                spinCtrl.Tag = wInput.id;
                spinCtrl.AttachValueChangedListener(changedSpinControl);
                spinCtrl.GetAccelerationCollection().Add(new SpinControlAcceleration(1, 500));
                spinCtrl.GetAccelerationCollection().Add(new SpinControlAcceleration(5, 1750));
                spinCtrl.GetAccelerationCollection().Add(new SpinControlAcceleration(10, 3500));
                spinCtrl.SetMaxValue(wInput.Max);
                spinCtrl.SetMinValue(wInput.Min);
                spinCtrl.SetCurrentValue(0);
                $("#input_" + wInput.id).append(spinCtrl.GetContainer());
                spinCtrl.StartListening();
                $("#input_" + wInput.id + " input.spinInput").attr("onkeydown", "return valid_num(event);").attr("onkeyup", "changeValueSpincontrol(this)");
                OrthoVariables.spinControls[Page] = spinCtrl;
            }

        }
    }

}

function changeValueSpincontrol(obj) {
    OrthoVariables.spinControls[OrthoVariables.lessonPage].SetCurrentValue(parseFloat($(obj).val()));
}
function changedSpinControl(sender, newVal) {
    var id = sender.Tag;
    if (OrthoVariables.lessonPage === Number(id)) {
        EnableButtonLink("SubmitAnswer");
        OrthoVariables.lessonAnswers[OrthoVariables.lessonPage].input = {value: newVal};
    }

}

function loadPreviousAnswers(Page) {
    "use strict";
    if (!OrthoVariables.loadedPreviousAnswer[parseInt(Page)]) {
        if (OrthoVariables.LessonData.Tracking !== undefined) {
            if (OrthoVariables.LessonData.Tracking[Page] !== undefined) {
                var tracking = OrthoVariables.LessonData.Tracking[Page];
                if (tracking !== undefined) {
                    var data = JSON.parse(tracking.answer);
                    switch (tracking.type) {
                        case "0": // Input
                            loadSpinControl(Page);
                            applyInputUserResponse(data.userrespond, Page);
                            applyInputResult(data.myanswer, parseInt(Page), false);
                            break;
                        case "1": // Draw Hotspots
                            if (OrthoVariables.queue.imgLoaded[Page] === true) {
                                OrthoVariables.PageTracking[Page].status = 'pending';
                                applyHotspotUserResponse(data.userrespond, Page);
                                ApplyHotspotResult(data.myanswer, parseInt(Page), false);
                            }
                            else {
                                OrthoVariables.queue.funcQueue[Page].enqueue((function (xdata, yPage) {
                                    return function () {
                                        OrthoVariables.PageTracking[yPage].status = 'pending';
                                        applyHotspotUserResponse(xdata.userrespond, yPage);
                                        ApplyHotspotResult(xdata.myanswer, parseInt(yPage), false);
                                    }

                                })(data,Page));
                            }

                            break;
                        case "2": //quiz
                            applyQuizUserResponse(data.userrespond, parseInt(Page));
                            ApplyQuizResult(data.myanswer, parseInt(Page), false);
                            break;

                    }
                    OrthoVariables.loadedPreviousAnswer[parseInt(Page)] = true;

                }
            }

        }
    }

}

function loadLessonPage_old(Page) {
    "use strict";
    //console.log(Page,OrthoVariables.LessonData.Tracking[Page]);
    if (OrthoVariables.LessonData.Tracking !== undefined) {
        if (OrthoVariables.LessonData.Tracking[Page] !== undefined) {
            var tracking = OrthoVariables.LessonData.Tracking[Page];
            if (tracking !== undefined) {
                switch (tracking.Type) {
                    case 0: // Input
                        OrthoVariables.spinControls[OrthoVariables.lessonPage].SetCurrentValue(tracking);
                        OrthoVariables.spinControls[OrthoVariables.lessonPage].SetDisabled(true);
                        break;
                    case 1: // Draw Hotspots
                        $.each(tracking.Hotspots, function () {
                            drawCircleHotSpot(Page, this.x, this.y, "yes", Page, true);
                        });
                        break;
                    case 2: // quiz

                        break;
                }
                // Set the variables so to be answered
                var answer = (tracking.Results === true ) ? "correct" : "answer";
                PageTracking(answer, "no");
                CheckReadyNextText(answer, "no");

                if (tracking.Results === true) {
                    $("#Page" + id).css("background", "url('img/bg_correct.png')").css('text-shadow', 'none');
                }
                else //wrong answer
                {
                    $("#Page" + id).css("background", "url('img/bg_wrong.png')").css('text-shadow', 'none');
                }


            }
        }
    }

}


function addEvents(i, c, orig, imagesToLoad) {
    OrthoVariables.origCanvas[imagesToLoad[i].id] = [orig, imagesToLoad[i].url, undefined , 0, 0, false, false ];
    //OrthoVariables.origCanvas[imagesToLoad[i].id][6] = (imagesToLoad[i].EnableTracking === "yes");
    OrthoVariables.MaxHotSpots[imagesToLoad[i].id] = imagesToLoad[i].MaxSpots;
    //sliders
    $('#slider_b_' + imagesToLoad[i].id).slider({
        range: "max",
        min: -100,
        max: 100,
        value: 0,
        slide: function (event, ui) {
            var id = this.id.substr(this.id.lastIndexOf("_") + 1, this.id.length);
            var value = ui.value / 100;
            OrthoVariables.origCanvas[id][3] = value;
            ApplyImageOperations(id);
        }
    });
    $('#slider_c_' + imagesToLoad[i].id).slider({
        range: "max",
        min: -100,
        max: 100,
        value: 0,
        slide: function (event, ui) {
            var id = getID(this.id);
            var value = ui.value / 100;
            OrthoVariables.origCanvas[id][4] = value;
            ApplyImageOperations(id);
        }
    });
    //for shapes
    var stage = new Kinetic.Stage({
        container: "container_" + imagesToLoad[i].id,
        width: c.width,
        height: c.height,
        listen: true
    });

    var shapelayer = new Kinetic.Layer({
        id: "shapelayer",
        name: "shapelayer"
    });

    var infolayer = new Kinetic.Layer({
       id: "infolayer",
        name: "infolayer"
    });
    var answerlayer = new Kinetic.Layer({
        id: "answerlayer",
        name: "answerlayer"
    });

    var trackingLayer = new Kinetic.Layer({
        id: "trackinglayer",
        name: "trackinglayer"
    });


    var tooltiplayer = new Kinetic.Layer({
        id: "tooltiplayer",
        name: "tooltiplayer",
        throttle: 20



    });
    var tooltip = new Kinetic.Text({
        text: "",
        fill: "#FFCDC1",
        fontFamily: "MerriweatherRegular,Georgia",
        fontSize: 14,
        verticalAlign: "middle",
        lineHeight: 1,
        padding: 6,
        //fill:"black",
        visible: false,
        opacity: 1,
        id: "tooltip",
        name: "tooltip"

    });
    tooltiplayer.add(tooltip);
    stage.add(infolayer);
    stage.add(shapelayer);
    stage.add(trackingLayer);
    stage.add(tooltiplayer);
    stage.add(answerlayer);
    OrthoVariables.origCanvas[imagesToLoad[i].id][2] = stage;

    $("#pointer_" + imagesToLoad[i].id).mousedown(function (e) {
        if (!OrthoVariables.line.pressed && !OrthoVariables.buttonState[OrthoVariables.lessonPage]['h']) {
            $(this).addClass("movecursor");
            OrthoVariables.zoomMouse.isdown = true;
            OrthoVariables.zoomMouse.x = e.pageX;
            OrthoVariables.zoomMouse.y = e.pageY;
        }
    });

    $("#pointer_" + imagesToLoad[i].id).mousemove({id: imagesToLoad[i].id}, function (e) {
        if (OrthoVariables.zoomMouse.isdown && OrthoVariables.buttonState[OrthoVariables.lessonPage]['h'] === false) {
            var nx = e.pageX - OrthoVariables.zoomMouse.x;
            var ny = e.pageY - OrthoVariables.zoomMouse.y;
            OrthoVariables.zoomMouse.x = e.pageX;
            OrthoVariables.zoomMouse.y = e.pageY;
            var top = parseFloat($(this).css("top")) + ny;
            var left = parseFloat($(this).css("left")) + nx;
            //console.log($(this).parent().height(),  $(this).height());
            var mintop = $(this).parent().height() - $(this).height();
            var minleft = $(this).parent().width() - $(this).width();
            if (top < mintop) {
                top = mintop;
            }

            if (left < minleft) {
                left = minleft;
            }

            if (left > 0) {
                left = 0;
            }
            if (top > 0) {
                top = 0;
            }
            $(this).css("left", left).css("top", top);
            //console.log($(this).attr('id'), e.data.id);
            //OrthoVariables.origCanvas[e.data.id][2].draw();

        }
    });

    $("#pointer_" + imagesToLoad[i].id).mouseleave(function () {
        $(this).removeClass("movecursor");
        OrthoVariables.zoomMouse.isdown = false;
        OrthoVariables.zoomMouse.x = OrthoVariables.zoomMouse.y = -1;
    });

    $("#pointer_" + imagesToLoad[i].id).mouseup(function () {
        $(this).removeClass("movecursor");
        OrthoVariables.zoomMouse.isdown = false;
        OrthoVariables.zoomMouse.x = OrthoVariables.zoomMouse.y = -1;
    });
    $("#container_" + imagesToLoad[i].id).click({
        "pos": imagesToLoad[i].HotSpots
    }, function (event) {
        var id = getID(this.id);
        if (OrthoVariables.buttonState[id]['h'] === true) {
            var ishotspots = event.data.pos;
            var mystage = OrthoVariables.origCanvas[id][2];
            var mousepos = mystage.getMousePosition();
            if (mousepos !== undefined && mystage !== undefined) {
                drawCircleHotSpot(id, mousepos.x, mousepos.y, ishotspots, OrthoVariables.lessonPage, false);
            }
        }
    });
    $("#container_" + imagesToLoad[i].id).mousedown(function () {
        if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"]) {
            var id = getID(this.id);
            var mystage = OrthoVariables.origCanvas[id][2];
            var mousepos = mystage.getMousePosition();
            if (mousepos !== undefined && mystage !== undefined) {
                OrthoVariables.line.pressed = true;
                OrthoVariables.line.startx = mousepos.x / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]);
                OrthoVariables.line.starty = mousepos.y / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]);
            }
        }
    });

    $("#container_" + imagesToLoad[i].id).mousemove(function () {
        if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"] && OrthoVariables.line.pressed) {
            DrawShape(this.id);
        }
        var imgID = getID(this.id);
        if (OrthoVariables.buttonState[imgID]['t']) {
            DrawTrackingLines(imgID);
        }
    });

    $("#container_" + imagesToLoad[i].id).mouseup(function () {
        if (OrthoVariables.buttonState[OrthoVariables.lessonPage].l && OrthoVariables.line.pressed) {
            DrawShape(this.id);
            SetonLine(this.id);
            CheckShape(this.id);
            OrthoVariables.line.pressed = false;
            OrthoVariables.line.startx = -1;
            OrthoVariables.line.starty = -1;
            OrthoVariables.line.prevline = null;
            OrthoVariables.line.id++;
        }

    });

    $("#container_" + imagesToLoad[i].id).mouseout(function () {
        if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"] && OrthoVariables.line.pressed) {
            SetonLine(this.id);
            CheckShape(this.id);

            OrthoVariables.line.pressed = false;
            OrthoVariables.line.startx = -1;
            OrthoVariables.line.starty = -1;
            OrthoVariables.line.prevline = null;
            OrthoVariables.line.id++;
        }
        var imgID = getID(this.id);
        if (OrthoVariables.buttonState[imgID]['t']) {
            clearTrackingLines(imgID);
        }

    });
}

function LoadImages(Page) {
    // Loading the Image to Canvas
    "use strict";
    var imagesToLoad = [];
    var counter = 0;
    for (var i in OrthoVariables.LessonData.Images) {
        if ((OrthoVariables.LessonData.Images[i].id === Number(Page) || OrthoVariables.LessonData.Images[i].id === (Number(Page) + 1)) && OrthoVariables.lessonLoaded[parseInt(Page)] === undefined) {
            imagesToLoad[counter] = OrthoVariables.LessonData.Images[i];
            counter++;
        }
    }

    /*if (Page < OrthoVariables.LessonData.Page.length) {
     if (OrthoVariables.LessonData.Page[Page].Widget[0].type === "video" || OrthoVariables.LessonData.Page[Page].Widget[1].type === "video")
     { LoadVideo(Page); }
     } */
    if (Page < OrthoVariables.LessonData.Page.length && OrthoVariables.lessonLoaded[parseInt(Page)] === undefined) {
        LoadVideo(Page);
    }
    if ((Number(Page) + 1) < OrthoVariables.LessonData.Page.length && OrthoVariables.lessonLoaded[parseInt(Page)] === undefined) {
        LoadVideo(Number(Page) + 1);
    }


    for (i = 0; i < imagesToLoad.length; i++) {
        /*$("#modal_" + imagesToLoad[i].id).dialog({
         modal: false,
         autoOpen: false,
         title: "Zoom Functions",
         show: "fold",
         hide: "fold"
         });*/
        var imageToLoadvar = $('#canvasid_' + imagesToLoad[i].id);
        var c = imageToLoadvar.get(0);
        imageToLoadvar.css("background", "url(" + imagesToLoad[i].url + ")");
        c.getContext("2d").zag_LoadImage(imagesToLoad[i].url, {i: i, page: Page, c: c, imagesToLoad: imagesToLoad  }).done(function (data) {
            if (typeof G_vmlCanvasManager != 'undefined') {
                data.c = G_vmlCanvasManager.initElement(data.c);
            }
            var orig = document.createElement('canvas');
            orig.width = data.c.width;
            orig.height = data.c.height;
            orig.getContext("2d").drawImage(data.c, 0, 0);
            if (typeof G_vmlCanvasManager != 'undefined') {
                orig = G_vmlCanvasManager.initElement(orig);
            }
            var thePage = data.imagesToLoad[data.i].id;
            addEvents(data.i, data.c, orig, data.imagesToLoad);
            OrthoVariables.lessonLoaded[thePage] = true;
            CheckResizeLimits(thePage);
            loadPreviousAnswers(thePage);
            drawInfoShapes(thePage, data.imagesToLoad[data.i].InfoShapes);
            while (OrthoVariables.queue.funcQueue[thePage].isEmpty() === false) {
               setTimeout(OrthoVariables.queue.funcQueue[thePage].dequeue(), 500);
            }
            OrthoVariables.queue.imgLoaded[thePage] = true;

        });


        //


    }
    /*OrthoVariables.lessonLoaded[parseInt(Page)] = true;
     if (counter > 0) {
     CheckResizeLimits(Page);
     }*/

}

function drawInfoShapes(pageID, data) {
    var id = pageID.toString() //+ subid.toString();
    var mystage = OrthoVariables.origCanvas[id][2];
    var myshapelayer = mystage.get(".infolayer")[0];
    $.each(data, function(index, value) {
       switch (value[0]) {
           case "Circle":
               myshapelayer.add(PaintCircle(value, OrthoVariables.ColorInfo, OrthoVariables.ColorInfoEdge));
               break;
           case "Rect":
               myshapelayer.add(PaintRect(value, OrthoVariables.ColorInfo, OrthoVariables.ColorInfoEdge));
               break;
           case "Polygon":
               myshapelayer.add(PaintPolygon(value, OrthoVariables.ColorInfo, OrthoVariables.ColorInfoEdge));
               break;
           case "Eclipse" :
               myshapelayer.add(PaintEclipse(value, OrthoVariables.ColorInfo, OrthoVariables.ColorInfoEdge));
               break;
       }
    });
    //myshapelayer.draw();
    var childrens = myshapelayer.getChildren();
    for (var i = 0; i < childrens.length; i++) {
        var tween = new Kinetic.Tween({
            node: childrens[i],
            opacity: 0.5,
            duration: 2,
            easing: Kinetic.Easings.EaseOut
        });
        tween.play();
    }

    myshapelayer.draw();
}


function drawCircleHotSpot(id, x, y, ishotspots, lessonPage, isDraw) {
    "use strict";
    // var id = getID(this.id);
    var mystage = OrthoVariables.origCanvas[id][2];
    //console.log(id);
    var mousepos = {"x": x, "y": y};
    if (mousepos !== undefined) {

        var myshapelayer = mystage.get(".shapelayer")[0];
        var mytooltip = mystage.get(".tooltiplayer")[0].get(".tooltip")[0];
        if (!OrthoVariables.buttonState[lessonPage].l && ishotspots === "yes") {
            if (OrthoVariables.PageTracking[lessonPage].status === "correct") {
                ShowMsg("You already answer it!", "highlight");
                return true;
            }
            if (OrthoVariables.PageTracking[lessonPage].status === "wrong" && OrthoVariables.LessonData.Page[lessonPage].attributes.Blocked === "no") {
                ShowMsg("You already answer it!", "highlight");
                return true;
            }
            var shapes = mystage.getIntersection({
                x: mousepos.x / (OrthoVariables.scalePage[lessonPage] * OrthoVariables.zoomPage[lessonPage]),
                y: mousepos.y / (OrthoVariables.scalePage[lessonPage] * OrthoVariables.zoomPage[lessonPage])
            });
            if (shapes === null) {
                var circle = new Kinetic.Circle({
                    x: mousepos.x / (OrthoVariables.scalePage[lessonPage] * OrthoVariables.zoomPage[lessonPage]),
                    y: mousepos.y / (OrthoVariables.scalePage[lessonPage] * OrthoVariables.zoomPage[lessonPage]),
                    radius: 10,
                    fill: "#cb842e",
                    stroke: "#cbb48f",
                    strokeWidth: 1,
                    opacity: 0.5,
                    id: "circle_" + id,
                    name: "circle_" + id
                });

                circle.on("mouseover", function () {
                    if (OrthoVariables.PageTracking[lessonPage].status === "pending") {
                        $("#pointer_" + id).removeClass().addClass("erasercursor");
                        var mousePos = mystage.getMousePosition();
                        var x = (mousePos.x + 10) / (OrthoVariables.scalePage[lessonPage] * OrthoVariables.zoomPage[lessonPage]);
                        var y = (mousePos.y + 15) / (OrthoVariables.scalePage[lessonPage] * OrthoVariables.zoomPage[lessonPage]);
                        drawTooltip(mytooltip, x, y, "click to remove");
                        var tween = new Kinetic.Tween({
                            node: this,
                            scaleX: 1.7,
                            scaleY: 1.7,
                            duration: 0.3,
                            easing: Kinetic.Easings.EaseOut
                        });
                        tween.play();
                    }

                });
                circle.on("mouseout", function () {

                    if (!OrthoVariables.clickcatch) {
                        SetCursor(id);
                        mytooltip.hide();
                        var mytooltiplayer = mytooltip.getLayer();
                        mytooltiplayer.get(".backgroundRect").remove();
                        mytooltiplayer.draw();
                        var tween = new Kinetic.Tween({
                            node: this,
                            scaleX: 1,
                            scaleY: 1,
                            duration: 0.3,
                            easing: Kinetic.Easings.EaseIn
                        });
                        tween.play();
                    }

                });
                circle.on("click", function () {
                    if (OrthoVariables.PageTracking[lessonPage].status === "pending") {
                        OrthoVariables.clickcatch = true;
                        this.off("mouseout");
                        this.off("mouseover");

                        OrthoVariables.lessonAnswers[lessonPage].hotspots[circle._id] = undefined;
                        var mytooltip = mystage.get(".tooltiplayer")[0].get(".tooltip")[0];
                        mytooltip.hide();
                        var mytooltiplayer = mytooltip.getLayer();
                        mytooltiplayer.get(".backgroundRect").remove();
                        mytooltiplayer.draw();
                        this.remove();
                        //this.getLayer().draw();
                        mystage.draw();
                        SetCursor(id);
                        if (!isDraw) {
                            DisableButtonLink("SubmitAnswer");
                        }
                    }

                });
                if (!OrthoVariables.clickcatch && !ReachMaxNumberHotSpots(lessonPage)) {
                    myshapelayer.add(circle);
                    OrthoVariables.lessonAnswers[lessonPage].hotspots[circle._id] = [Math.round(mousepos.x / (OrthoVariables.scalePage[lessonPage] * OrthoVariables.zoomPage[lessonPage])), Math.round(mousepos.y / (OrthoVariables.scalePage[lessonPage] * OrthoVariables.zoomPage[lessonPage]))];
                    myshapelayer.draw();
                    $("#pointer_" + id).removeClass().addClass("erasercursor");
                    if (ReachMaxNumberHotSpots(lessonPage) && !isDraw) {
                        EnableButtonLink("SubmitAnswer");
                    }
                } else if (ReachMaxNumberHotSpots(lessonPage)) {
                    ShowMsg("Reach maximum points. Please remove the previous to add new ones.", "highlight");
                }
                OrthoVariables.clickcatch = false;
            }
        }

    }
}

function drawTooltip(tooltip, x, y, text) {
    "use strict";
    tooltip.setText(text);
    tooltip.setPosition(x, y);
    tooltip.show();
    tooltip.setScale(1 / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]), 1 / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]));
    var tooltiplayer = tooltip.getLayer();

    var rect = new Kinetic.Rect({
        x: x,
        y: y,
        height: tooltip.getHeight(),
        width: tooltip.getWidth(),
        fill: "black",
        name: "backgroundRect"
    });
    rect.setScale(1 / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]), 1 / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]));

    tooltiplayer.add(rect);
    rect.moveToBottom();
    tooltiplayer.draw();

}

function drawTriangle(triangle, points) {
    "use strict";
    triangle.setPoints(points);
    triangle.show();
    triangle.setScale(1 / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]), 1 / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]));
    triangle.getLayer().draw();
}

function CheckShape(strID) {
    "use strict";
    var id = getID(strID);
    var mystage = OrthoVariables.origCanvas[id][2];
    var mousepos = mystage.getMousePosition();
    if (mousepos !== undefined) {
        var distance = Distance(OrthoVariables.line.startx, OrthoVariables.line.starty, mousepos.x / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]), mousepos.y / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]));
        if (OrthoVariables.line.prevline !== null && distance < OrthoVariables.linemindistance) {
            var myshapelayer = mystage.get(".shapelayer")[0];
            OrthoVariables.line.prevline.remove();
            OrthoVariables.line.prevline = null;
            myshapelayer.draw();
        }
        if (OrthoVariables.line.prevline !== null) {
            checkIntersections(id);
        }
    }

}

function checkIntersections(id) {
    "use strict";
    var mystage = OrthoVariables.origCanvas[id][2];
    //var mousepos = mystage.getMousePosition();
    var groupline = OrthoVariables.line.prevline;
    //var line = groupline.get("#" + groupline.getId())[0];
    var line = groupline.get("Line")[0];
    var points = line.getPoints();
    var prevlines = line.getLayer().getChildren();
    for (var l in prevlines) {
        if (prevlines[l] instanceof Kinetic.Group) {
            if (prevlines[l].getId() !== line.getId()) {
                var t_points = prevlines[l].get("Line")[0].getPoints();
                var intersectPoint = lineToLineIntersect(points[0].x, points[0].y, points[1].x, points[1].y, t_points[0].x, t_points[0].y, t_points[1].x, t_points[1].y);
                if (intersectPoint.x !== 0 || intersectPoint.y !== 0) {
                    var p1 = minPointY(points[0], points[1], intersectPoint);
                    var p2 = minPointY(t_points[0], t_points[1], intersectPoint);
                    var p1A = maxPointY(points[0], points[1], intersectPoint);
                    var p2A = maxPointY(t_points[0], t_points[1], intersectPoint);
                    var angle = calcAngles(p1.x, p1.y, intersectPoint.x, intersectPoint.y, p2.x, p2.y, intersectPoint.x, intersectPoint.y);
                    addAngleCircle(intersectPoint.x, intersectPoint.y, groupline, angle, mystage, p1, p2, p1A, p2A);

                }
            }
        }
    }
    mystage.draw();
}

function calcAngles(x1, y1, x2, y2, x3, y3, x4, y4) {
    "use strict";
    var dx1 = x2 - x1;
    var dy1 = y2 - y1;
    var dx2 = x4 - x3;
    var dy2 = y4 - y3;
    var d = dx1 * dx2 + dy1 * dy2;   // dot product of the 2 vectors
    var l2 = (dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2); // product of the squared lengths
    var angle = Math.round(Math.acos(d / Math.sqrt(l2)) * (180 / Math.PI));
    //console.log(angle);
    return angle;
}

function minPointY(pointA, pointB, pointX) {
    "use strict";
    return (pointA.y < pointX.y ) ? pointA : pointB;
}

function maxPointY(pointA, pointB, pointX) {
    "use strict";
    return (pointA.y < pointX.y) ? pointB : pointA;
}

function addAngleCircle(x, y, group, angle, mystage, point1, point2, point1A, point2A) {
    "use strict";
    var circle = new Kinetic.Circle({
        x: x, // / (OrthoVariables.scalePage[OrthoVariables.lessonPage]*OrthoVariables.zoomPage[OrthoVariables.lessonPage]),
        y: y, // / (OrthoVariables.scalePage[OrthoVariables.lessonPage]*OrthoVariables.zoomPage[OrthoVariables.lessonPage]),
        radius: 10,
        fill: "#FF550C",
        stroke: "#ff0000",
        strokeWidth: 1,
        opacity: 0.7
        //id:"circle_" + id
    });

    var leftAngleTip = new Kinetic.Text({
        text: "",
        fill: "#DDDDDD",
        fontFamily: "MerriweatherRegular,Georgia",
        fontSize: 14,
        fontStyle: "normal",
        verticalAlign: "left",
        lineHeight: 1,
        padding: 13,
        //fill:"black",
        visible: false,
        opacity: 1
        //id:"leftAngleTip"
    });

    var triangle1 = new Kinetic.Polygon({
        points: [x, y, point1.x, point1.y, point2.x, point2.y],
        fill: "#F74E0C",
        stroke: "#ff0000",
        strokeWidth: 1,
        opacity: 0.4,
        visible: false
    });

    var triangle1A = triangle1.clone({
        points: [x, y, point1A.x, point1A.y, point2A.x, point2A.y]
    });


    var triangle2 = triangle1.clone({
        points: [x, y, point1.x, point1.y, point2A.x, point2A.y],
        fill: "#38A008", stroke: "#00ff00"
    });

    var triangle2A = triangle2.clone({
        points: [x, y, point1A.x, point1A.y, point2.x, point2.y]
    });

    var rect1 = new Kinetic.Rect({
        fill: "#F74E0C",
        stroke: "#ff0000",
        strokeWidth: 1,
        opacity: 0.7,
        visible: false,
        x: 72,
        y: 8,
        width: 20,
        height: 20
    });

    var rect2 = rect1.clone({
        x: 130,
        y: 8,
        fill: "#38A008",
        stroke: "#00ff00"
    });
    //group.setVisible(false);

    circle.on("mouseover", function () {
        OrthoVariables.isOverAngleCircle = true;
        var langlex = 0;
        var langley = -1;
        var r_angle = 180 - parseInt(angle, 10);
        drawTooltip(leftAngleTip, langlex, langley, "Angles:       " + angle + "⁰,        " + r_angle + "⁰");
        triangle1.show();
        triangle1A.show();
        triangle2.show();
        triangle2A.show();
        resetRectDims(rect1, 72);
        resetRectDims(rect2, 130);
        rect1.show();
        rect2.show();
        rect1.moveToTop();
        leftAngleTip.getLayer().draw();
    });

    circle.on("mouseout", function () {
        OrthoVariables.isOverAngleCircle = false;
        leftAngleTip.hide();
        triangle1.hide();
        triangle1A.hide();
        triangle2.hide();
        triangle2A.hide();
        rect1.hide();
        rect2.hide();
        var mytooltiplayer = leftAngleTip.getLayer();
        mytooltiplayer.get(".backgroundRect").remove();
        mytooltiplayer.draw();
    });
    group.add(leftAngleTip);
    group.add(triangle1);
    group.add(triangle1A);
    group.add(triangle2);
    group.add(triangle2A);
    group.add(rect1);
    group.add(rect2);
    group.add(circle);

}

function resetRectDims(rect, x) {
    var size = rect.getSize();
    var pos = rect.getPosition();
    size.width = 20 / (OrthoVariables.zoomPage[OrthoVariables.lessonPage] * OrthoVariables.scalePage[OrthoVariables.lessonPage]);
    size.height = 20 / (OrthoVariables.zoomPage[OrthoVariables.lessonPage] * OrthoVariables.scalePage[OrthoVariables.lessonPage]);
    pos.x = x / (OrthoVariables.zoomPage[OrthoVariables.lessonPage] * OrthoVariables.scalePage[OrthoVariables.lessonPage]);
    pos.y = 8 / (OrthoVariables.zoomPage[OrthoVariables.lessonPage] * OrthoVariables.scalePage[OrthoVariables.lessonPage]);
    rect.setPosition(pos.x, pos.y);
    rect.setSize(size.width, size.height);

}

function moveAngleTip(x1, x2, x) {
    "use strict";
    var variable = 0;
    var k1 = x1 - x;
    var k2 = x2 - x;
    if (k1 >= 0 && k2 >= 0) {
        variable = Math.max((Math.min(x1, x2) - x) / 2, 90);
    } else if (k1 <= 0 && k2 <= 0) {
        variable = Math.min(-x / 2 + Math.max(x1, x2) / 2, -4);
    }
    else {
        variable = 0;
    }
    return variable;
}

function moveAngleTipY(y1, y2, y) {
    "use strict";
    var variable = 0;

}


function lineToLineIntersect(x1, y1, x2, y2, x3, y3, x4, y4) {
    "use strict";
    var UBottom, Ua, Ub, interceptX, interceptY;
    var i = {b: false, x: 0, y: 0};
    UBottom = ((y4 - y3) * (x2 - x1)) - ((x4 - x3) * (y2 - y1));
    if (UBottom !== 0) {
        Ua = (((x4 - x3) * (y1 - y3)) - ((y4 - y3) * (x1 - x3))) / UBottom;
        Ub = (((x2 - x1) * (y1 - y3)) - ((y2 - y1) * (x1 - x3))) / UBottom;
        if ((Ua >= 0) && (Ua <= 1) && (Ub >= 0) && (Ub <= 1)) {
            interceptX = x1 + (Ua * (x2 - x1));
            interceptY = y1 + (Ua * (y2 - y1));
            i.b = true;
            i.x = interceptX;
            i.y = interceptY;
            return i;
        }
    }
    return i;
}


function clearTrackingLines(canvasID) {
    "use strict";
    var mystage = OrthoVariables.origCanvas[canvasID][2];
    var mytracking = mystage.get(".trackinglayer")[0];
    mytracking.removeChildren();
    mytracking.draw();
}

function DrawTrackingLines(canvasID) {
    "use strict";
    var mystage = OrthoVariables.origCanvas[canvasID][2];
    var mousepos = mystage.getMousePosition();
    var width = mystage.getWidth() / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]);
    var height = mystage.getHeight() / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]);
    var strokecolor = "#227BAB";
    if (mousepos !== undefined) {
        var mytracking = mystage.get(".trackinglayer")[0];
        mytracking.removeChildren();
        var x1 = mousepos.x / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]);
        var y1 = mousepos.y / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]);
        var lineHorizontal = new Kinetic.Line({
            points: [
                { x: 0, y: y1},
                {x: width, y: y1}
            ],
            stroke: strokecolor,
            lineCap: "butt",
            opacity: 0.7,
            strokeWidth: 3,
            dashArray: [10, 5]
        });
        var lineVertical = new Kinetic.Line({
            points: [
                { x: x1, y: 0},
                {x: x1, y: height}
            ],
            stroke: strokecolor,
            lineCap: "butt",
            opacity: 0.7,
            strokeWidth: 3,
            dashArray: [10, 5]
        });
        mytracking.add(lineHorizontal);
        mytracking.add(lineVertical);
        mytracking.draw();
    }
}

function DrawShape(strid) {
    "use strict";
    var id = getID(strid);
    var shapeid = OrthoVariables.line.id;
    var mystage = OrthoVariables.origCanvas[id][2];
    var mousepos = mystage.getMousePosition();
    if (mousepos !== undefined && mystage.get(".shapelayer")[0] !== undefined) {
        var myshapelayer = mystage.get(".shapelayer")[0];

        if (OrthoVariables.line.prevline !== null) {
            OrthoVariables.line.prevline.remove();
        }
        var x1 = mousepos.x / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]);
        var y1 = mousepos.y / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]);
        var line = new Kinetic.Line({
            points: [
                {x: OrthoVariables.line.startx, y: OrthoVariables.line.starty },
                {x: x1, y: y1}
            ],
            fill: "orange",
            stroke: "orange",
            strokeWidth: 5,
            id: "line_" + shapeid,
            name: "line_" + shapeid
        });

        var linegroup = new Kinetic.Group({
            id: "line_" + shapeid,
            name: "line_" + shapeid
        });
        linegroup.add(line);


        myshapelayer.add(linegroup);
        OrthoVariables.line.prevline = linegroup;
        myshapelayer.draw();
    }
}

function Distance(x1, y1, x2, y2) {
    return Math.abs(x2 - x1) + Math.abs(y2 - y1);
}

function EuclideanDistance(x1, y1, x2, y2) {
    return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
}

function horizAngle(x1, y1, x2, y2) {
    return Math.round(-Math.atan2(y2 - y1, x2 - x1) * 180 / Math.PI);
}

function SetonLine(strID) {
    var id = getID(strID);
    var line = OrthoVariables.line.prevline;
    if (line !== null) {

        var mouseover_func = function (obj) {
            $("#pointer_" + id).removeClass().addClass("erasercursor");
            var mystage = obj.getStage();
            var mytooltip = mystage.get(".tooltiplayer")[0].get(".tooltip")[0];
            var mousepos = mystage.getMousePosition();
            var x = mousepos.x / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]) + 10;
            var y = mousepos.y / (OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.zoomPage[OrthoVariables.lessonPage]) + 5;
            drawTooltip(mytooltip, x, y, "click to remove");
        };

        var mouseout_func = function (obj, lid) {
            SetCursor(lid);
            var mystage = obj.getStage();
            if (mystage !== undefined) {
                var mytooltip = mystage.get(".tooltiplayer")[0].get(".tooltip")[0];
                mytooltip.hide();
                var mytooltiplayer = mytooltip.getLayer();
                mytooltiplayer.get(".backgroundRect").remove();
                mytooltiplayer.draw();
            }

        };
        line.on("mouseover", function () {
            if (!OrthoVariables.isOverAngleCircle) {
                mouseover_func(this);
            }
        });

        //infotip.on("mouseover", function() { mouseover_func(this);});


        line.on("mouseout", function () {
            mouseout_func(this, id);
        });

        //infotip.on("mouseout", function() {mouseout_func(this,id)});

        line.on("click", function () {
            if (!OrthoVariables.isOverAngleCircle) {
                OrthoVariables.clickcatch = true;
                SetCursor(id);
                var mylayer = this.getLayer();
                var shapeid = this.getId().split('_')[1];
                var mystage = this.getStage();
                var mytooltip = mystage.get(".tooltiplayer")[0].get(".tooltip")[0];
                mytooltip.hide();
                var mytooltiplayer = mytooltip.getLayer();
                mytooltiplayer.get(".backgroundRect").remove();
                mytooltiplayer.draw();
                this.off("mouseout");
                this.off("mouseover");
                this.remove();
                mylayer.draw();
                OrthoVariables.line.pressed = false;
            }


        });

    }
}

function SetCursor(id) {
    $("#pointer_" + id).removeClass();
    if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"]) {
        $("#pointer_" + id).addClass("pencilcursor");
    } else if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["h"]) {
        $("#pointer_" + id).addClass("pointcursor");
    }

}

function getID(strID) {
    return strID.substr(strID.lastIndexOf("_") + 1, strID.length);

}

function getBrOrCo(strID) {
    return strID.substr(strID.indexOf("_") + 1, 1);
}


function displayFunctions() {
    $('#lesson').turn({duration: 600});
    $('#lesson').turn('size', $('#content_wrap').width(), $(window).height() - OrthoVariables.HeightFromBottom);
    //console.log("turn size:",$('#content_wrap').width());
    $('#lesson').turn('disable', OrthoVariables.disableturn);
    //for debuging
    //CurPage = 3; ShowPage();

    $(window).bind('keydown',
        function (e) {
            if (e.keyCode == 37)
                $('#lesson').turn('previous');
            else if (e.keyCode == 39)
                $('#lesson').turn('next');

        }).resize(function () {
            //$('body').prepend('<div>' + $('#content_wrap').width() + '</div>');
            var h = $(window).height() - OrthoVariables.HeightFromBottom;
            h = (h < 460) ? 460 : h;

            var w = $('#content_wrap').width();
            w = (w < 720) ? 720 : w;
            CheckResizeLimits();
            $('#lesson').turn('size', w, h);
            //console.log("after resize limits",w,h);

        });

    $('#lesson').bind('turning', function (e, page, pageObj) {
        $("#NextTest").data("fire", false);
        $("#PreviousTest").data("fire", false);
    });
    $('#lesson').bind('turned', function (e, page, pageObj) {
        pageIsTurned(page);


    });

    $("#lesson").bind("last", function (e, page, pageObj) {
        $("#pageresults").html($("#EndPageTemplate").render(OrthoVariables));
    });


}

function pageIsTurned(newPage) {
    $("#NextTest").data("fire", true);
    $("#PreviousTest").data("fire", true);

    OrthoVariables.CurPage = newPage % 2 === 0 && newPage !== 1 ? newPage + 1 : newPage;
    var lessonpage = (OrthoVariables.CurPage === 0 || OrthoVariables.CurPage >= OrthoVariables.maxPages) ? -1 : ( Math.floor(OrthoVariables.CurPage / 2)) - 1;
    OrthoVariables.lessonPage = lessonpage;
    if (OrthoVariables.maxPages >= OrthoVariables.CurPage) {
        ApplyRoundtoPages(OrthoVariables.CurPage, OrthoVariables.CurPage + 2);
        LoadImages((OrthoVariables.lessonPage + 1).toString());
        loadSpinControl((OrthoVariables.lessonPage + 1).toString());
        var nPage = OrthoVariables.lessonPage + 1;

        if (OrthoVariables.LessonData.Page[nPage] != undefined) {
            if (!(OrthoVariables.LessonData.Page[nPage].Widget[0].type === "image" || OrthoVariables.LessonData.Page[nPage].Widget[1].type === "image")) {
                loadPreviousAnswers(nPage);
            }
        }
        //setTracking((OrthoVariables.lessonPage + 1).toString());
        //loadPreviousAnswers((OrthoVariables.lessonPage + 1).toString());
    }

    if (OrthoVariables.CurPage <= 1) {
        DisableButtonLink("PreviousTest");
        EnableButtonLink("NextTest");
        DisableButtonLink("SubmitAnswer");
    }
    else if (OrthoVariables.CurPage >= OrthoVariables.maxPages) {
        EnableButtonLink("PreviousTest");
        DisableButtonLink("NextTest");
        DisableButtonLink("SubmitAnswer");
    }
    else {
        EnableButtonLink("PreviousTest");
        if (!OrthoVariables.PageTracking[lessonpage].nextpass) {
            DisableButtonLink("NextTest");
        }
        else {
            EnableButtonLink("NextTest");
        }

        if (OrthoVariables.PageTracking[lessonpage].submitbutton) {
            EnableButtonLink("SubmitAnswer");
        } else {
            DisableButtonLink("SubmitAnswer");
        }
    }


    CheckResizeLimits();
    if ((OrthoVariables.lessonPage + 1) < OrthoVariables.LessonData.Page.length) {
        CheckResizeLimits(OrthoVariables.lessonPage + 1);
    }
    $("#curPage").html(parseInt(OrthoVariables.CurPage / 2));
    $('#counter_small').hide();
    $('#counter').stop(true).fadeIn("slow").delay(5000).fadeOut(1000, function () {
        $("#counter_small").show();
    });

}


// Image Functions
function CheckResizeLimits(page) {
    page = (typeof page === "undefined") ? OrthoVariables.lessonPage : page;
    if (page > -1) {
        var h = $(window).height() - OrthoVariables.HeightFromBottom;
        var w = Math.round($('#content_wrap').width() / 2);
        //from the padding
        w -= 60;
        w = (w < 300) ? 300 : w;
        //console.log("w,h",w,h);
        var mypage = OrthoVariables.LessonData.Page[page];
        var subid = (mypage.Widget[0].type === "image") ? 0 : 1;

        var id = page.toString();
        if (OrthoVariables.origCanvas[id] !== undefined) {

            var cW = Math.round(OrthoVariables.origCanvas[id][0].width);
            var cH = Math.round(OrthoVariables.origCanvas[id][0].height);
            var ratio = cW / cH;
            var nH = cH;
            var nW = cW;
            if (cH > (h - 80 - 30)) {
                nH = h - 80 - 30;
                nH = (nH < 330 ) ? 330 : nH;
                nW = ratio * nH;
                if (nW > w - 20) {
                    nW = w - 20;
                    nH = nW / ratio;
                }
            }
            else if (cW > w - 20) {
                nW = w - 20;
                nH = nW / ratio;
            }
            /*if (nW < 380) {
             nW = 380;
             nH = nW /ratio;
             }*/
            //console.log(nH,nW);
            resize(id, nH, nW, page);
            //console.log(nW,nH);
        }
        else {
            for (var wid in OrthoVariables.LessonData.Page[page].Widget) {
                if (OrthoVariables.LessonData.Page[page].Widget[wid].type === "video") {
                    w = Math.round($('#content_wrap').width() / 2) - 80;
                    var maxh = $(window).height() - OrthoVariables.HeightFromBottom;
                    var myvideo = $("#video_" + OrthoVariables.LessonData.Page[page].Widget[wid].Video.id);
                    var n_w = myvideo.width();
                    var n_h = myvideo.height();
                    if (n_w > 0 && n_h > 0) {
                        if (n_w == 0) n_w = 150;
                        if (n_h == 0) n_h = 150;
                        h = Math.round(w * (n_h / n_w));
                        if (h > maxh - 35) {
                            h = maxh - 35;
                            w = Math.round(h * (n_w / n_h));
                        }
                        //var vid = OrthoVariables.LessonData.Page[page].Widget[wid].Video.id;
                        /*for (var e in mejs.players) {
                            if (mejs.players[e].media.id === "video_" + vid) {
                                mejs.players[e].setPlayerSize(w, h);
                                mejs.players[e].media.setVideoSize(w, h);
                                mejs.players[e].setControlsSize();
                            }
                        }*/
                        myvideo.width(w);
                        myvideo.height(h);
                    }
                    else {
                        myvideo.width(w);
                    }
                }
            }
        }


    }
}

function resize(id, newHeight, newWidth, page) {
    page = page || OrthoVariables.lessonPage;
    OrthoVariables.scalePage[page] = newHeight / OrthoVariables.origCanvas[id][0].height;
    $("#canvasid_" + id).css("height", newHeight).css("width", newWidth);
    OrthoVariables.origCanvas[id][2].setSize(newWidth, newHeight);
    OrthoVariables.origCanvas[id][2].setScale(OrthoVariables.scalePage[page], OrthoVariables.scalePage[page] * OrthoVariables.zoomPage[page]);
    OrthoVariables.origCanvas[id][2].draw();
    $("#pointer_" + id).css("height", newHeight).css("width", newWidth);
    $("#container_" + id).css("top", -newHeight);
    $("#1_" + id).css("width", newWidth).css("top", newHeight + 30);
    $("#2_" + id).css("width", newWidth).css("top", newHeight + 30);
    //$("#slider_b_" + id).css("left", 272);
    //$("#slider_c_" + id).css("left", 317);
    $("#pointer_" + id).parent().css("height", newHeight);
    if (OrthoVariables.zoomPage[page] !== 1) {
        $("#pointer_" + id).css("top", "0px").css("left", "0px");
        var ratio = OrthoVariables.origCanvas[id][0].width / OrthoVariables.origCanvas[id][0].height;
        var newWidth = Math.round(OrthoVariables.zoomPage[id] * OrthoVariables.scalePage[page] * OrthoVariables.origCanvas[id][0].width);
        var newHeight = Math.round(newWidth / ratio);
        zoomResize(id, newHeight, newWidth, page);

    }

}

function zoomInImage(id) {
    /* Opens another windows

     $("#modal_"+id).dialog("option","height", $("section#lesson").height());
     $("#modal_"+id).dialog("option","width", $("section#lesson").width());
     $("#modal_"+id).dialog('open');
     $("a#jqlink_"+id).CloudZoom();*/
    /*var div =  $("#pointer_" + id).parent();
     var h = $(window).height() - OrthoVariables.HeightFromBottom;
     var w = Math.round($('#content_wrap').width()/2) - 10;
     var cW = Math.round(OrthoVariables.origCanvas[id][0].width);
     var cH = Math.round(OrthoVariables.origCanvas[id][0].height);
     var ratio = cW / cH;
     div.css("height",OrthoVariables.origCanvas[id][0].height* OrthoVariables.scalePage[OrthoVariables.lessonPage]).css("width", w);*/
    OrthoVariables.zoomPage[id] *= 1.2;
    if (OrthoVariables.zoomPage[id] >= 6) {
        OrthoVariables.zoomPage[id] = 6;
    }
    var ratio = OrthoVariables.origCanvas[id][0].width / OrthoVariables.origCanvas[id][0].height;
    var newWidth = Math.round(OrthoVariables.zoomPage[id] * OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.origCanvas[id][0].width);
    var newHeight = Math.round(newWidth / ratio);
    zoomResize(id, newHeight, newWidth);
}

function zoomOutImage(id) {
    OrthoVariables.zoomPage[id] *= 0.8;
    if (OrthoVariables.zoomPage[id] <= 0.25) {
        OrthoVariables.zoomPage[id] = 0.25
    }
    $("#pointer_" + id).css("top", "0px").css("left", "0px");
    var ratio = OrthoVariables.origCanvas[id][0].width / OrthoVariables.origCanvas[id][0].height;
    var newWidth = Math.round(OrthoVariables.zoomPage[id] * OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.origCanvas[id][0].width);
    var newHeight = Math.round(newWidth / ratio);
    zoomResize(id, newHeight, newWidth);
}

function zoomRsImage(id) {
    OrthoVariables.zoomPage[id] = 1;
    $("#pointer_" + id).css("top", "0px").css("left", "0px");
    var ratio = OrthoVariables.origCanvas[id][0].width / OrthoVariables.origCanvas[id][0].height;
    var newWidth = Math.round(OrthoVariables.zoomPage[id] * OrthoVariables.scalePage[OrthoVariables.lessonPage] * OrthoVariables.origCanvas[id][0].width);
    var newHeight = Math.round(newWidth / ratio);
    zoomResize(id, newHeight, newWidth);
}

function zoomResize(id, newHeight, newWidth, page) {
    //OrthoVariables.scalePage[OrthoVariables.lessonPage] = newHeight / OrthoVariables.origCanvas[id][0].height;
    page = page || OrthoVariables.lessonPage;
    $("#canvasid_" + id).css("height", newHeight).css("width", newWidth);
    OrthoVariables.origCanvas[id][2].setSize(newWidth, newHeight);
    OrthoVariables.origCanvas[id][2].setScale(OrthoVariables.scalePage[page] * OrthoVariables.zoomPage[id], OrthoVariables.scalePage[page] * OrthoVariables.zoomPage[id]);
    $("#pointer_" + id).css("height", newHeight).css("width", newWidth);
    $("#container_" + id).css("top", -newHeight);
    OrthoVariables.origCanvas[id][2].draw();
}

function InvertImage(id) {
    var c_slider = $('#slider_c_' + id);
    var b_slider = $('#slider_b_' + id);
    if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["c"]) {
        c_slider.hide('slide', function () {
            ShowOffImage(id, "contrast");
        });
        OrthoVariables.buttonState[OrthoVariables.lessonPage]["c"] = false;
    }
    if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["b"]) {
        b_slider.hide('slide', function () {
            ShowOffImage(id, "brightness");
        });
        OrthoVariables.buttonState[OrthoVariables.lessonPage]["b"] = false;
    }
    //var mycanvas = $('#canvasid_' + id).get(0);
    //mycanvas.getContext("2d").zag_Invert(0, 0, mycanvas.width, mycanvas.height);
    //OrthoVariables.origCanvas[id][0] = mycanvas.zag_Clone();
    OrthoVariables.origCanvas[id][5] = !OrthoVariables.origCanvas[id][5];
    ApplyImageOperations(id);
}

function ApplyImageOperations(id) {
    var mycanvas = $('#canvasid_' + id).get(0);
    var canvasobj = OrthoVariables.origCanvas[id][0].zag_Clone();
    var canvasobjcontext = canvasobj.getContext("2d");
    var bright = OrthoVariables.origCanvas[id][3];
    var contr = OrthoVariables.origCanvas[id][4];
    var invert = OrthoVariables.origCanvas[id][5];
    /*if (bright !== 0) {
     canvasobjcontext.zag_Brightening(bright, 0, 0, mycanvas.width, mycanvas.height);
     }

     if (contr !== 0) {
     canvasobjcontext.zag_Contrast(contr, 0, 0, mycanvas.width, mycanvas.height);
     }

     if (invert) {
     canvasobjcontext.zag_Invert(0, 0, mycanvas.width, mycanvas.height);
     }*/
    canvasobjcontext.zag_BCI(bright, contr, invert, 0, 0, mycanvas.width, mycanvas.height);
    mycanvas.getContext("2d").drawImage(canvasobj, 0, 0);

}

function SaveImageState(id) {
    var mycanvas = $('#canvasid_' + id).get(0);
    OrthoVariables.origCanvas[id][0] = mycanvas.zag_Clone();
}

function Brightness(id, value, canvasobj) {
    var mycanvas = $('#canvasid_' + id).get(0);
    canvasobj.getContext("2d").zag_Brightening(value, 0, 0, mycanvas.width, mycanvas.height);
    mycanvas.getContext("2d").drawImage(canvasobj, 0, 0);

}

function ShowOnImage(id, imgTarget) {
    if (!(imgTarget === "brightness" && OrthoVariables.buttonState[OrthoVariables.lessonPage]["b"])
        && !(imgTarget === "contrast" && OrthoVariables.buttonState[OrthoVariables.lessonPage]["c"])
        && !(OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"] && imgTarget === "line")
        && !(OrthoVariables.buttonState[OrthoVariables.lessonPage]["t"] && imgTarget === "target")
        && !(OrthoVariables.buttonState[OrthoVariables.lessonPage]["h"] && imgTarget === "hotspot")
        ) {
        var imgobj = $("#" + imgTarget + "_" + id);
        imgobj.attr("src", imgobj.attr("src").replace(".", "_on."));
    }
}

function ShowOffImage(id, imgTarget) {
    if (!(imgTarget === "brightness" && OrthoVariables.buttonState[OrthoVariables.lessonPage]["b"])
        && !(imgTarget === "contrast" && OrthoVariables.buttonState[OrthoVariables.lessonPage]["c"])
        && !(OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"] && imgTarget === "line")
        && !(OrthoVariables.buttonState[OrthoVariables.lessonPage]["t"] && imgTarget === "target")
        && !(OrthoVariables.buttonState[OrthoVariables.lessonPage]["h"] && imgTarget === "hotspot")
        ) {
        var imgobj = $("#" + imgTarget + "_" + id);
        imgobj.attr("src", imgobj.attr("src").replace("_on.", "."));
    }
}

function TogglePaint(action, id) {
    if (!(OrthoVariables.PageTracking[OrthoVariables.lessonPage].status === "correct"
        || OrthoVariables.PageTracking[OrthoVariables.lessonPage].status === "wrong")) {
        switch (action) {
            case "line":
                if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["h"]) {
                    TogglePaint("hotspot", id);
                    ShowOffImage(id, "hotspot");
                }
                OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"] = !OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"];
                if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"]) {
                    $("#pointer_" + id).removeClass().addClass("pencilcursor");
                } else {
                    $("#pointer_" + id).removeClass();
                }
                break;
            case "target":
                if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["h"]) {
                    TogglePaint("hotspot", id);
                    ShowOffImage(id, "hotspot");
                }
                OrthoVariables.buttonState[OrthoVariables.lessonPage]["t"] = !OrthoVariables.buttonState[OrthoVariables.lessonPage]["t"];
                break;
            case "hotspot":
                if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"]) {
                    TogglePaint("line", id);
                    ShowOffImage(id, "line");
                }
                if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["t"]) {
                    TogglePaint("target", id);
                    ShowOffImage(id, "target");
                }
                OrthoVariables.buttonState[OrthoVariables.lessonPage]["h"] = !OrthoVariables.buttonState[OrthoVariables.lessonPage]["h"];
                if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["h"]) {
                    $("#pointer_" + id).removeClass().addClass("pointcursor");
                } else {
                    $("#pointer_" + id).removeClass();
                }
                break;
        }
    }
}

function Contrast(id, value, canvasobj) {
    var mycanvas = $('#canvasid_' + id).get(0);
    canvasobj.getContext("2d").zag_Contrast(value, 0, 0, mycanvas.width, mycanvas.height);
    mycanvas.getContext("2d").drawImage(canvasobj, 0, 0);
}

function ReloadImage(id) {
    //alert(id);
    //$('#canvasid_' + id).get(0).getContext("2d").drawImage(OrthoVariables.origCanvas[id],0,0);
    if (!(OrthoVariables.PageTracking[OrthoVariables.lessonPage].status === "correct" || OrthoVariables.PageTracking[OrthoVariables.lessonPage].status === "wrong")) {
        $('#slider_b_' + id).slider('value', 0);
        $('#slider_c_' + id).slider('value', 0);
        OrthoVariables.origCanvas[id][3] = 0;
        OrthoVariables.origCanvas[id][4] = 0;
        OrthoVariables.origCanvas[id][5] = false;
        //mycanvas.getContext("2d").drawImage(canvasobj, 0, 0);
        $('#canvasid_' + id).get(0).getContext("2d").drawImage(OrthoVariables.origCanvas[id][0], 0, 0);
        var mystage = OrthoVariables.origCanvas[id][2];
        var myshapelayer = mystage.get(".shapelayer")[0];
        myshapelayer.removeChildren();
        if (OrthoVariables.lessonAnswers[id].hotspots.length > 0) {
            OrthoVariables.lessonAnswers[id].hotspots = [];
            DisableButtonLink("SubmitAnswer");
        }


        mystage.draw();
    }
}

function ActionSlider(sliderid, action) {
    var myslide = $('#' + sliderid);
    var borc = getBrOrCo(sliderid);
    switch (action) {
        case 'toggle':
            myslide.toggle('slide');
            OrthoVariables.buttonState[OrthoVariables.lessonPage][borc] = !OrthoVariables.buttonState[OrthoVariables.lessonPage][borc];
            break;
        case 'show':
            if (!OrthoVariables.buttonState[OrthoVariables.lessonPage][borc]) {
                myslide.show('slide');
                OrthoVariables.buttonState[OrthoVariables.lessonPage][borc] = true;
            }
            break;
        case 'hide':
            if (OrthoVariables.buttonState[OrthoVariables.lessonPage][borc]) {
                var id = getID(sliderid)
                //SaveImageState(id);
                //myslide.slider('value', 0);
                myslide.hide('slide', function () {
                    ShowOffImage(id, (borc === "c") ? "contrast" : "brightness");
                });
                OrthoVariables.buttonState[OrthoVariables.lessonPage][borc] = false;
            }
            break;
    }

}

// Book Like Functions
function ApplyRoundtoPages(Start, End) {
    for (var i = Start; i <= End; i++) {
        if (i % 2 == 0) {
            $(".p" + i).addClass("even");
        } else {
            $(".p" + i).addClass("odd");
        }
    }

}

/*function CheckNavLimits() {
 if(OrthoVariables.CurPage >= OrthoVariables.maxPages)
 DisableButtonLink("NextTest");
 else
 EnableButtonLink("NextTest");
 if(OrthoVariables.CurPage <= 1)
 DisableButtonLink("PreviousTest");
 else
 EnableButtonLink("PreviousTest");
 }*/

function IncreasePage() {
    if (OrthoVariables.CurPage < OrthoVariables.maxPages) {
        OrthoVariables.CurPage += 2;
        if (OrthoVariables.CurPage > OrthoVariables.maxPages)
            OrthoVariables.CurPage = OrthoVariables.maxPages;
        ShowPage();
    }
}

function DecreasePage() {

    if (OrthoVariables.CurPage > 1) {
        OrthoVariables.CurPage -= 2;
        ShowPage();
    }
}

function ShowPage() {
    // alert(CurPage);
    $('#lesson').turn('page', OrthoVariables.CurPage);
}

function DisableButtonLink(id) {
    $("#" + id).removeClass("more").addClass("disablemore");
    $("#" + id).unbind('click');
    if (id === "SubmitAnswer" && OrthoVariables.PageTracking[OrthoVariables.lessonPage] !== undefined) {
        OrthoVariables.PageTracking[OrthoVariables.lessonPage].submitbutton = false;
    }
    if (id === "NextTest") {
        $('#lesson').turn('disable', true);
    }
    switch (id) {
        case "NextTest":
            $("#" + id).stop(true,true).animate({right: "-130px"}, 500, 'linear');
            break;
        case "PreviousTest":
            $("#" + id).stop(true,true).animate({left: "-130px"}, 500, 'linear');
            break;
        case "SubmitAnswer":
            $("#" + id).stop(true,true).animate({top: "-50px"}, 500, 'linear');
            break;
    }
}

function IsEnabledButtonLink(id) {
    return ($("#" + id).hasClass("disablemore")) ? false : true;
}

function EnableButtonLink(id) {
    if ($("#" + id).hasClass("disablemore")) {
        $("#" + id).removeClass("disablemore").addClass("more");
        switch (id) {
            case "NextTest":
                $("#" + id).on("click", function () {
                    if ($("#NextTest").data("fire") === true) {
                        IncreasePage();
                    }
                });
                $('#lesson').turn('disable', OrthoVariables.disableturn);
                $("#" + id).stop(true,true).animate({right: "10px"}, 1000, 'easeOutElastic');
                break;
            case "PreviousTest":
                $("#" + id).on("click", function () {
                    if ($("#PreviousTest").data("fire") === true) {
                        DecreasePage();
                    }
                });
                $("#" + id).stop(true,true).animate({left: "10px"}, 1000, 'easeOutElastic');
                break;
            case "SubmitAnswer":
                $("#" + id).on("click", function () {
                    SubmitAnswer();
                });
                OrthoVariables.PageTracking[OrthoVariables.lessonPage].submitbutton = true;
                $("#" + id).stop(true,true).animate({top: "+=50px"}, 1000, 'easeOutElastic');
                break;
        }

    }


}

function CreatePages() {
    var pages = "";

    for (var i = 1; i < OrthoVariables.maxPages; i++) {
        pages += "<div id=\"Page" + i + "\"></div>";
    }

}

function newShowMsg(message, type) {
    var vicon, vtype, vhide;
    switch (type) {
        case "alert":
            vicon = "ui-icon-alert";
            vtype = "error";
            vhide = false;
            break;
        case "highlight":
            vicon = "ui-icon-flag";
            vtype = "success";
            vhide = true;
            break;
        case "info":
            vicon = "ui-icon-info";
            vtype = "info";
            vhide = false;
            break;
    }

    var stack_bottomright = {"dir1": "up", "dir2": "left", "firstpos1": 25, "firstpos2": 25};
    $.pnotify({
        title: 'Please Notice',
        text: message,
        icon: 'ui-icon ' + vicon,
        type: vtype,
        opacity: .8,
        addclass: "stack-bottomright",
        stack: stack_bottomright,
        hide: vhide

    });
}

// Message Functions
function ShowMsg(message, type) {
    var id = ++OrthoVariables.msg_curIndex;
    $("#msg_area").append($("#msgboxTemplate").render({
        "id": id,
        "type": type,
        "message": message
    }));
    // Clear the timeout or stop animation when mouse over notification
    $("#msgbox_" + id).mouseenter(function () {
        var myid = getID(this.id);
        if (OrthoVariables.msg_info[myid] != null) {
            clearTimeout(OrthoVariables.msg_info[myid]);
            OrthoVariables.msg_info[myid] = null;
            $(this).stop(false, false);
            $("#" + this.id + "> div > .ui-icon-closethick").fadeIn('fast');
            $(this).fadeTo('fast', 1);
        }
    });
    $("#msgbox_" + id + " span").click(function () {
        $(this).parent().parent().fadeOut("slow", function () {
            $(this).remove();
        });
    });

    $("#msgbox_" + id).slideDown("slow");

    var msgfadeout = setTimeout(function () {
        $("#msgbox_" + id).fadeOut(2000, "easeInQuart", function () {
            var myid = getID(this.id);
            OrthoVariables.msg_info[id] = null;
            $(this).remove();
        });
    }, 1000);
    OrthoVariables.msg_info[id] = msgfadeout;
}

// Quiz
function getIndex(strIndex) {
    return strIndex.substr(0, strIndex.indexOf("."));
}

function ToggleQuizSelection(element) {
    if (OrthoVariables.PageTracking[OrthoVariables.lessonPage].status === "correct") {
        ShowMsg("You already answer it!", "highlight");
        element.checked = !element.checked;
        return true;
    }
    if (OrthoVariables.PageTracking[OrthoVariables.lessonPage].status === "wrong" && OrthoVariables.LessonData.Page[OrthoVariables.lessonPage].attributes["Blocked"] === "no") {
        ShowMsg("You already answer it!", "highlight");
        element.checked = !element.checked;
        return true;
    }
    var id = getID(element.name);
    var myPage = id;
    var myindex = getIndex(element.name);
    OrthoVariables.lessonAnswers[myPage].quiz[myindex] = element.checked;
    if (element.checked) {
        $("[name='" + element.name + "']").parent().addClass("quizselected");
    } else {
        $("[name='" + element.name + "']").parent().removeClass("quizselected");
    }


    var todisable = true;
    for (var i = 0; i < OrthoVariables.lessonAnswers[myPage].quiz.length; i++) {
        if (OrthoVariables.lessonAnswers[myPage].quiz[i]) {
            todisable = false;
            break;
        }
    }
    if (todisable) {
        DisableButtonLink("SubmitAnswer");
    }
    else if (!IsEnabledButtonLink("SubmitAnswer")) {
        EnableButtonLink("SubmitAnswer");
    }


}


function ToggleText(element) {
    //var elemen
    //ToggleQuizSelection();
    if (OrthoVariables.PageTracking[OrthoVariables.lessonPage].status === "correct") {
        ShowMsg("You already answer it!", "highlight");
        return true;
    }
    if (OrthoVariables.PageTracking[OrthoVariables.lessonPage].status === "wrong" && OrthoVariables.LessonData.Page[OrthoVariables.lessonPage].attributes["Blocked"] === "no") {
        ShowMsg("You already answer it!", "highlight");
        return true;
    }
    var inputelement = $(element).parent().children("input")[0];
    inputelement.checked = !inputelement.checked;
    ToggleQuizSelection(inputelement);
    return false;
}

function SubmitAnswer() {
    var type = GetTypeofPage(OrthoVariables.lessonPage);
    var mypage = OrthoVariables.LessonData.Page[OrthoVariables.lessonPage];
    var subid = (mypage.Widget[0].type === "image") ? 0 : 1;
    var id = OrthoVariables.lessonPage.toString();
    $("#pointer_" + id).removeClass();

    if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"]) {
        TogglePaint("line", id);
        ShowOffImage(id, "line");
    }
    if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["h"]) {
        TogglePaint("hotspot", id);
        ShowOffImage(id, "hotspot");
    }
    if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["t"]) {
        TogglePaint("target", id);
        ShowOffImage(id, "target");
    }

    switch (type) {
        case "quiz":
            $.getJSON(OrthoVariables.JsonUrl, GetQuizQuestion(), function (data) {
                ApplyQuizResult(data);
                updateCounter();
                checkFinal(data);
            });
            break;
        case "hotspots":
            $.getJSON(OrthoVariables.JsonUrl, GetHotspotQuestion(), function (data) {
                ApplyHotspotResult(data);
                updateCounter();
                checkFinal(data);
            });
            break;
        case "input":
            $.getJSON(OrthoVariables.JsonUrl, GetInputQuestion(), function (data) {
                applyInputResult(data);
                updateCounter();
                checkFinal(data);
            });
            break;
    }
    $("#overlay").removeClass("overlay_hidden").addClass("waiting");
}

function checkFinal(data) {
    if (data.final === "true") {
        lessonFinalized(false)
    }
}


function GetQuizQuestion() {
    var Question = new Object();
    Question.action = 2;
    Question.orthoeman_id = OrthoVariables.InitialQueryString.id;
    Question.Page = OrthoVariables.lessonPage;
    Question.name = OrthoVariables.InitialQueryString["name"];
    Question.type = "quiz";
    var answer = "";
    for (var i = 0; i < OrthoVariables.lessonAnswers[Question.Page].quiz.length; i++) {
        if (OrthoVariables.lessonAnswers[Question.Page].quiz[i]) {
            answer += ";" + i;
        }
    }
    Question.answer = answer;
    return Question;
}
function valid_num(evt) {
    var charCode = (evt.which) ? evt.which : event.keyCode;
    return (charCode > 31 && (charCode < 48 || charCode > 57)) ? false : true;
}

function GetInputQuestion() {
    var Question = new Object();
    Question.name = OrthoVariables.InitialQueryString["name"];
    Question.orthoeman_id = OrthoVariables.InitialQueryString.id;
    Question.action = 2;
    Question.Page = OrthoVariables.lessonPage;
    Question.type = "input";
    Question.value = OrthoVariables.lessonAnswers[Question.Page].input.value;
    return Question;
}

function GetHotspotQuestion() {
    var Question = new Object();
    Question.name = OrthoVariables.InitialQueryString["name"];
    Question.orthoeman_id = OrthoVariables.InitialQueryString.id;
    Question.action = 2;
    Question.Page = OrthoVariables.lessonPage;
    Question.type = "hotspots";
    var answer = [];
    var counter = 0;
    var hotspots = OrthoVariables.lessonAnswers[Question.Page].hotspots;
    for (var i = 0; i < hotspots.length; i++) {
        if (hotspots[i] !== undefined) {
            answer[counter] = [hotspots[i][0], hotspots[i][1]];
            counter++;
        }
    }
    Question.answer = answer;
    return Question;
}


function ReachMaxNumberHotSpots(pageid) {

    var counter = 0;
    var hotspots = OrthoVariables.lessonAnswers[pageid].hotspots;
    if (hotspots == 0) {
        return false;
    }
    for (var i = 0; i < hotspots.length; i++) {
        if (hotspots[i] !== undefined) {
            counter++;
        }
    }
    return  (counter >= OrthoVariables.MaxHotSpots[pageid]) ? true : false;
}

function CheckReadyNextText(answer, blocked) {
    if (blocked === "yes" && answer === "wrong") {
        DisableButtonLink("NextTest");
    } else {
        EnableButtonLink("NextTest");
        DisableButtonLink("SubmitAnswer");
    }
}

function GetTypeofPage(PageID) {
    var type = undefined;
    var mypage = OrthoVariables.LessonData.Page[PageID];
    if (mypage !== undefined) {

        if (mypage.Widget[0].type === "quiz" || mypage.Widget[1].type === "quiz") {
            type = "quiz"
        } else if (mypage.Widget[0].type === "input" || mypage.Widget[1].type === "input") {
            type = "input";
        } else if (mypage.Widget[0].type === "image") {
            if (mypage.Widget[0].Image.HotSpots === "yes") {
                type = "hotspots"
            }
        } else if (mypage.Widget[1].type === "image") {
            if (mypage.Widget[1].Image.HotSpots === "yes") {
                type = "hotspots"
            }
        }

    }
    return type;
}


function applyQuizUserResponse(data, lessonPage) {
    var ca = data.split(";");

    var id = "";
    var widgets = OrthoVariables.LessonData.Page[lessonPage].Widget;
    for (var i = 0; i < widgets.length; i++) {
        if (widgets[i].type === "quiz") {
            id = widgets[i].Quiz.id;
        }
    }

    for (var i = 0; i < ca.length; i++) {
        if (ca[i].length > 0) {
            OrthoVariables.lessonAnswers[lessonPage].quiz[ca[i]] = true;
            var name = ca[i] + ".answer_" + id;
            $("[name='" + name + "']").prop('checked', true);
            $("[name='" + name + "']").parent().addClass("quizselected");
        }

    }


}

function ApplyQuizResult(data, lessonPage, showMsg) {
    lessonPage = (typeof lessonPage === "undefined") ? OrthoVariables.lessonPage : lessonPage;
    showMsg = (typeof showMsg === "undefined") ? true : false;
    var page = 2 * lessonPage + 2;
    var myanswer = "wrong";
    var blocked = OrthoVariables.LessonData.Page[lessonPage].attributes["Blocked"]
    if (data.Answer === "correct") {
        myanswer = "correct";
        applyCorrectCue(page, undefined, showMsg);
    } else {
        myanswer = "wrong";
        applyWrongCue(page, undefined, showMsg);
    }
    var length = data.PaintShapes.length;
    if (length > 0) {
        var mypage = OrthoVariables.LessonData.Page[lessonPage];
        var subid = (mypage.Widget[0].type === "image") ? 0 : 1;
        var id = lessonPage.toString() //+ subid.toString();
        var mystage = OrthoVariables.origCanvas[id][2];
        var myshapelayer = mystage.get(".answerlayer")[0];
        myshapelayer.removeChildren();
        var fillcolor = (myanswer === "correct") ? OrthoVariables.ColorRight : OrthoVariables.ColorWrong;

        var strikecolor = (myanswer === "correct") ? OrthoVariables.ColorRightEdge : OrthoVariables.ColorWrongEdge;
        for (var i = 0; i < length; i++) {
            switch (data.PaintShapes[i][0]) {
                case "Circle":
                    myshapelayer.add(PaintCircle(data.PaintShapes[i], fillcolor, strikecolor));
                    break;
                case "Rect":
                    myshapelayer.add(PaintRect(data.PaintShapes[i], fillcolor, strikecolor));
                    break;
                case "Polygon":
                    myshapelayer.add(PaintPolygon(data.PaintShapes[i], fillcolor, strikecolor));
                    break;
                case 'Eclipse':
                    myshapelayer.add(PaintEclipse(data.PaintShapes[i], fillcolor, strikecolor));
                    break;
            }
        }
        //myshapelayer.draw();
        var childrens = myshapelayer.getChildren();
        for (var i = 0; i < childrens.length; i++) {
            var tween = new Kinetic.Tween({
                node: childrens[i],
                opacity: 0.5,
                duration: 2,
                easing: Kinetic.Easings.EaseOut
            });
            tween.play();
        }

        myshapelayer.draw();
    }

    if (data.CorrectAnswer !== "") {
        var ca = data.CorrectAnswer.split(";");

        if (data.Answer === "wrong") {
            ApplyQuizColors(ca, lessonPage);
        }
        for (var i = 1; i < ca.length; i++) {
            ApplyQuizColor(ca[i], lessonPage)
        }

    }

    CheckReadyNextText(myanswer, blocked);
    PageTracking(myanswer, blocked, lessonPage);
    RemoveOverlay();
}

function ApplyQuizColor(index, lessonPage) {
    lessonPage = (typeof lessonPage === "undefined") ? OrthoVariables.lessonPage : lessonPage;
    var id = "";
    var widgets = OrthoVariables.LessonData.Page[lessonPage].Widget;
    for (var i = 0; i < widgets.length; i++) {
        if (widgets[i].type === "quiz") {
            id = widgets[i].Quiz.id;
        }
    }
    id = index + ".answer_" + id;
    var inputlelem = $("[name='" + id + "']");
    inputlelem.parent().css("text-shadow", "1px 1px 0 black").stop(false, true).animate({backgroundColor: OrthoVariables.ColorRight, color: "white"}, 2000);
}

function ApplyQuizColors(ca, lessonPage) {
    lessonPage = (typeof lessonPage === "undefined") ? OrthoVariables.lessonPage : lessonPage;
    var id = "";
    var widgets = OrthoVariables.LessonData.Page[lessonPage].Widget;
    for (var i = 0; i < widgets.length; i++) {
        if (widgets[i].type === "quiz") {
            id = widgets[i].Quiz.id;
        }
    }

    for (var i = 0; i < OrthoVariables.lessonAnswers[lessonPage].quiz.length; i++) {
        if (!Iscontains(i, ca) === true && OrthoVariables.lessonAnswers[lessonPage].quiz[i] === true) {
            var input = $("[name='" + i + ".answer_" + id + "']");
            input.parent().removeClass("quizselected").css("text-shadow", "1px 1px 0 black").animate({backgroundColor: OrthoVariables.ColorWrong, color: "white"}, 2000);
        }

    }
}

function Iscontains(value, tables) {

    var contains = false;
    for (var j = 0; j < tables.length; j++) {
        if (tables[j] === value) {
            contains = true;
            break;
        }
    }
    return contains;

}


function applyInputUserResponse(data, lessonPage) {
    OrthoVariables.spinControls[lessonPage].SetCurrentValue(data);
}

function applyInputResult(data, lessonPage, showMsg) {
    lessonPage = (typeof lessonPage === "undefined") ? OrthoVariables.lessonPage : lessonPage;
    showMsg = (typeof showMsg === "undefined") ? true : false;
    var page = 2 * lessonPage + 2;
    var myanswer = "wrong";
    var blocked = OrthoVariables.LessonData.Page[lessonPage].attributes["Blocked"];
    if (data.Answer === "correct") {
        myanswer = "correct";
        applyCorrectCue(page, undefined, showMsg);
    }
    else {
        myanswer = "wrong";
        applyWrongCue(page, undefined, showMsg);
        showCorrectInputResult(data.CorrectAnswer, getSpinID(lessonPage));
    }
    PageTracking(myanswer, blocked, lessonPage);
    CheckReadyNextText(myanswer, blocked);
    RemoveOverlay();
    if (!(blocked === "yes" && answer === "wrong")) {
        OrthoVariables.spinControls[lessonPage].SetDisabled(true);
    }
}

function getSpinID(Page) {
    var id = -1;
    for (var wid in OrthoVariables.LessonData.Page[Page].Widget) {
        if (OrthoVariables.LessonData.Page[Page].Widget[wid].type === "input") {
            id = OrthoVariables.LessonData.Page[Page].Widget[wid].Input.id;
        }
    }
    return id
}

function showCorrectInputResult(correctValue, id) {
    $("#correctRangeValue_" + id + ">span").html(correctValue);
    $("#correctRangeValue_" + id).fadeIn('slow');
}

function applyCorrectCue(id, msg, showMsg) {
    showMsg = (typeof showMsg === "undefined") ? true : showMsg;
    if (showMsg) {
        msg = msg || "Your Answer is Correct!";
        ShowMsg(msg, "highlight");
    }
    $("#Page" + id).css("background", "url('img/bg_correct.png')").css('text-shadow', 'none');
}

function applyWrongCue(id, msg, showMsg) {
    showMsg = (typeof showMsg === "undefined") ? true : showMsg;
    if (showMsg) {
        msg = msg || "Your Answer is Wrong!";
        ShowMsg(msg, "alert");
    }
    $("#Page" + id).css("background", "url('img/bg_wrong.png')").css('text-shadow', 'none');


}


function applyHotspotUserResponse(data, lessonPage) {
    $.each(data, function () {
        drawCircleHotSpot(lessonPage, this[0], this[1], "yes", lessonPage, true);
    });
}

function ApplyHotspotResult(data, lessonPage, showMsg) {
    lessonPage = (typeof lessonPage === "undefined") ? OrthoVariables.lessonPage : lessonPage;
    showMsg = (typeof showMsg === "undefined") ? true : false;
    var page = 2 * lessonPage + 2;
    var myanswer = "wrong";
    var blocked = OrthoVariables.LessonData.Page[lessonPage].attributes["Blocked"]
    if (data.Answer === "correct") {
        myanswer = "correct";
        applyCorrectCue(page, undefined, showMsg);
    }
    else {
        myanswer = "wrong";
        applyWrongCue(page, undefined, showMsg);
    }
    var length = data.PaintShapes.length;
    //var fillcolor = (myanswer==="correct") ? OrthoVariables.ColorRight : OrthoVariables.ColorWrong;
    var fillcolor = function (fillanswer) {
        return   (fillanswer) ? OrthoVariables.ColorRight : OrthoVariables.ColorWrong;
    }
    var strikecolor = (myanswer === "correct") ? OrthoVariables.ColorRightEdge : OrthoVariables.ColorWrongEdge;
    if (length > 0) {
        var mypage = OrthoVariables.LessonData.Page[lessonPage];
        var subid = (mypage.Widget[0].type === "image") ? 0 : 1;
        var id = lessonPage.toString();
        var mystage = OrthoVariables.origCanvas[id][2];
        var myshapelayer = mystage.get(".answerlayer")[0];
        myshapelayer.removeChildren();
        for (var i = 0; i < length; i++) {
            switch (data.PaintShapes[i][0]) {
                case "Circle":
                    myshapelayer.add(PaintCircle(data.PaintShapes[i], fillcolor(data.Fill[i]), strikecolor));
                    break;
                case "Rect":
                    myshapelayer.add(PaintRect(data.PaintShapes[i], fillcolor(data.Fill[i]), strikecolor));
                    break;
                case "Polygon":
                    myshapelayer.add(PaintPolygon(data.PaintShapes[i], fillcolor(data.Fill[i]), strikecolor));
                    break;
                case 'Eclipse':
                    myshapelayer.add(PaintEclipse(data.PaintShapes[i], fillcolor(data.Fill[i]), strikecolor));
                    break;
            }
        }
        //myshapelayer.draw();
        var childrens = myshapelayer.getChildren();
        for (var i = 0; i < childrens.length; i++) {
            var tween = new Kinetic.Tween({
                node: childrens[i],
                opacity: 0.5,
                duration: 2,
                easing: Kinetic.Easings.EaseOut
            });
            tween.play();
        }
        myshapelayer.draw();
    }
    PageTracking(myanswer, blocked, lessonPage);
    if (lessonPage === OrthoVariables.lessonPage) {
        CheckReadyNextText(myanswer, blocked);
    }

    RemoveOverlay();
}

function PageTracking(answer, blocked, lessonPage) {
    lessonPage = (typeof lessonPage === "undefined") ? OrthoVariables.lessonPage : lessonPage;
    if (answer === "correct") {
        OrthoVariables.PageTracking[lessonPage].status = "correct";
        OrthoVariables.PageTracking[lessonPage].nextpass = true;
        OrthoVariables.PageTracking[lessonPage].grade = getNormalizeGrade(parseInt(OrthoVariables.LessonData.Page[lessonPage].attributes.Grade));
    } else {
        OrthoVariables.PageTracking[lessonPage].status = "wrong";
        OrthoVariables.PageTracking[lessonPage].grade = getNormalizeGrade(-parseInt(OrthoVariables.LessonData.Page[lessonPage].attributes.negativeGrade));
        OrthoVariables.PageTracking[lessonPage].nextpass = (blocked !== "yes");
    }
}


function getNormalizeGrade(Grade) {
    //calculate totalSum
    var totalSum = 0;
    for (var i = 0; i < OrthoVariables.LessonData.Page.length; i++) {
        if (!OrthoVariables.PageTracking[i].theory) {
            totalSum += parseFloat(OrthoVariables.LessonData.Page[i].attributes.Grade);
        }
    }
    return Math.round(10 * (100 / totalSum) * Grade) / 10;
}

function RemoveOverlay() {
    if (OrthoVariables.isLessonDisable === false && OrthoVariables.isLessonComplete === false) {
        $("#overlay").removeClass("waiting").addClass("overlay_hidden");
    }

}

function PaintCircle(data, fillcolor, strokecolor) {
    var circle = new Kinetic.Circle({
        x: data[1]["X"],
        y: data[1]["Y"],
        radius: parseInt(data[2]),
        fill: fillcolor,
        stroke: strokecolor,
        strokeWidth: 1,
        opacity: 0
    });
    return circle;
}


function oldPaintEclipse(data, fillcolor, strokecolor) {  // for kinetic 3.9.4
    var radx = data[2]["RadiusX"];
    var rady = data[2]["RadiusY"];
    var scalex = 0, scaley = 0, radius = 0;
    if (radx >= rady) {
        radius = radx;
        scalex = 1.0;
        scaley = rady / radx;
    } else {
        radius = rady;
        scaley = 1.0;
        scalex = radx / rady;
    }
    var eclipse = new Kinetic.Circle({
        x: data[1]["X"],
        y: data[1]["Y"],
        radius: radius,
        fill: fillcolor,
        stroke: strokecolor,
        strokeWidth: 1,
        opacity: 0
    });
    eclipse.setScale(scalex, scaley);
    return eclipse;
}

function PaintEclipse(data, fillcolor, strokecolor) {
    var eclipse = new Kinetic.Ellipse({
        x: data[1]["X"],
        y: data[1]["Y"],
        radius: {x: data[2]["RadiusX"], y: data[2]["RadiusY"]},
        fill: fillcolor,
        stroke: strokecolor,
        strokeWidth: 1,
        opacity: 0
    });
    return eclipse;
}

function PaintRect(data, fillcolor, strokecolor) {
    var rect = new Kinetic.Rect({
        x: data[1]["X"],
        y: data[1]["Y"],
        width: data[2],
        height: data[3],
        fill: fillcolor,
        stroke: strokecolor,
        strokeWidth: 1,
        opacity: 0
    });
    return rect;
}

function PaintPolygon(data, fillcolor, strokecolor) {
    var points = [];
    var len = (data.length - 1);
    for (var i = 0; i < len; i++) {
        points[i] = {
            x: parseInt(data[i + 1]["X"]),
            y: parseInt(data[i + 1]["Y"])
        };
    }
    var poly = new Kinetic.Polygon({
        points: points,
        fill: fillcolor,
        stroke: strokecolor,
        strokeWidth: 1,
        opacity: 0
    });
    return poly;
}

