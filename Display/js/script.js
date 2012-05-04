/* Author: Konstantinos Zagoris
 The script logic for ORTHO e-Man
 */
"use strict";

var OrthoVariables = {
    maxPages:5,
    CurPage:1,
    HeightFromBottom:200 ,   //$('#navigation').height() - $('footer').height();
    origCanvas: [],
    JsonUrl: "sslayer.php",
    LessonData: "",
    buttonState: { "b" : false, "c" : false, "l" : false },
    line: {"pressed": false, startx:-1, starty:-1, "prevline":null}
};

//var JsonUrl = "sslayer.php";
//var LessonData = "";
//var origCanvas = [];



$(document).ready(function () {
    //Helper Functions
    //var LessonData = {Page: }
    $.getJSON(OrthoVariables.JsonUrl, {"action":1}, function (data) {
        OrthoVariables.LessonData = data;
        OrthoVariables.maxPages = 2 * (OrthoVariables.LessonData.Page.length + 1);
        DoTemplating();
        displayFunctions();
        /*
         // Testing the values of the return object
         alert("lessonid:" + data["@attributes"].id + "\n" +
         "abstract:" + data["abstract"] + "\n" +
         "No Pages:" + data.Page.length + data.Page[0]["@attributes"].Title
         );*/

    })


});

function DoTemplating() {

    $("#lesson").html(
        $("#LessonTemplate").render(OrthoVariables.LessonData)
    );

    // Loading the Image to Canvas
    for (var i in OrthoVariables.LessonData.Images) {
        var c = $('#canvasid_' + OrthoVariables.LessonData.Images[i].id).get(0)
        c.getContext("2d").zag_LoadImage(OrthoVariables.LessonData.Images[i].url);
        var orig = document.createElement('canvas');
        orig.width = c.width;
        orig.height = c.height;
        orig.getContext("2d").zag_LoadImage(OrthoVariables.LessonData.Images[i].url);
        //orig.getContext("2d").drawImage(c, 0 , 0);
        OrthoVariables.origCanvas[OrthoVariables.LessonData.Images[i].id] = [orig,OrthoVariables.LessonData.Images[i].url ];
        //sliders
        $('#slider_b_' + OrthoVariables.LessonData.Images[i].id).slider({
            range: "max",
            min:-100,
            max:100,
            value:0,
            slide: function(event,ui){
                var id = this.id.substr(this.id.lastIndexOf("_")+1,this.id.length);
                var value = ui.value/100;
                Brightness(id,value, OrthoVariables.origCanvas[id][0].zag_Clone());
            }
        });
        $('#slider_c_' + OrthoVariables.LessonData.Images[i].id).slider({
            range: "max",
            min:-100,
            max:100,
            value:0,
            slide: function(event,ui){
                var id = getID(this.id);
                var value = ui.value/100;
                Contrast(id,value, OrthoVariables.origCanvas[id][0].zag_Clone());
            }
        });
       //for shapes
        var stage = new Kinetic.Stage({
            container: "container_"+OrthoVariables.LessonData.Images[i].id,
            width:c.width,
            height:c.height,
            listen: true
        });
        var shapelayer = new Kinetic.Layer({
            id: "shapelayer"
        });

        stage.add(shapelayer);
        OrthoVariables.origCanvas[OrthoVariables.LessonData.Images[i].id][2] = stage;
        //stage.on('mouseover', function() {console.log("here")});
        //var s = stage.getDOM();
        $("#container_" +OrthoVariables.LessonData.Images[i].id).click(function() {
            var id = getID(this.id);
            var mystage = OrthoVariables.origCanvas[id][2];
            var mousepos = mystage.getMousePosition();
            if (mousepos !== undefined)
            {

               var myshapelayer = mystage.get("#shapelayer")[0];

                if (!OrthoVariables.buttonState["l"]) {
                    var shapes =  mystage.getIntersections({
                        x: mousepos.x,
                        y: mousepos.y
                    })
                    //var mylayer = new Kinetic.Layer();
                    if (shapes.length == 0) {
                        var circle = new Kinetic.Circle({
                            x: mousepos.x,
                            y: mousepos.y,
                            radius:5,
                            fill: "#227bab",
                            stroke: "#2177a5",
                            strokeWidth: 1
                        })

                        circle.on("mouseover", function() {
                            $(".imgcontainer").css("cursor", "pointer");
                        });
                        circle.on("mouseout", function() {
                            $(".imgcontainer").css("cursor", "crosshair");
                        });


                        myshapelayer.add(circle);
                        myshapelayer.draw();
                        $(".imgcontainer").css("cursor", "pointer");
                    }
                    else {
                        myshapelayer.remove(shapes[0]);
                        myshapelayer.draw();
                        $(".imgcontainer").css("cursor", "crosshair");
                    }

                }



            }

        });
        $("#container_" + OrthoVariables.LessonData.Images[i].id).mousedown(function(){
            if (OrthoVariables.buttonState["l"]){
                var id = getID(this.id);
                var mystage = OrthoVariables.origCanvas[id][2];
                var mousepos = mystage.getMousePosition();
                if (mousepos !== undefined){
                    OrthoVariables.line.pressed = true;
                    OrthoVariables.line.startx = mousepos.x;
                    OrthoVariables.line.starty = mousepos.y;
                }
            }
        });

        $("#container_" + OrthoVariables.LessonData.Images[i].id).mousemove(function(){
           if (OrthoVariables.buttonState["l"] && OrthoVariables.line.pressed)
           {
              DrawShape(this.id);
           }
        });

        $("#container_" + OrthoVariables.LessonData.Images[i].id).mouseup(function() {
            if (OrthoVariables.buttonState["l"] && OrthoVariables.line.pressed){
               DrawShape(this.id);
               OrthoVariables.line.pressed = false;
                OrthoVariables.line.startx = -1;
                OrthoVariables.line.starty = -1;
                OrthoVariables.line.prevline = null;
            }

        });

        $("#container_" + OrthoVariables.LessonData.Images[i].id).mouseout(function(){
            if (OrthoVariables.buttonState["l"] && OrthoVariables.line.pressed) {
                OrthoVariables.line.pressed = false;
                OrthoVariables.line.startx = -1;
                OrthoVariables.line.starty = -1;
                OrthoVariables.line.prevline = null;
            }

        });


    }
}

