/* Author: Konstantinos Zagoris
 The script logic for ORTHO e-Man
*/


var OrthoVariables = {
    maxPages : 5,
    CurPage : 1,
    HeightFromBottom: 200    //$('#navigation').height() - $('footer').height();
};

$(document).ready(function(){


    $('#lesson').turn();
    $('#lesson').turn('size', $('#content_wrap').width(),$(window).height()-OrthoVariables.HeightFromBottom);

    //for debuging
    //CurPage = 3; ShowPage();

    $(window).bind('keydown', function(e){
        if (e.keyCode==37)
            $('#lesson').turn('previous');
        else if (e.keyCode==39)
            $('#lesson').turn('next');

    }).resize(function() {
            //$('body').prepend('<div>' + $('#content_wrap').width() + '</div>');
            var h = $(window).height() - OrthoVariables.HeightFromBottom;
            var w = $('#content_wrap').width();
            $('#lesson').turn('size', w, h);
        });

    CheckNavLimits();

    $('#lesson').bind('turned', function(e, page, pageObj) {
        //if (page < CurPage) CurPage = page + 1;
        OrthoVariables.CurPage = page % 2 == 0 && page != 1 ? page + 1 : page;
        CheckNavLimits();

    });
    $('#NextTest').click(function() { IncreasePage();});
    $("#PreviousTest").click(function() {DecreasePage();});
   
   $("#invert").click(function() {InvertImage("imgTest1N1");});
   $("#brightness").click(function() {Brightness("imgTest1N1", 130);})
   $("#contrast").click(function() {Contrast("imgTest1N1", 1.5);})
   ApplyRoundtoPages();

});


function ApplyRoundtoPages ()
{
    for (var i = 1; i <= OrthoVariables.maxPages; i++)
        if (i % 2 == 0)
            $(".p" + i).addClass("even");
        else
            $(".p" + i).addClass("odd");
}

function CheckNavLimits ()
{
    if (OrthoVariables.CurPage >= OrthoVariables.maxPages) DisableButtonLink("NextTest");
    else EnableButtonLink("NextTest");
    if (OrthoVariables.CurPage <= 1) DisableButtonLink("PreviousTest");
    else EnableButtonLink("PreviousTest");
}

function InvertImage (id)
{
	$("#"+id).pixastic("invert");
}

function Brightness (id, value)
{
	$("#"+id).pixastic("brightness", {brightness:value});
}

function Contrast (id,value)
{
	$("#"+id).pixastic("brightness", {contrast:value});
}

function IncreasePage ()
{
    if (OrthoVariables.CurPage < OrthoVariables.maxPages)
    {
        OrthoVariables.CurPage+=2;
        ShowPage();
    }
}

function DecreasePage()
{

    if (OrthoVariables.CurPage > 1)
    {
        OrthoVariables.CurPage-=2;
        ShowPage();
    }
}

function ShowPage ()
{
   // alert(CurPage);
    $('#lesson').turn('page',OrthoVariables.CurPage);
}

    function DisableButtonLink (id){
        $("#"+id).removeClass("more").addClass("disablemore");

    }

    function EnableButtonLink (id) {
        $("#"+id).removeClass("disablemore").addClass("more");
    }



function CreatePages()
{
    var pages = "";

    for (var i=1;i< OrthoVariables.maxPages;i++)
    {
        pages += "<div id=\"Page" + i + "\"></div>";
    }

}

