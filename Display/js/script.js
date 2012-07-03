/* Author: Konstantinos Zagoris
 The script logic for ORTHO e-Man
 */
"use strict";

var OrthoVariables = {
    maxPages:5,
    CurPage:1,
    HeightFromBottom:120, //$('#navigation').height() - $('footer').height();
    origCanvas:[], // is array with [0] original image, [1] imageurl, [2] stage [3] brightness [4] contrast, [5] is invert
   // scale:1, //official scale
    scalePage:[], //scale for each page. Do not know if it is needed.
    JsonUrl:"sslayer.php",
    LessonData:"",
    buttonState:[],
    line:{
        "pressed":false,
        startx:-1,
        starty:-1,
        "prevline":null
    },
    msg_info:[],
    msg_curIndex:0,
    linemindistance:10,
    clickcatch:false,
    lessonAnswers:[],
    lessonPage:-1,
    InitialQueryString:[],
    lessonLoaded:[],
    MaxHotSpots:[],
    PageTracking:[],
    ColorRight:"#047816",
    ColorRightEdge:"#285935",
    ColorWrong:"#9C2100",
    ColorWrongEdge:"#592835"

};


$(document).ready(function () {
    $("#NextTest").data("fire", true);
    $.prettyLoader({
        animation_speed:'normal',
        bind_to_ajax:true,
        delay:false,
        loader:'img/ajax-loader.gif'
    });
    document.onselectstart = function () {
        return false;
    };
    OrthoVariables.InitialQueryString = getUrlVars();
    $.getJSON(OrthoVariables.JsonUrl, {
        "action":1, "name":OrthoVariables.InitialQueryString["name"]

    }, function (data) {
        OrthoVariables.LessonData = data;
        OrthoVariables.maxPages = 2 * (OrthoVariables.LessonData.Page.length + 1);
        $("#lesson").html($("#LessonTemplate").render(OrthoVariables.LessonData));
        //initialize the buttonstates
        for (var i = 0; i < OrthoVariables.LessonData.Page.length; i++) {
            OrthoVariables.buttonState[i] = {
                "b":false,
                "c":false,
                "l":false
            };
            OrthoVariables.scalePage[i] = 1;
        }
        displayFunctions();
        LoadImages("0");

        ApplyRoundtoPages(1, 3);
        //DisableButtonLink("SubmitAnswer");

        EnableButtonLink("NextTest");
    })
});


function getUrlVars() {
    var vars = [], hash;
    var hashes = window.location.href.replace("#", "").slice(window.location.href.indexOf('?') + 1).split('&');
    for (var i = 0; i < hashes.length; i++) {
        hash = hashes[i].split('=');
        if (hash[0] === "name") {
            vars.push(hash[0]);
            vars[hash[0]] = hash[1];
        }
    }
    return vars;
}

