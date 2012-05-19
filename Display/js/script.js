/* Author: Konstantinos Zagoris
 The script logic for ORTHO e-Man
 */"use strict";

var OrthoVariables = {
	maxPages : 5,
	CurPage : 1,
	HeightFromBottom : 200, //$('#navigation').height() - $('footer').height();
	origCanvas : [],
	JsonUrl : "sslayer.php",
	LessonData : "",
	buttonState : {
		"b" : false,
		"c" : false,
		"l" : false
	},
	line : {
		"pressed" : false,
		startx : -1,
		starty : -1,
		"prevline" : null
	},
	msg_info : [],
	msg_curIndex : 0,
	linemindistance : 10,
	clickcatch : false,
	lessonAnswers : [],
	lessonPage : -1,
    InitialQueryString: [],
    lessonLoaded:[],
    MaxHotSpots:[],
    PageTracking:[],
    ColorRight: "#047816",
    ColorRightEdge: "#285935",
    ColorWrong: "#9C2100",
    ColorWrongEdge: "#592835"

};


$(document).ready(function() {
    $.prettyLoader({
        animation_speed:'normal',
        bind_to_ajax: true,
        delay: false,
        loader: 'img/ajax-loader.gif'
    });
    document.onselectstart = function(){ return false; };
     OrthoVariables.InitialQueryString = getUrlVars();
    $.getJSON(OrthoVariables.JsonUrl, {
		"action" : 1, "name" : OrthoVariables.InitialQueryString["name"]

	}, function(data) {
		OrthoVariables.LessonData = data;
		OrthoVariables.maxPages = 2 * (OrthoVariables.LessonData.Page.length + 1);
		$("#lesson").html($("#LessonTemplate").render(OrthoVariables.LessonData));

        displayFunctions();
        LoadImages("0");

		ApplyRoundtoPages(1,3);
        //DisableButtonLink("SubmitAnswer");
        EnableButtonLink("NextTest");
	})
});