function DrawShape ($strID) {
    var id = getID($strID);
    var mystage = OrthoVariables.origCanvas[id][2];
    var mousepos = mystage.getMousePosition();
    if (mousepos !== undefined){
        var myshapelayer = mystage.get("#shapelayer")[0];
        if (OrthoVariables.line.prevline != null)
        {
            myshapelayer.remove(OrthoVariables.line.prevline);
        }
        var line = new Kinetic.Line({
            points:[{
                x: OrthoVariables.line.startx,
                y:OrthoVariables.line.starty},{
                x:mousepos.x,
                y:mousepos.y}],
            stroke: "black",
            strokeWidth: 2,
            lineCap: 'round',
            lineJoin: 'round'
        });
        myshapelayer.add(line);
        OrthoVariables.line.prevline = line;
        myshapelayer.draw();
    }
}

 function getID (strID) {
     return strID.substr(strID.lastIndexOf("_")+1,strID.length);

 }

function getBrOrCo(strID) {
    return strID.substr(strID.indexOf("_")+1,1);
}

function displayFunctions() {
    $('#lesson').turn();
    $('#lesson').turn('size', $('#content_wrap').width(), $(window).height() - OrthoVariables.HeightFromBottom);

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
            var w = $('#content_wrap').width();
            $('#lesson').turn('size', w, h);
        });

    CheckNavLimits();

    $('#lesson').bind('turned', function (e, page, pageObj) {
        //if (page < CurPage) CurPage = page + 1;
        OrthoVariables.CurPage = page % 2 == 0 && page != 1 ? page + 1 : page;
        CheckNavLimits();

    });
    $('#NextTest').click(function () {
        IncreasePage();
    });
    $("#PreviousTest").click(function () {
        DecreasePage();
    });


    ApplyRoundtoPages();
}


// Image Functions

function InvertImage(id) {
    var c_slider = $('#slider_c_'+id);
    var b_slider = $('#slider_b_'+id);
    if (OrthoVariables.buttonState["c"]) {
        c_slider.hide('slide', function() {ShowOffImage(id,"contrast");});
        OrthoVariables.buttonState["c"] = false;
    }
    if (OrthoVariables.buttonState["b"]) {
        b_slider.hide('slide', function() {ShowOffImage(id,"brightness");});
        OrthoVariables.buttonState["b"] = false;
    }
    var mycanvas = $('#canvasid_' + id).get(0);
    mycanvas.getContext("2d").zag_Invert(0,0,mycanvas.width, mycanvas.height);
    OrthoVariables.origCanvas[id][0] = mycanvas.zag_Clone();
}