function LoadImages(Page) {

    // Loading the Image to Canvas
    var imagesToLoad = [];
    var counter = 0;
    for (var i in OrthoVariables.LessonData.Images) {
        if (OrthoVariables.LessonData.Images[i].id[0] === Page && OrthoVariables.lessonLoaded[parseInt(Page)] === undefined) {
            imagesToLoad[counter] = OrthoVariables.LessonData.Images[i];
            counter++;
        }
    }
    for (var i = 0; i < imagesToLoad.length; i++) {
        var c = $('#canvasid_' + imagesToLoad[i].id).get(0)
        c.getContext("2d").zag_LoadImage(imagesToLoad[i].url);
        var orig = document.createElement('canvas');
        orig.width = c.width
        orig.height = c.height;
        orig.getContext("2d").zag_LoadImage(imagesToLoad[i].url);

        //orig.getContext("2d").drawImage(c, 0 , 0);

        OrthoVariables.origCanvas[imagesToLoad[i].id] = [orig, imagesToLoad[i].url, 0, 0, false];
        OrthoVariables.MaxHotSpots[imagesToLoad[i].id[0]] = imagesToLoad[i].MaxSpots;
        //sliders
        $('#slider_b_' + imagesToLoad[i].id).slider({
            range:"max",
            min:-100,
            max:100,
            value:0,
            slide:function (event, ui) {
                var id = this.id.substr(this.id.lastIndexOf("_") + 1, this.id.length);
                var value = ui.value / 100;
                OrthoVariables.origCanvas[id][3] = value;
                //Brightness(id, value, OrthoVariables.origCanvas[id][0].zag_Clone());
                ApplyImageOperations(id);
            }
        });
        $('#slider_c_' + imagesToLoad[i].id).slider({
            range:"max",
            min:-100,
            max:100,
            value:0,
            slide:function (event, ui) {
                var id = getID(this.id);
                var value = ui.value / 100;
                OrthoVariables.origCanvas[id][4] = value;
                //Contrast(id, value, OrthoVariables.origCanvas[id][0].zag_Clone());
                ApplyImageOperations(id);
            }
        });
        //for shapes
        var stage = new Kinetic.Stage({
            container:"container_" + imagesToLoad[i].id,
            width:c.width,
            height:c.height,
            listen:true
        });

        var shapelayer = new Kinetic.Layer({
            id:"shapelayer"
        });
        var answerlayer = new Kinetic.Layer({
            id:"answerlayer"
        });

        var tooltiplayer = new Kinetic.Layer({
            id:"tooltiplayer",
            throttle:20

        });
        var tooltip = new Kinetic.Text({
            text:"",
            textFill:"white",
            fontFamily:"Georgia",
            fontSize:8,
            verticalAlign:"bottom",
            padding:4,
            fill:"black",
            visible:false,
            alpha:0.75,
            id:"tooltip"
        });
        tooltiplayer.add(tooltip);
        stage.add(shapelayer);
        stage.add(tooltiplayer);
        stage.add(answerlayer);
        OrthoVariables.origCanvas[imagesToLoad[i].id][2] = stage;

        $("#container_" + imagesToLoad[i].id).click({
            "pos":imagesToLoad[i].HotSpots
        }, function (event) {
            var id = getID(this.id);
            //console.log(id);
            var mystage = OrthoVariables.origCanvas[id][2];
            var mousepos = mystage.getMousePosition();
            if (mousepos !== undefined) {

                var myshapelayer = mystage.get("#shapelayer")[0];
                var mytooltip = mystage.get("#tooltiplayer")[0].get("#tooltip")[0];
                var ishotspots = event.data.pos;
                if (!OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"] && ishotspots === "yes") {
                    if (OrthoVariables.PageTracking[OrthoVariables.lessonPage].status === "correct") {
                        ShowMsg("You already answer it!", "highlight");
                        return true;
                    }
                    if (OrthoVariables.PageTracking[OrthoVariables.lessonPage].status === "wrong" && OrthoVariables.LessonData.Page[OrthoVariables.lessonPage].attributes["Blocked"] === "no") {
                        ShowMsg("You already answer it!", "highlight");
                        return true;
                    }
                    var shapes = mystage.getIntersections({
                        x:mousepos.x / OrthoVariables.scalePage[OrthoVariables.lessonPage],
                        y:mousepos.y / OrthoVariables.scalePage[OrthoVariables.lessonPage]
                    })
                    if (shapes.length == 0) {
                        var circle = new Kinetic.Circle({
                            x:mousepos.x / OrthoVariables.scalePage[OrthoVariables.lessonPage],
                            y:mousepos.y / OrthoVariables.scalePage[OrthoVariables.lessonPage],
                            radius:6/OrthoVariables.scalePage[OrthoVariables.lessonPage],
                            fill:"#cb842e",
                            stroke:"#cbb48f",
                            strokeWidth:1,
                            alpha:0.5,
                            id:"circle_" + id
                        });

                        circle.on("mouseover", function () {
                            $("#pointer_" + id).removeClass().addClass("erasercursor");
                            var mousePos = mystage.getMousePosition();
                            var x = mousePos.x / OrthoVariables.scalePage[OrthoVariables.lessonPage] + 5;
                            var y = mousePos.y / OrthoVariables.scalePage[OrthoVariables.lessonPage] + 10;
                            drawTooltip(mytooltip, x, y, "click to remove");
                            this.transitionTo({
                                scale:{
                                    x:1.7,
                                    y:1.7
                                },
                                duration:0.3,
                                easing:'ease-out'
                            });
                        });
                        circle.on("mouseout", function () {
                            SetCursor(id);
                            mytooltip.hide();
                            mytooltip.getLayer().draw();
                            this.transitionTo({
                                scale:{
                                    x:1,
                                    y:1
                                },

                                duration:0.3,
                                easing:'ease-in'
                            });
                        });
                        circle.on("click", function () {
                            if (OrthoVariables.PageTracking[OrthoVariables.lessonPage].status === "correct" ||
                                (OrthoVariables.PageTracking[OrthoVariables.lessonPage].status === "wrong" && OrthoVariables.LessonData.Page[OrthoVariables.lessonPage].attributes["Blocked"] === "no" )) {
                                return true;
                            }
                            OrthoVariables.clickcatch = true;
                            myshapelayer.remove(this);
                            OrthoVariables.lessonAnswers[OrthoVariables.lessonPage].hotspots[circle._id] = undefined;
                            var mytooltip = mystage.get("#tooltiplayer")[0].get("#tooltip")[0];
                            mytooltip.hide();
                            mystage.draw();
                            SetCursor(id);
                            DisableButtonLink("SubmitAnswer");
                        });
                        if (!OrthoVariables.clickcatch && !ReachMaxNumberHotSpots(OrthoVariables.lessonPage)) {
                            myshapelayer.add(circle);
                            OrthoVariables.lessonAnswers[OrthoVariables.lessonPage].hotspots[circle._id] = [Math.round(mousepos.x / OrthoVariables.scalePage[OrthoVariables.lessonPage]), Math.round(mousepos.y / OrthoVariables.scalePage[OrthoVariables.lessonPage])];
                            myshapelayer.draw();
                            $("#pointer_" + id).removeClass().addClass("erasercursor");
                            if (ReachMaxNumberHotSpots(OrthoVariables.lessonPage)) {
                                EnableButtonLink("SubmitAnswer");
                            }
                        } else if (ReachMaxNumberHotSpots(OrthoVariables.lessonPage)) {
                            ShowMsg("Reach maximum points. Please remove the previous to add new ones.", "highlight");
                        }
                        OrthoVariables.clickcatch = false;
                    } else {

                    }

                }

            }

        });
        $("#container_" + imagesToLoad[i].id).mousedown(function () {
            if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"]) {
                var id = getID(this.id);
                var mystage = OrthoVariables.origCanvas[id][2];
                var mousepos = mystage.getMousePosition();
                if (mousepos !== undefined) {
                    OrthoVariables.line.pressed = true;
                    OrthoVariables.line.startx = mousepos.x / OrthoVariables.scalePage[OrthoVariables.lessonPage];
                    OrthoVariables.line.starty = mousepos.y / OrthoVariables.scalePage[OrthoVariables.lessonPage];
                }
            }
        });

        $("#container_" + imagesToLoad[i].id).mousemove(function () {
            if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"] && OrthoVariables.line.pressed) {
                DrawShape(this.id);
            }
        });

        $("#container_" + imagesToLoad[i].id).mouseup(function () {
            if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"] && OrthoVariables.line.pressed) {
                DrawShape(this.id);
                SetonLine(this.id);
                CheckShape(this.id);
                OrthoVariables.line.pressed = false;
                OrthoVariables.line.startx = -1;
                OrthoVariables.line.starty = -1;
                OrthoVariables.line.prevline = null;
            }

        });

        $("#container_" + imagesToLoad[i].id).mouseout(function () {
            if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"] && OrthoVariables.line.pressed) {
                OrthoVariables.line.pressed = false;
                OrthoVariables.line.startx = -1;
                OrthoVariables.line.starty = -1;
                OrthoVariables.line.prevline = null;
            }

        });

    }
    //console.log(counter);
    OrthoVariables.lessonLoaded[parseInt(Page)] = true;
    if (counter > 0) CheckResizeLimits(Page);
}