function getUrlVars()
{
    var vars = [], hash;
    var hashes = window.location.href.replace("#","").slice(window.location.href.indexOf('?') + 1).split('&');
    for(var i = 0; i < hashes.length; i++)
    {
        hash = hashes[i].split('=');
        if (hash[0] === "name"){
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
    for(var i in OrthoVariables.LessonData.Images) {
        if  (OrthoVariables.LessonData.Images[i].id[0] === Page && OrthoVariables.lessonLoaded[parseInt(Page)] === undefined) {
            imagesToLoad[counter] = OrthoVariables.LessonData.Images[i];
            counter++;
        }
    }
    for(var i =0; i< imagesToLoad.length;i++) {
        var c = $('#canvasid_' + imagesToLoad[i].id).get(0)
		c.getContext("2d").zag_LoadImage(imagesToLoad[i].url);
		var orig = document.createElement('canvas');
		orig.width = c.width;
		orig.height = c.height;
		orig.getContext("2d").zag_LoadImage(imagesToLoad[i].url);
		//orig.getContext("2d").drawImage(c, 0 , 0);
		OrthoVariables.origCanvas[imagesToLoad[i].id] = [orig, imagesToLoad[i].url];
        OrthoVariables.MaxHotSpots[imagesToLoad[i].id[0]] = imagesToLoad[i].MaxSpots;
		//sliders
		$('#slider_b_' + imagesToLoad[i].id).slider({
			range : "max",
			min : -100,
			max : 100,
			value : 0,
			slide : function(event, ui) {
				var id = this.id.substr(this.id.lastIndexOf("_") + 1, this.id.length);
				var value = ui.value / 100;
				Brightness(id, value, OrthoVariables.origCanvas[id][0].zag_Clone());
			}
		});
		$('#slider_c_' + imagesToLoad[i].id).slider({
			range : "max",
			min : -100,
			max : 100,
			value : 0,
			slide : function(event, ui) {
				var id = getID(this.id);
				var value = ui.value / 100;
				Contrast(id, value, OrthoVariables.origCanvas[id][0].zag_Clone());
			}
		});
		//for shapes
		var stage = new Kinetic.Stage({
			container : "container_" + imagesToLoad[i].id,
			width : c.width,
			height : c.height,
			listen : true
		});

		var shapelayer = new Kinetic.Layer({
			id : "shapelayer"
		});
		var answerlayer = new Kinetic.Layer({
			id : "answerlayer"
		});

		var tooltiplayer = new Kinetic.Layer({
			id : "tooltiplayer",
			throttle : 20

		});
		var tooltip = new Kinetic.Text({
			text : "",
			textFill : "white",
			fontFamily : "Georgia",
			fontSize : 8,
			verticalAlign : "bottom",
			padding : 4,
			fill : "black",
			visible : false,
			alpha : 0.75,
			id : "tooltip"
		});
		tooltiplayer.add(tooltip);
		stage.add(shapelayer);
		stage.add(tooltiplayer);
		stage.add(answerlayer);
		OrthoVariables.origCanvas[imagesToLoad[i].id][2] = stage;
		$("#container_" + imagesToLoad[i].id).click({
			"pos" : imagesToLoad[i].HotSpots
		}, function(event) {
			var id = getID(this.id);
			var mystage = OrthoVariables.origCanvas[id][2];
			var mousepos = mystage.getMousePosition();
			if(mousepos !== undefined) {

				var myshapelayer = mystage.get("#shapelayer")[0];
				var mytooltip = mystage.get("#tooltiplayer")[0].get("#tooltip")[0];
				var ishotspots = event.data.pos;
				if(!OrthoVariables.buttonState["l"] && ishotspots === "yes") {
                    if (OrthoVariables.PageTracking[OrthoVariables.lessonPage].status=== "correct") {
                        ShowMsg("You already answer it!", "highlight");
                        return true;
                    }
                    if (OrthoVariables.PageTracking[OrthoVariables.lessonPage].status=== "wrong" && OrthoVariables.LessonData.Page[OrthoVariables.lessonPage].attributes["Blocked"] === "no" ) {
                        ShowMsg("You already answer it!", "highlight");
                        return true;
                    }
                    var shapes = mystage.getIntersections({
						x : mousepos.x,
						y : mousepos.y
					})
					if(shapes.length == 0) {
						var circle = new Kinetic.Circle({
							x : mousepos.x,
							y : mousepos.y,
							radius : 6,
							fill : "#cb842e",
							stroke : "#cbb48f",
							strokeWidth : 1,
							alpha : 0.5,
							id : "circle_" + id
						});

						circle.on("mouseover", function() {
							$("#pointer_" + id).removeClass().addClass("erasercursor");
							var mousePos = mystage.getMousePosition();
							var x = mousePos.x + 5;
							var y = mousePos.y + 10;
							drawTooltip(mytooltip, x, y, "click to remove");
							this.transitionTo({
								scale : {
									x : 1.7,
									y : 1.7
								},
								duration : 0.3,
								easing : 'ease-out'
							});
						});
						circle.on("mouseout", function() {
							SetCursor(id);
							mytooltip.hide();
							mytooltip.getLayer().draw();
							this.transitionTo({
								scale : {
									x : 1,
									y : 1
								},

								duration : 0.3,
								easing : 'ease-in'
							});
						});
						circle.on("click", function() {
							if (OrthoVariables.PageTracking[OrthoVariables.lessonPage].status=== "correct" ||
                                (OrthoVariables.PageTracking[OrthoVariables.lessonPage].status=== "wrong" && OrthoVariables.LessonData.Page[OrthoVariables.lessonPage].attributes["Blocked"] === "no" )) {
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
						if(!OrthoVariables.clickcatch && !ReachMaxNumberHotSpots(OrthoVariables.lessonPage)) {
                                myshapelayer.add(circle);
                                OrthoVariables.lessonAnswers[OrthoVariables.lessonPage].hotspots[circle._id] = [mousepos.x, mousepos.y];
                                myshapelayer.draw();
                                $("#pointer_" + id).removeClass().addClass("erasercursor");
                                if(ReachMaxNumberHotSpots(OrthoVariables.lessonPage)) { EnableButtonLink("SubmitAnswer");}
						} else if (ReachMaxNumberHotSpots(OrthoVariables.lessonPage)){
                            ShowMsg("Reach maximum points. Please remove the previous to add new ones.","highlight" );
                        }
						OrthoVariables.clickcatch = false;
					} else {

					}

				}

			}

		});
		$("#container_" + imagesToLoad[i].id).mousedown(function() {
			if(OrthoVariables.buttonState["l"]) {
				var id = getID(this.id);
				var mystage = OrthoVariables.origCanvas[id][2];
				var mousepos = mystage.getMousePosition();
				if(mousepos !== undefined) {
					OrthoVariables.line.pressed = true;
					OrthoVariables.line.startx = mousepos.x;
					OrthoVariables.line.starty = mousepos.y;
				}
			}
		});

		$("#container_" + imagesToLoad[i].id).mousemove(function() {
			if(OrthoVariables.buttonState["l"] && OrthoVariables.line.pressed) {
				DrawShape(this.id);
			}
		});

		$("#container_" + imagesToLoad[i].id).mouseup(function() {
			if(OrthoVariables.buttonState["l"] && OrthoVariables.line.pressed) {
				DrawShape(this.id);
				SetonLine(this.id);
				CheckShape(this.id);
				OrthoVariables.line.pressed = false;
				OrthoVariables.line.startx = -1;
				OrthoVariables.line.starty = -1;
				OrthoVariables.line.prevline = null;
			}

		});

		$("#container_" + imagesToLoad[i].id).mouseout(function() {
			if(OrthoVariables.buttonState["l"] && OrthoVariables.line.pressed) {
				OrthoVariables.line.pressed = false;
				OrthoVariables.line.startx = -1;
				OrthoVariables.line.starty = -1;
				OrthoVariables.line.prevline = null;
			}

		});

	}

    OrthoVariables.lessonLoaded[parseInt(Page)] = true;
}

function drawTooltip(tooltip, x, y, text) {
	tooltip.setText(text);
	/*var maxRight = 530;
	 if(x > maxRight) {
	 x = maxRight;
	 } */
	tooltip.setPosition(x, y + 10);
	tooltip.show();
	tooltip.getLayer().draw();
}

function CheckShape($strID) {
	var id = getID($strID);
	var mystage = OrthoVariables.origCanvas[id][2];
	var mousepos = mystage.getMousePosition();
	if(mousepos !== undefined) {
		var distance = Distance(OrthoVariables.line.startx, OrthoVariables.line.starty, mousepos.x, mousepos.y);
		if(OrthoVariables.line.prevline != null && distance < OrthoVariables.linemindistance) {
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
	if(mousepos !== undefined) {
		var myshapelayer = mystage.get("#shapelayer")[0];
		if(OrthoVariables.line.prevline != null) {
			myshapelayer.remove(OrthoVariables.line.prevline);
		}
		var line = new Kinetic.Line({
			points : [{
				x : OrthoVariables.line.startx,
				y : OrthoVariables.line.starty
			}, {
				x : mousepos.x,
				y : mousepos.y
			}],
			stroke : "orange",
			strokeWidth : 2,
			lineCap : 'round',
			lineJoin : 'round',
			detectionType : "pixel"
		});
		//var mytooltip = mystage.get("#tooltiplayer")[0].get("#tooltip")[0];

		myshapelayer.add(line);
		OrthoVariables.line.prevline = line;
		myshapelayer.draw();
		line.saveData();
	}
}

function Distance(x1, y1, x2, y2) {
	return Math.abs(x2 - x1) + Math.abs(y2 - y1);
}

function SetonLine(strID) {
	var id = getID(strID);
	var line = OrthoVariables.line.prevline;
	line.on("mouseover", function() {
		$("#pointer_" + id).removeClass().addClass("erasercursor");
		var mystage = this.getStage();
		var mytooltip = mystage.get("#tooltiplayer")[0].get("#tooltip")[0];
		var mousepos = mystage.getMousePosition();
		var x = mousepos.x + 15;
		var y = mousepos.y + 10;
		drawTooltip(mytooltip, x, y, "click to remove");
	});
	line.on("mouseout", function() {
		//$("#pointer_"+id).removeClass().addClass("pencilcursor");
		SetCursor(id);
		var mystage = this.getStage();
		var mytooltip = mystage.get("#tooltiplayer")[0].get("#tooltip")[0];
		mytooltip.hide();
		mytooltip.getLayer().draw();
	});

	line.on("click", function() {
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
	if(OrthoVariables.buttonState["l"]) {
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

	$(window).bind('keydown', function(e) {
		if(e.keyCode == 37)
			$('#lesson').turn('previous');
		else if(e.keyCode == 39)
			$('#lesson').turn('next');

	}).resize(function() {
		//$('body').prepend('<div>' + $('#content_wrap').width() + '</div>');
		var h = $(window).height() - OrthoVariables.HeightFromBottom;
		var w = $('#content_wrap').width();
		$('#lesson').turn('size', w, h);
	});


	$('#lesson').bind('turned', function(e, page, pageObj) {

		OrthoVariables.CurPage = page % 2 == 0 && page != 1 ? page + 1 : page;
		var lessonpage = (OrthoVariables.CurPage === 0 || OrthoVariables.CurPage >= OrthoVariables.maxPages) ? -1 : ( Math.floor(OrthoVariables.CurPage / 2)) - 1;
		if(OrthoVariables.lessonAnswers[lessonpage] === undefined) {
			OrthoVariables.lessonAnswers[lessonpage] = {
				quiz : [],
				hotspots : []
			}
		};
		OrthoVariables.lessonPage = lessonpage;
		ApplyRoundtoPages(OrthoVariables.CurPage, OrthoVariables.CurPage+2);
        LoadImages((OrthoVariables.lessonPage+1).toString());
        if (lessonpage >= 0) {
        if (OrthoVariables.PageTracking[lessonpage] === undefined) {
            OrthoVariables.PageTracking[lessonpage] = {
                status: "pending",
                grade : OrthoVariables.LessonData.Page[lessonpage].attributes.Grade,
                nextpass: false,
                submitbutton: false
            };
        }}
        if (OrthoVariables.CurPage <= 1) {
            DisableButtonLink("PreviousTest");
            EnableButtonLink("NextTest");
        }
        else {
            EnableButtonLink("PreviousTest");
            if (!OrthoVariables.PageTracking[lessonpage].nextpass){
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



    });

    $("#lesson").bind("last", function(e,page,pageObj){
        $("#pageresults").html($("#EndPageTemplate").render(OrthoVariables));
    });


}

// Image Functions

function InvertImage(id) {
	var c_slider = $('#slider_c_' + id);
	var b_slider = $('#slider_b_' + id);
	if(OrthoVariables.buttonState["c"]) {
		c_slider.hide('slide', function() {
			ShowOffImage(id, "contrast");
		});
		OrthoVariables.buttonState["c"] = false;
	}
	if(OrthoVariables.buttonState["b"]) {
		b_slider.hide('slide', function() {
			ShowOffImage(id, "brightness");
		});
		OrthoVariables.buttonState["b"] = false;
	}
	var mycanvas = $('#canvasid_' + id).get(0);
	mycanvas.getContext("2d").zag_Invert(0, 0, mycanvas.width, mycanvas.height);
	OrthoVariables.origCanvas[id][0] = mycanvas.zag_Clone();
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
	if(!(imgTarget === "brightness" && OrthoVariables.buttonState["b"]) && !(imgTarget === "contrast" && OrthoVariables.buttonState["c"]) && !(OrthoVariables.buttonState["l"] && imgTarget === "draw")) {
		var imgobj = $("#" + imgTarget + "_" + id);
		imgobj.attr("src", imgobj.attr("src").replace(".", "_on."));
	}
}

function ShowOffImage(id, imgTarget) {
	if(!(imgTarget === "brightness" && OrthoVariables.buttonState["b"]) && !(imgTarget === "contrast" && OrthoVariables.buttonState["c"]) && !(OrthoVariables.buttonState["l"] && imgTarget === "draw")) {
		var imgobj = $("#" + imgTarget + "_" + id);
		imgobj.attr("src", imgobj.attr("src").replace("_on.", "."));
	}
}

function TogglePaint(action, id, pointerclass) {
	switch (action) {
		case "line":
			OrthoVariables.buttonState["l"] = !OrthoVariables.buttonState["l"];
			if(OrthoVariables.buttonState["l"]) {
				$("#pointer_" + id).removeClass().addClass("pencilcursor");
			} else {
				$("#pointer_" + id).removeClass().addClass(pointerclass);
			}

			break;
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
	$('#canvasid_' + id).get(0).getContext("2d").zag_LoadImage(OrthoVariables.origCanvas[id][1]);
	var mystage = OrthoVariables.origCanvas[id][2];
	var myshapelayer = mystage.get("#shapelayer")[0];
	myshapelayer.removeChildren();
	mystage.draw();
}

function ActionSlider(sliderid, action) {
	var myslide = $('#' + sliderid);
	var borc = getBrOrCo(sliderid);
	switch (action) {
		case 'toggle':
			myslide.toggle('slide');
			OrthoVariables.buttonState[borc] = !OrthoVariables.buttonState[borc];
			break;
		case 'show':
			if(!OrthoVariables.buttonState[borc]) {
				myslide.show('slide');
				OrthoVariables.buttonState[borc] = true;
			}
			break;
		case 'hide':
			if(OrthoVariables.buttonState[borc]) {
				var id = getID(sliderid)
				SaveImageState(id);
				myslide.slider('value', 0);
				myslide.hide('slide', function() {
					ShowOffImage(id, (borc === "c") ? "contrast" : "brightness");
				});
				OrthoVariables.buttonState[borc] = false;
			}
			break;
	}

}

// Book Like Functions
function ApplyRoundtoPages(Start,End) {
    for(var i = Start; i <= End; i++) {
		if(i % 2 == 0) {
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
    if(OrthoVariables.CurPage < OrthoVariables.maxPages) {
		OrthoVariables.CurPage += 2;
		if(OrthoVariables.CurPage > OrthoVariables.maxPages)
			OrthoVariables.CurPage = OrthoVariables.maxPages;
		ShowPage();
	}
}

function DecreasePage() {

	if(OrthoVariables.CurPage > 1) {
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
    if (id === "SubmitAnswer") {OrthoVariables.PageTracking[OrthoVariables.lessonPage].submitbutton = false;}
    if (id  === "NextTest") {$('#lesson').turn('disable', true);}

}

function EnableButtonLink(id) {
    if ($("#" + id).hasClass("disablemore")) {
        $("#" + id).removeClass("disablemore").addClass("more");
        switch (id) {
            case "NextTest":
                $("#" + id).on("click", function () {
                    IncreasePage();
                });
                $('#lesson').turn('disable', false);
                break;
            case "PreviousTest":
                $("#" + id).on("click",function () {
                    DecreasePage();
                });
                break;
            case "SubmitAnswer":
                $("#" + id).on("click",function () {
                    SubmitAnswer();
                });
                OrthoVariables.PageTracking[OrthoVariables.lessonPage].submitbutton = true;
                break;
        }
    }
}

function CreatePages() {
	var pages = "";

	for(var i = 1; i < OrthoVariables.maxPages; i++) {
		pages += "<div id=\"Page" + i + "\"></div>";
	}

}

// Message Functions
function ShowMsg(message, type) {
	var id = ++OrthoVariables.msg_curIndex;
	$("#msg_area").append($("#msgboxTemplate").render({
		"id" : id,
		"type" : type,
		"message" : message
	}));
	// Clear the timeout or stop animation when mouse over notification
	$("#msgbox_" + id).mouseenter(function() {
		var myid = getID(this.id);
		if(OrthoVariables.msg_info[myid] != null) {
			clearTimeout(OrthoVariables.msg_info[myid]);
			OrthoVariables.msg_info[myid] = null;
			$(this).stop(false, false);
			$("#" + this.id + "> div > .ui-icon-closethick").fadeIn('fast');
			$(this).fadeOut(1).fadeIn();
		}
	});
	$("#msgbox_" + id).click(function() {
		$(this).fadeOut("slow", function() {
			$(this).remove();
		});
	});

	$("#msgbox_" + id).slideDown("slow");

	var msgfadeout = setTimeout(function() {
		$("#msgbox_" + id).fadeOut(3000, function() {
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
    if (OrthoVariables.PageTracking[OrthoVariables.lessonPage].status=== "correct") {
        ShowMsg("You already answer it!", "highlight");
        element.checked = !element.checked;
        return true;
    }
    if (OrthoVariables.PageTracking[OrthoVariables.lessonPage].status=== "wrong" && OrthoVariables.LessonData.Page[OrthoVariables.lessonPage].attributes["Blocked"] === "no" ) {
        ShowMsg("You already answer it!", "highlight");
        element.checked = !element.checked;
        return true;
    }
    var id = getID(element.name);
	var myPage = id[0];
	var myindex = getIndex(element.name);
	OrthoVariables.lessonAnswers[myPage].quiz[myindex] = element.checked;
    if(element.checked) {
		$("[name='" + element.name + "']").parent().addClass("quizselected");
	} else {
		$("[name='" + element.name + "']").parent().removeClass("quizselected");
	}

    DisableButtonLink("SubmitAnswer");
    for (var i=0;i< OrthoVariables.lessonAnswers[myPage].quiz.length; i ++){
        if (OrthoVariables.lessonAnswers[myPage].quiz[i]) {
            EnableButtonLink("SubmitAnswer");
            break;
        }
    }

}



function ToggleText(element) {
	//var elemen
	//ToggleQuizSelection();
    if (OrthoVariables.PageTracking[OrthoVariables.lessonPage].status=== "correct") {
        ShowMsg("You already answer it!", "highlight");
        return true;
    }
    if (OrthoVariables.PageTracking[OrthoVariables.lessonPage].status=== "wrong" && OrthoVariables.LessonData.Page[OrthoVariables.lessonPage].attributes["Blocked"] === "no" ) {
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
    switch (type) {
		case "quiz":
			$.getJSON(OrthoVariables.JsonUrl, GetQuizQuestion(), function(data) {
				ApplyQuizResult(data);
			});
			break;
		case "hotspots":
			$.getJSON(OrthoVariables.JsonUrl, GetHotspotQuestion(), function(data) {
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
	for(var i = 0; i < OrthoVariables.lessonAnswers[Question.Page].quiz.length; i++) {
		if(OrthoVariables.lessonAnswers[Question.Page].quiz[i]) {
			answer +=";" + i;
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
	for (var i=0; i< hotspots.length;i++) {
		if (hotspots[i] !== undefined) {
			answer[counter]= [hotspots[i][0], hotspots[i][1]];
			counter++;
		}
	}
	Question.answer = answer;
    return Question;
}


function ReachMaxNumberHotSpots(pageid) {

    var counter = 0;
    var hotspots = OrthoVariables.lessonAnswers[pageid].hotspots;
    for (var i=0; i< hotspots.length;i++) {
        if (hotspots[i] !== undefined) {
            counter++;
        }
    }
    return  (counter >= OrthoVariables.MaxHotSpots[pageid]) ? true : false;
}

function CheckReadyNextText (answer, blocked) {
    if ( blocked === "yes" && answer === "wrong"){
        DisableButtonLink("NextTest");
    } else {
        EnableButtonLink("NextTest");
        DisableButtonLink("SubmitAnswer");
    }
}

function GetTypeofPage(PageID) {
	var type = undefined;
	var mypage = OrthoVariables.LessonData.Page[PageID];
	if(mypage !== undefined) {
		if(mypage.Widget[0].type === "quiz" || mypage.Widget[1].type === "quiz") {
			type = "quiz"
		} else if(mypage.Widget[0].type === "compleximage") {
			if(mypage.Widget[0].Image.HotSpots === "yes") {
				type = "hotspots"
			}
		} else if(mypage.Widget[1].type === "compleximage") {
			if(mypage.Widget[1].Image.HotSpots === "yes") {
				type = "hotspots"
			}
		}
	}
	return type;
}

function ApplyQuizResult(data) {
	var myanswer = "wrong";
    var blocked = OrthoVariables.LessonData.Page[OrthoVariables.lessonPage].attributes["Blocked"]
    if(data.Answer === "correct") {
		ShowMsg("Your Answer is Correct!", "highlight");
        myanswer = "correct";
	} else {
		ShowMsg("Your Answer is Wrong!", "alert");
        myanswer = "wrong";
	}
	var length = data.PaintShapes.length;

    if(length > 0) {
		var mypage = OrthoVariables.LessonData.Page[OrthoVariables.lessonPage];
		var subid = (mypage.Widget[0].type === "compleximage") ? 0 : 1;
		var id = OrthoVariables.lessonPage.toString() + subid.toString();
		var mystage = OrthoVariables.origCanvas[id][2];
		var myshapelayer = mystage.get("#answerlayer")[0];
		myshapelayer.removeChildren();
        var fillcolor = (myanswer==="correct") ? OrthoVariables.ColorRight : OrthoVariables.ColorWrong;
        var strikecolor = (myanswer==="correct") ? OrthoVariables.ColorRightEdge : OrthoVariables.ColorWrongEdge;
         for(var i = 0; i < length; i++) {
			switch (data.PaintShapes[i][0]) {
				case "Circle":
					myshapelayer.add(PaintCircle(data.PaintShapes[i],fillcolor,strikecolor));
					break;
				case "Rect":
					myshapelayer.add(PaintRect(data.PaintShapes[i],fillcolor,strikecolor));
					break;
				case "Polygon":
					myshapelayer.add(PaintPolygon(data.PaintShapes[i],fillcolor,strikecolor));
					break;
				case 'Eclipse':
					myshapelayer.add(PaintEclipse(data.PaintShapes[i],fillcolor,strikecolor));
					break;
			}
		}
		myshapelayer.draw();
        var childrens = myshapelayer.getChildren();
        for(var i=0;i< childrens.length;i++){
            childrens[i].transitionTo({alpha:0.5,duration:2,easing:'ease-out'});
        }
        myshapelayer.draw();
	}

    if (data.CorrectAnswer !== "") {
        var ca = data.CorrectAnswer.split(";");
        for (var i=1;i< ca.length;i++) {
           ApplyQuizColor(ca[i],data.Answer)
        }
    }
    CheckReadyNextText(myanswer,blocked);
    PageTracking(myanswer,blocked);
    RemoveOverlay();
}

function ApplyQuizColor(index, result) {
    var id = "";
    var widgets = OrthoVariables.LessonData.Page[OrthoVariables.lessonPage].Widget;
    for(var i=0; i<widgets.length;i++) {
        if (widgets[i].type ==="quiz") {
            id = widgets[i].Quiz.id;
        }
    }
    id = index + ".answer_" + id;
    if (result==="correct") {
        $("[name='" + id + "']").parent().css("text-shadow", "1px 1px 0 black").animate({backgroundColor: OrthoVariables.ColorRight, color:"white"},2000);
    } else {
        $("[name='" + id + "']").parent().css("text-shadow", "1px 1px 0 black").animate({backgroundColor: OrthoVariables.ColorWrong, color:"white"},2000);
    }

}


function ApplyHotspotResult(data) {
    var myanswer = "wrong";
    var blocked = OrthoVariables.LessonData.Page[OrthoVariables.lessonPage].attributes["Blocked"]
    if(data.Answer === "correct") {
        ShowMsg("Your Answer is Correct!", "highlight");
        myanswer = "correct";

    } else {
        ShowMsg("Your Answer is Wrong!", "alert");
        myanswer = "wrong";

    }
    var length = data.PaintShapes.length;
    var fillcolor = (myanswer==="correct") ? OrthoVariables.ColorRight : OrthoVariables.ColorWrong;
    var strikecolor = (myanswer==="correct") ? OrthoVariables.ColorRightEdge : OrthoVariables.ColorWrongEdge;

    if(length > 0) {
        var mypage = OrthoVariables.LessonData.Page[OrthoVariables.lessonPage];
        var subid = (mypage.Widget[0].type === "compleximage") ? 0 : 1;
        var id = OrthoVariables.lessonPage.toString() + subid.toString();
        var mystage = OrthoVariables.origCanvas[id][2];
        var myshapelayer = mystage.get("#answerlayer")[0];
        myshapelayer.removeChildren();
        for(var i = 0; i < length; i++) {
            switch (data.PaintShapes[i][0]) {
                case "Circle":
                    myshapelayer.add(PaintCircle(data.PaintShapes[i],fillcolor,strikecolor));
                    break;
                case "Rect":
                    myshapelayer.add(PaintRect(data.PaintShapes[i],fillcolor,strikecolor));
                    break;
                case "Polygon":
                    myshapelayer.add(PaintPolygon(data.PaintShapes[i],fillcolor,strikecolor));
                    break;
                case 'Eclipse':
                    myshapelayer.add(PaintEclipse(data.PaintShapes[i],fillcolor,strikecolor));
                    break;
            }
        }
        myshapelayer.draw();
        var childrens = myshapelayer.getChildren();
        for(var i=0;i< childrens.length;i++){
            childrens[i].transitionTo({alpha:0.5,duration:2,easing:'ease-out'});
        }
        myshapelayer.draw();
    }
    PageTracking(myanswer,blocked);
    CheckReadyNextText(myanswer,blocked);
    RemoveOverlay();
}

function PageTracking(answer,blocked)
{
    if (answer=== "correct") {
        OrthoVariables.PageTracking[OrthoVariables.lessonPage].status = "correct";
        OrthoVariables.PageTracking[OrthoVariables.lessonPage].nextpass = true;
    } else {
        OrthoVariables.PageTracking[OrthoVariables.lessonPage].status = "wrong";
        OrthoVariables.PageTracking[OrthoVariables.lessonPage].grade = 0;
        OrthoVariables.PageTracking[OrthoVariables.lessonPage].nextpass =  (blocked === "yes") ? false : true;
    }
}

function RemoveOverlay() {
    $("#overlay").removeClass("waiting").addClass("overlay_hidden");
}

function PaintCircle(data,fillcolor,strokecolor) {
	var circle = new Kinetic.Circle({
		x : data[1]["X"],
		y : data[1]["Y"],
		radius : data[2],
		fill : fillcolor,
		stroke : strokecolor,
		strokeWidth : 1,
		alpha : 0
	});
   return circle;
}

function PaintEclipse(data,fillcolor,strokecolor) {
	var radx = data[2]["RadiusX"];
	var rady = data[2]["RadiusY"];
	var scalex = 0, scaley = 0, radius = 0;
	if(radx >= rady) {
		radius = radx;
		scalex = 1.0;
		scaley = rady / radx;
	} else {
		radius = rady;
		scaley = 1.0;
		scalex = radx / rady;
	}
	var eclipse = new Kinetic.Circle({
		x : data[1]["X"],
		y : data[1]["Y"],
		radius : radius,
		fill : fillcolor,
		stroke : strokecolor,
		strokeWidth : 1,
		alpha : 0
	});
	eclipse.setScale(scalex, scaley);
	return eclipse;
}

function PaintRect(data,fillcolor,strokecolor) {
	var rect = new Kinetic.Rect({
		x : data[1]["X"],
		y : data[1]["Y"],
		width : data[2],
		height : data[3],
		fill : fillcolor,
		stroke : strokecolor,
		strokeWidth : 1,
		alpha : 0
	});
	return rect;
}

function PaintPolygon(data,fillcolor,strokecolor) {
	var points = [];
	var len = (data.length - 1);
	for(var i = 0; i < len; i++) {
		points[i] = {
			x : parseInt(data[i+1]["X"]),
			y : parseInt(data[i+1]["Y"])
		};
	}
    var poly = new Kinetic.Polygon({
		points : points,
		fill : fillcolor,
		stroke : strokecolor,
		strokeWidth : 1,
		alpha : 0
	});
    return poly;
}