function SaveImageState(id)
{
    var mycanvas = $('#canvasid_' + id).get(0);
    OrthoVariables.origCanvas[id][0] = mycanvas.zag_Clone();
}


function Brightness(id, value, canvasobj) {
    var mycanvas = $('#canvasid_' + id).get(0);
    canvasobj.getContext("2d").zag_Brightening(value,0,0,mycanvas.width, mycanvas.height);
    mycanvas.getContext("2d").drawImage(canvasobj,0,0);

}

function ShowOnImage(id, imgTarget)
{
    if ( !(imgTarget === "brightness" && OrthoVariables.buttonState["b"]) && !(imgTarget === "contrast" && OrthoVariables.buttonState["c"]) && !OrthoVariables.buttonState["l"]) {
        var imgobj = $("#" + imgTarget + "_" + id);
        imgobj.attr("src", imgobj.attr("src").replace(".", "_on."));
    }
}

function ShowOffImage(id, imgTarget)
{
   if ( !(imgTarget === "brightness" && OrthoVariables.buttonState["b"]) && !(imgTarget === "contrast" && OrthoVariables.buttonState["c"]) && !OrthoVariables.buttonState["l"]) {
    var imgobj = $("#" + imgTarget + "_" + id);
    imgobj.attr("src", imgobj.attr("src").replace("_on.", "."));
   }
}

function TogglePaint (action)
{
    switch (action)
    {
        case "line":
            OrthoVariables.buttonState["l"] = !OrthoVariables.buttonState["l"];
            break;
    }
}

function Contrast(id, value, canvasobj) {
    var mycanvas = $('#canvasid_' + id).get(0);
    canvasobj.getContext("2d").zag_Contrast(value,0,0,mycanvas.width, mycanvas.height);
    mycanvas.getContext("2d").drawImage(canvasobj,0,0);
}

function ReloadImage(id)
{
    //alert(id);
    //$('#canvasid_' + id).get(0).getContext("2d").drawImage(OrthoVariables.origCanvas[id],0,0);
    $('#canvasid_' + id).get(0).getContext("2d").zag_LoadImage(OrthoVariables.origCanvas[id][1]);
}

function ActionSlider(sliderid, action)
{
    var myslide = $('#'+sliderid);
    var borc = getBrOrCo(sliderid);
    switch (action)
    {
        case 'toggle':
            myslide.toggle('slide');
            OrthoVariables.buttonState[borc] = !OrthoVariables.buttonState[borc];
            break;
        case 'show':
            if (!OrthoVariables.buttonState[borc]) {
                myslide.show('slide');
                OrthoVariables.buttonState[borc] = true;
            }
            break;
        case 'hide':
            if (OrthoVariables.buttonState[borc]) {
                var id = getID(sliderid)
                SaveImageState(id);
                myslide.slider('value',0);
                myslide.hide('slide', function() {ShowOffImage(id, (borc === "c") ? "contrast" : "brightness" );});
                OrthoVariables.buttonState[borc] = false;
            }
            break;
    }

}

// Book Like Functions
function ApplyRoundtoPages() {
    for (var i = 1; i <= OrthoVariables.maxPages; i++)
        if (i % 2 == 0)
            $(".p" + i).addClass("even");
        else
            $(".p" + i).addClass("odd");
}

function CheckNavLimits() {
    if (OrthoVariables.CurPage >= OrthoVariables.maxPages) DisableButtonLink("NextTest");
    else EnableButtonLink("NextTest");
    if (OrthoVariables.CurPage <= 1) DisableButtonLink("PreviousTest");
    else EnableButtonLink("PreviousTest");
}


function IncreasePage() {
    if (OrthoVariables.CurPage < OrthoVariables.maxPages) {
        OrthoVariables.CurPage += 2;
        if (OrthoVariables.CurPage > OrthoVariables.maxPages) OrthoVariables.CurPage = OrthoVariables.maxPages;
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

}

function EnableButtonLink(id) {
    $("#" + id).removeClass("disablemore").addClass("more");
}

function CreatePages() {
    var pages = "";

    for (var i = 1; i < OrthoVariables.maxPages; i++) {
        pages += "<div id=\"Page" + i + "\"></div>";
    }

}