function drawTooltip(tooltip, x, y, text) {
    tooltip.setText(text);
    /*var maxRight = 530;
     if(x > maxRight) {
     x = maxRight;
     } */
    tooltip.setPosition(x, y + 10);
    tooltip.show();
    //console.log(tooltip);
    tooltip.setScale(1 / OrthoVariables.scalePage[OrthoVariables.lessonPage], 1 / OrthoVariables.scalePage[OrthoVariables.lessonPage]);
    tooltip.getLayer().draw();
}

function CheckShape($strID) {
    var id = getID($strID);
    var mystage = OrthoVariables.origCanvas[id][2];
    var mousepos = mystage.getMousePosition();
    if (mousepos !== undefined) {
        var distance = Distance(OrthoVariables.line.startx, OrthoVariables.line.starty, mousepos.x / OrthoVariables.scalePage[OrthoVariables.lessonPage], mousepos.y / OrthoVariables.scalePage[OrthoVariables.lessonPage]);
        if (OrthoVariables.line.prevline != null && distance < OrthoVariables.linemindistance) {
            var myshapelayer = mystage.get("#shapelayer")[0];
            myshapelayer.remove(OrthoVariables.line.prevline);
            myshapelayer.draw();
        }
    }
}

function DrawShape($strID) {
    var id = getID($strID);
    var mystage = OrthoVariables.origCanvas[id][2];
    var mousepos = mystage.getMousePosition();
    if (mousepos !== undefined) {
        var myshapelayer = mystage.get("#shapelayer")[0];
        if (OrthoVariables.line.prevline != null) {
            myshapelayer.remove(OrthoVariables.line.prevline);
        }
//        var line = new Kinetic.Line({
//            points:[
//                {
//                    x:OrthoVariables.line.startx,
//                    y:OrthoVariables.line.starty
//                },
//                {
//                    x:mousepos.x/OrthoVariables.scale,
//                    y:mousepos.y/OrthoVariables.scale
//                }
//            ],
//            stroke:"orange",
//            strokeWidth:2,
//            lineCap:'round',
//            lineJoin:'round',
//            detectionType:"pixel"
//        });
        var linepoints = [];
        linepoints[0] = {x:OrthoVariables.line.startx, y:OrthoVariables.line.starty };
        linepoints[1] = {x:mousepos.x / OrthoVariables.scalePage[OrthoVariables.lessonPage], y:mousepos.y / OrthoVariables.scalePage[OrthoVariables.lessonPage]};
        linepoints[2] = {x:mousepos.x / OrthoVariables.scalePage[OrthoVariables.lessonPage] + 1, y:mousepos.y / OrthoVariables.scalePage[OrthoVariables.lessonPage] + 1};
        linepoints[3] = {x:OrthoVariables.line.startx + 1, y:OrthoVariables.line.starty + 1};
        var line = new Kinetic.Polygon({
            points:linepoints,
            fill:"orange",
            stroke:"orange",
            strokeWidth:1
        });
        //var mytooltip = mystage.get("#tooltiplayer")[0].get("#tooltip")[0];

        myshapelayer.add(line);
        OrthoVariables.line.prevline = line;
        //line.saveData();

        myshapelayer.draw();

    }
}

