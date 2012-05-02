/* Author: Konstantinos Zagoris
 The script logic for ORTHO e-Man
 */
"use strict";

var OrthoVariables = {
    maxPages:5,
    CurPage:1,
    HeightFromBottom:200    //$('#navigation').height() - $('footer').height();
};

var JsonUrl = "sslayer.php";
var LessonData = "";
var origCanvas = [];



$(document).ready(function () {
    //Helper Functions
    //var LessonData = {Page: }
    $.getJSON(JsonUrl, {"action":1}, function (data) {
        LessonData = data;
        OrthoVariables.maxPages = 2 * (LessonData.Page.length + 1);
        DoTemplating();
        //displayFunctions();
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
        $("#LessonTemplate").render(LessonData)
    );

    // Loading the Image to Canvas
    for (var i in LessonData.Images) {
        var c = $('#canvasid_' + LessonData.Images[i].id).get(0)
        c.getContext("2d").zag_LoadImage(LessonData.Images[i].url);
        var orig = document.createElement('canvas');
        orig.width = c.width;
        orig.height = c.height;
        orig.getContext("2d").zag_LoadImage(LessonData.Images[i].url);
        origCanvas[LessonData.Images[i].id] = orig;
    }
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
    var mycanvas = $('#canvasid_' + id).get(0);
    mycanvas.getContext("2d").zag_Invert(0,0,mycanvas.width, mycanvas.height);
}

function Brightness(id, value) {
    var mycanvas = $('#canvasid_' + id).get(0);
    mycanvas.getContext("2d").zag_Brightening(value,0,0,mycanvas.width, mycanvas.height);

}

function Contrast(id, value) {
    var mycanvas = $('#canvasid_' + id).get(0);
    mycanvas.getContext("2d").zag_Contrast(value,0,0,mycanvas.width, mycanvas.height);
}

function ReloadImage(id)
{
    //alert(id);
    $('#canvasid_' + id).get(0).getContext("2d").drawImage(origCanvas[id],0,0);
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