function Distance(x1, y1, x2, y2) {
    return Math.abs(x2 - x1) + Math.abs(y2 - y1);
}

function SetonLine(strID) {
    var id = getID(strID);
    var line = OrthoVariables.line.prevline;
    line.on("mouseover", function () {
        $("#pointer_" + id).removeClass().addClass("erasercursor");
        var mystage = this.getStage();
        var mytooltip = mystage.get("#tooltiplayer")[0].get("#tooltip")[0];
        var mousepos = mystage.getMousePosition();
        var x = mousepos.x / OrthoVariables.scalePage[OrthoVariables.lessonPage] + 15;
        var y = mousepos.y / OrthoVariables.scalePage[OrthoVariables.lessonPage] + 10;
        drawTooltip(mytooltip, x, y, "click to remove");
    });
    line.on("mouseout", function () {
        //$("#pointer_"+id).removeClass().addClass("pencilcursor");
        SetCursor(id);
        var mystage = this.getStage();
        var mytooltip = mystage.get("#tooltiplayer")[0].get("#tooltip")[0];
        mytooltip.hide();
        mytooltip.getLayer().draw();
    });

    line.on("click", function () {
        OrthoVariables.clickcatch = true;
        //$("#pointer_"+id).removeClass().addClass("pencilcursor");
        SetCursor(id);
        var mylayer = this.getLayer();
        mylayer.remove(line);
        mylayer.draw();
        var mystage = this.getStage();
        var mytooltip = mystage.get("#tooltiplayer")[0].get("#tooltip")[0];
        mytooltip.hide();
        mytooltip.getLayer().draw();
    });
}

function SetCursor(id) {
    $("#pointer_" + id).removeClass();
    if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"]) {
        $("#pointer_" + id).addClass("pencilcursor");
    } else {
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
    $('#lesson').turn({duration:600});
    $('#lesson').turn('size', $('#content_wrap').width(), $(window).height() - OrthoVariables.HeightFromBottom);
    //$('#lesson').turn('disable', true);
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
            h = (h< 400) ? 400 : h;
            var w = $('#content_wrap').width();
            w = (w< 500) ? 500 : w;
            CheckResizeLimits();
            $('#lesson').turn('size', w, h);
        });

    $('#lesson').bind('turning', function (e, page, pageObj) {
        $("#NextTest").data("fire", false);
        $("#PreviousTest").data("fire", false);
    });
    $('#lesson').bind('turned', function (e, page, pageObj) {
        $("#NextTest").data("fire", true);
        $("#PreviousTest").data("fire", true);

        OrthoVariables.CurPage = page % 2 == 0 && page != 1 ? page + 1 : page;
        var lessonpage = (OrthoVariables.CurPage === 0 || OrthoVariables.CurPage >= OrthoVariables.maxPages) ? -1 : ( Math.floor(OrthoVariables.CurPage / 2)) - 1;
        if (OrthoVariables.lessonAnswers[lessonpage] === undefined) {
            OrthoVariables.lessonAnswers[lessonpage] = {
                quiz:[],
                hotspots:[]
            }
        }
        ;
        OrthoVariables.lessonPage = lessonpage;
        ApplyRoundtoPages(OrthoVariables.CurPage, OrthoVariables.CurPage + 2);
        LoadImages((OrthoVariables.lessonPage + 1).toString());
        if (lessonpage >= 0) {
            if (OrthoVariables.PageTracking[lessonpage] === undefined) {
                OrthoVariables.PageTracking[lessonpage] = {
                    status:"pending",
                    grade:OrthoVariables.LessonData.Page[lessonpage].attributes.Grade,
                    nextpass:false,
                    submitbutton:false
                };
            }
        }
        if (OrthoVariables.CurPage <= 1) {
            DisableButtonLink("PreviousTest");
            EnableButtonLink("NextTest");
        }
        else {
            EnableButtonLink("PreviousTest");
            if (!OrthoVariables.PageTracking[lessonpage].nextpass) {
                DisableButtonLink("NextTest");
            }
            else {
                EnableButtonLink("NextTest");
            }
        }
        if (OrthoVariables.PageTracking[lessonpage].submitbutton) {
            EnableButtonLink("SubmitAnswer");
        } else {
            DisableButtonLink("SubmitAnswer");
        }

        CheckResizeLimits();
    });

    $("#lesson").bind("last", function (e, page, pageObj) {
        $("#pageresults").html($("#EndPageTemplate").render(OrthoVariables));
    });


}

// Image Functions
function CheckResizeLimits(page) {
    //console.log ("original w,h:" + w + "," + h);
    page = page || OrthoVariables.lessonPage;
    if (page > -1) {
        var h = $(window).height() - OrthoVariables.HeightFromBottom;
        var w = Math.round($('#content_wrap').width()/2);
        w = (w< 250) ? 250 : w;
        //console.log(w + "," + h);
        var mypage = OrthoVariables.LessonData.Page[page];
        var subid = (mypage.Widget[0].type === "compleximage") ? 0 : 1;
        var id = page.toString() + subid.toString();
        var cW = Math.round(OrthoVariables.origCanvas[id][0].width);
        var cH = Math.round(OrthoVariables.origCanvas[id][0].height);
        var ratio = cW / cH;
        var nH = cH;
        var nW = cW;
        if (cH > (h - 80)) {
            nH = h - 80;
            nH = (nH <300 ) ? 300 : nH;
            nW = ratio * nH;
            if (nW> w -20) {
                nW = w - 20;
                nH = nW/ratio;
            }
        }
        else if (cW > w - 20) {
            nW = w - 20;
            nH = nW / ratio;
        }
        if (nW < 250) {
            nW = 250;
            nH = nW /ratio;
        }
        resize(id, nH, nW);
    }
    //console.log(cW + "," + cH);
}

function resize(id, newHeight, newWidth) {
    //var newHeight = OrthoVariables.origCanvas[id][0].height * scale;
    //var newWidth = OrthoVariables.origCanvas[id][0].width * scale
    OrthoVariables.scalePage[OrthoVariables.lessonPage] = newHeight / OrthoVariables.origCanvas[id][0].height;
    $("#canvasid_" + id).css("height", newHeight).css("width", newWidth);
    OrthoVariables.origCanvas[id][2].setSize(newWidth, newHeight);
    OrthoVariables.origCanvas[id][2].setScale(OrthoVariables.scalePage[OrthoVariables.lessonPage]);
    $("#pointer_" + id).css("height", newHeight).css("width", newWidth);
    $("#container_" + id).css("top", -newHeight);
    $("#1_" + id).css("width", newWidth);
    $("#2_" + id).css("width", newWidth);
    $("#slider_b_" + id).css("left", newWidth - 165);
    $("#slider_c_" + id).css("left", newWidth - 115);

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
    if (!(imgTarget === "brightness" && OrthoVariables.buttonState[OrthoVariables.lessonPage]["b"]) && !(imgTarget === "contrast" && OrthoVariables.buttonState[OrthoVariables.lessonPage]["c"]) && !(OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"] && imgTarget === "draw")) {
        var imgobj = $("#" + imgTarget + "_" + id);
        imgobj.attr("src", imgobj.attr("src").replace(".", "_on."));
    }
}

function ShowOffImage(id, imgTarget) {
    if (!(imgTarget === "brightness" && OrthoVariables.buttonState[OrthoVariables.lessonPage]["b"]) && !(imgTarget === "contrast" && OrthoVariables.buttonState[OrthoVariables.lessonPage]["c"]) && !(OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"] && imgTarget === "draw")) {
        var imgobj = $("#" + imgTarget + "_" + id);
        imgobj.attr("src", imgobj.attr("src").replace("_on.", "."));
    }
}

function TogglePaint(action, id, pointerclass) {
    if (!(OrthoVariables.PageTracking[OrthoVariables.lessonPage].status === "correct" || OrthoVariables.PageTracking[OrthoVariables.lessonPage].status === "wrong")) {
        switch (action) {
            case "line":
                OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"] = !OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"];
                if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"]) {
                    $("#pointer_" + id).removeClass().addClass("pencilcursor");
                } else {
                    $("#pointer_" + id).removeClass().addClass(pointerclass);
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
        var myshapelayer = mystage.get("#shapelayer")[0];
        myshapelayer.removeChildren();
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
    if (id === "SubmitAnswer") {
        OrthoVariables.PageTracking[OrthoVariables.lessonPage].submitbutton = false;
    }
    if (id === "NextTest") {
        $('#lesson').turn('disable', true);
    }
    switch (id) {
        case "NextTest":
            $("#" + id).animate({right:"-130px"}, 500, 'linear');
            break;
        case "PreviousTest":
            $("#" + id).animate({left:"-130px"}, 500, 'linear');
            break;
        case "SubmitAnswer":
            $("#" + id).animate({top:"-50px"}, 500, 'linear');
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
                $('#lesson').turn('disable', false);
                $("#" + id).animate({right:"10px"}, 1000, 'easeOutElastic');
                break;
            case "PreviousTest":
                $("#" + id).on("click", function () {
                    if ($("#PreviousTest").data("fire") === true) {
                        DecreasePage();
                    }
                });
                $("#" + id).animate({left:"10px"}, 1000, 'easeOutElastic');
                break;
            case "SubmitAnswer":
                $("#" + id).on("click", function () {
                    SubmitAnswer();
                });
                OrthoVariables.PageTracking[OrthoVariables.lessonPage].submitbutton = true;
                $("#" + id).animate({top:"+=50px"}, 1000, 'easeOutElastic');
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

// Message Functions
function ShowMsg(message, type) {
    var id = ++OrthoVariables.msg_curIndex;
    $("#msg_area").append($("#msgboxTemplate").render({
        "id":id,
        "type":type,
        "message":message
    }));
    // Clear the timeout or stop animation when mouse over notification
    $("#msgbox_" + id).mouseenter(function () {
        var myid = getID(this.id);
        if (OrthoVariables.msg_info[myid] != null) {
            clearTimeout(OrthoVariables.msg_info[myid]);
            OrthoVariables.msg_info[myid] = null;
            $(this).stop(false, false);
            $("#" + this.id + "> div > .ui-icon-closethick").fadeIn('fast');
            $(this).fadeOut(1).fadeIn();
        }
    });
    $("#msgbox_" + id).click(function () {
        $(this).fadeOut("slow", function () {
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
    var myPage = id[0];
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
    if (OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"]) {
        OrthoVariables.buttonState[OrthoVariables.lessonPage]["l"] = false;
        var mypage = OrthoVariables.LessonData.Page[OrthoVariables.lessonPage];
        var subid = (mypage.Widget[0].type === "compleximage") ? 0 : 1;
        var id = OrthoVariables.lessonPage.toString() + subid.toString();
        $("#pointer_" + id).removeClass().addClass("pointcursor");
        ShowOffImage(id,"draw");
    }

    switch (type) {
        case "quiz":
            $.getJSON(OrthoVariables.JsonUrl, GetQuizQuestion(), function (data) {
                ApplyQuizResult(data);
            });
            break;
        case "hotspots":
            $.getJSON(OrthoVariables.JsonUrl, GetHotspotQuestion(), function (data) {
                ApplyHotspotResult(data);
            });
            break;
    }
    $("#overlay").removeClass("overlay_hidden").addClass("waiting");
}

function GetQuizQuestion() {
    var Question = new Object();
    Question.action = 2;
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

function GetHotspotQuestion() {
    var Question = new Object();
    Question.name = OrthoVariables.InitialQueryString["name"];
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
        } else if (mypage.Widget[0].type === "compleximage") {
            if (mypage.Widget[0].Image.HotSpots === "yes") {
                type = "hotspots"
            }
        } else if (mypage.Widget[1].type === "compleximage") {
            if (mypage.Widget[1].Image.HotSpots === "yes") {
                type = "hotspots"
            }
        }
    }
    return type;
}

function ApplyQuizResult(data) {
    var myanswer = "wrong";
    var blocked = OrthoVariables.LessonData.Page[OrthoVariables.lessonPage].attributes["Blocked"]
    if (data.Answer === "correct") {
        ShowMsg("Your Answer is Correct!", "highlight");
        myanswer = "correct";
        $("#Page" + (OrthoVariables.CurPage - 1)).css("background", "url('img/bg_correct.png')");
    } else {
        ShowMsg("Your Answer is Wrong!", "alert");
        $("#Page" + (OrthoVariables.CurPage - 1)).css("background", "url('img/bg_wrong.png')");
        myanswer = "wrong";
    }
    var length = data.PaintShapes.length;

    if (length > 0) {
        var mypage = OrthoVariables.LessonData.Page[OrthoVariables.lessonPage];
        var subid = (mypage.Widget[0].type === "compleximage") ? 0 : 1;
        var id = OrthoVariables.lessonPage.toString() + subid.toString();

        var mystage = OrthoVariables.origCanvas[id][2];
        var myshapelayer = mystage.get("#answerlayer")[0];
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
        myshapelayer.draw();
        var childrens = myshapelayer.getChildren();
        for (var i = 0; i < childrens.length; i++) {
            childrens[i].transitionTo({alpha:0.5, duration:2, easing:'ease-out'});
        }
        myshapelayer.draw();
    }

    if (data.CorrectAnswer !== "") {
        var ca = data.CorrectAnswer.split(";");

        if (data.Answer === "wrong") {
            ApplyQuizColors(ca);
        }
        for (var i = 1; i < ca.length; i++) {
            ApplyQuizColor(ca[i], data.Answer)
        }

    }

    CheckReadyNextText(myanswer, blocked);
    PageTracking(myanswer, blocked);
    RemoveOverlay();
}

function ApplyQuizColor(index, result) {
    var id = "";
    var widgets = OrthoVariables.LessonData.Page[OrthoVariables.lessonPage].Widget;
    for (var i = 0; i < widgets.length; i++) {
        if (widgets[i].type === "quiz") {
            id = widgets[i].Quiz.id;
        }
    }
    id = index + ".answer_" + id;
    var inputlelem = $("[name='" + id + "']");
    inputlelem.parent().css("text-shadow", "1px 1px 0 black").stop(false,true).animate({backgroundColor:OrthoVariables.ColorRight, color:"white"}, 2000);
}

function ApplyQuizColors(ca) {
    var id = "";
    var widgets = OrthoVariables.LessonData.Page[OrthoVariables.lessonPage].Widget;
    for (var i = 0; i < widgets.length; i++) {
        if (widgets[i].type === "quiz") {
            id = widgets[i].Quiz.id;
        }
    }

    for (var i = 0; i < OrthoVariables.lessonAnswers[OrthoVariables.lessonPage].quiz.length; i++) {
        //console.log(Iscontains(i, ca));
        if (!Iscontains(i, ca) === true && OrthoVariables.lessonAnswers[OrthoVariables.lessonPage].quiz[i] === true) {
            var input = $("[name='" + i + ".answer_" + id + "']");
            input.parent().removeClass("quizselected").css("text-shadow", "1px 1px 0 black").animate({backgroundColor:OrthoVariables.ColorWrong, color:"white"}, 2000);
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


function ApplyHotspotResult(data) {
    var myanswer = "wrong";
    var blocked = OrthoVariables.LessonData.Page[OrthoVariables.lessonPage].attributes["Blocked"]
    if (data.Answer === "correct") {
        ShowMsg("Your Answer is Correct!", "highlight");
        myanswer = "correct";
        $("#Page" + (OrthoVariables.CurPage - 1)).css("background", "url('img/bg_correct.png')")

    }
    else {
        ShowMsg("Your Answer is Wrong!", "alert");
        myanswer = "wrong";
        $("#Page" + (OrthoVariables.CurPage - 1)).css("background", "url('img/bg_wrong.png')");
    }
    var length = data.PaintShapes.length;
    //var fillcolor = (myanswer==="correct") ? OrthoVariables.ColorRight : OrthoVariables.ColorWrong;
    var fillcolor = function (fillanswer) {
        return   (fillanswer) ? OrthoVariables.ColorRight : OrthoVariables.ColorWrong;
    }
    var strikecolor = (myanswer === "correct") ? OrthoVariables.ColorRightEdge : OrthoVariables.ColorWrongEdge;
    if (length > 0) {
        var mypage = OrthoVariables.LessonData.Page[OrthoVariables.lessonPage];
        var subid = (mypage.Widget[0].type === "compleximage") ? 0 : 1;
        var id = OrthoVariables.lessonPage.toString() + subid.toString();
        var mystage = OrthoVariables.origCanvas[id][2];
        var myshapelayer = mystage.get("#answerlayer")[0];
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
            childrens[i].transitionTo({alpha:0.5, duration:2, easing:'ease-out'});
        }
        myshapelayer.draw();
    }
    PageTracking(myanswer, blocked);
    CheckReadyNextText(myanswer, blocked);
    RemoveOverlay();
}

function PageTracking(answer, blocked) {
    if (answer === "correct") {
        OrthoVariables.PageTracking[OrthoVariables.lessonPage].status = "correct";
        OrthoVariables.PageTracking[OrthoVariables.lessonPage].nextpass = true;
    } else {
        OrthoVariables.PageTracking[OrthoVariables.lessonPage].status = "wrong";
        OrthoVariables.PageTracking[OrthoVariables.lessonPage].grade = 0;
        OrthoVariables.PageTracking[OrthoVariables.lessonPage].nextpass = (blocked === "yes") ? false : true;
    }
}

function RemoveOverlay() {
    $("#overlay").removeClass("waiting").addClass("overlay_hidden");
}

function PaintCircle(data, fillcolor, strokecolor) { // for kinetic 3.9.4
    var circle = new Kinetic.Circle({
        x:data[1]["X"],
        y:data[1]["Y"],
        radius:data[2],
        fill:fillcolor,
        stroke:strokecolor,
        strokeWidth:1,
        alpha:0
    });
    return circle;
}

function newPaintCircle(data, fillcolor, strokecolor) {
    var circle = new Kinetic.Ellipse({
        x:data[1]["X"],
        y:data[1]["Y"],
        radius: {  x: data[2],y: data[2] },
        fill:fillcolor,
        stroke:strokecolor,
        strokeWidth:1,
        alpha:0
    });
    return circle;
}

function PaintEclipse(data, fillcolor, strokecolor) {  // for kinetic 3.9.4
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
        x:data[1]["X"],
        y:data[1]["Y"],
        radius:radius,
        fill:fillcolor,
        stroke:strokecolor,
        strokeWidth:1,
        alpha:0
    });
    eclipse.setScale(scalex, scaley);
    return eclipse;
}

function newPaintEclipse(data, fillcolor, strokecolor) {
    var eclipse = new Kinetic.Ellipse({
        x:data[1]["X"],
        y:data[1]["Y"],
        radius: {x: data[2]["RadiusX"], y:data[2]["RadiusY"]},
        fill:fillcolor,
        stroke:strokecolor,
        strokeWidth:1,
        alpha:0
    });
    return eclipse;
}

function PaintRect(data, fillcolor, strokecolor) {
    var rect = new Kinetic.Rect({
        x:data[1]["X"],
        y:data[1]["Y"],
        width:data[2],
        height:data[3],
        fill:fillcolor,
        stroke:strokecolor,
        strokeWidth:1,
        alpha:0
    });
    return rect;
}

function PaintPolygon(data, fillcolor, strokecolor) {
    var points = [];
    var len = (data.length - 1);
    for (var i = 0; i < len; i++) {
        points[i] = {
            x:parseInt(data[i + 1]["X"]),
            y:parseInt(data[i + 1]["Y"])
        };
    }
    var poly = new Kinetic.Polygon({
        points:points,
        fill:fillcolor,
        stroke:strokecolor,
        strokeWidth:1,
        alpha:0
    });
    return poly;
}

