<?php
 ob_start();
//session_start();
require_once('Display/fb.php');
//echo dirname(dirname(dirname('../lib.php'))).'/config.php';
//require_once(dirname(dirname(dirname('../lib.php'))).'/config.php');
require_once('../../config.php');
//echo dirname('../lib.php').'/lib.php';
require_once('lib.php');


$action = $_GET["action"];
// 1- transform xml to json

switch ($action) {
	case "1" :
		//$lessonid = $_GET["lessonid"];
		$xml = getXMLData();
		$displaydata = GetTemplateData($xml);
		//print_r($displaydata);
		echo json_encode($displaydata);
		break;
	case "2" :
		echo json_encode(GetAnswer());
		break;
}

function GetAnswer() {
	//sleep(2);
    $type = $_GET["type"];
	$return = null;
	switch ($type) {
		case 'quiz' :
			$return = GetQuizAnswer();
			break;
		case 'hotspots':
			$return = GetHotspotsAnswer();
			break;
        case 'input':
            $return = getInputAnswer();
            break;
		default :
			$return = "error";
			break;
	}
	return $return;
}

function GetQuizAnswer() {
    $return = array();
	$xml = getXMLData();
	$useranswer = $_GET["answer"];
	$Page = $_GET["Page"];
	$xmlquizanswer = GetQuizXMLData($Page, $xml);
    if ($xmlquizanswer === $useranswer) {
		$return["Answer"] = "correct";
	} else {
		$return["Answer"] = "wrong";
	}

    $isblocked = strval($xml->page[intval($Page)]["blocked"]);
    if ($isblocked === "yes" && $return["Answer"] === "wrong") {
        $return["PaintShapes"] = "";
        $return["CorrectAnswer"] = "";
    } else {
        $return["PaintShapes"] =GetShapesFromImage($Page, $xml);
        $return["CorrectAnswer"] = $xmlquizanswer;
    }
    return $return;
}

function getInputAnswer() {
    $return = array();
    $xml = getXMLData();
    $myvalue = intval((int)$_GET["value"]);
    $Page = $_GET["Page"];
    foreach ($xml->page[intval($Page)]->widget as $key=> $value) {
        if (strval($value["type"]) == "input") {
            $min = intval($value->input["minvalue"]);
            $max = intval($value->input["maxvalue"]);
        }
    }
    $isblocked = strval($xml->page[intval($Page)]["blocked"]);
    $return["Answer"] = ($myvalue >= $min && $myvalue <= $max ) ? "correct" : "wrong";

    return $return;
}

function GetHotspotsAnswer() {
	$useranswer = isset($_GET["answer"]) ? $_GET["answer"] : array();
	$return = array();
	$Page = $_GET["Page"];
    $xml = getXMLData();
    $myimg = GetHotSpotImage($Page,$xml);
    $result = true;
    $burnded = array();
    $fillcolors = array();
    for ($j=0;$j<count($myimg);$j++){
        $fillcolors[$j] = false;
    }
     $r = array();
    for ($i=0;$i< count($useranswer);$i++) {
        $r[$i] = false;
        $x = $useranswer[$i][0];
        $y = $useranswer[$i][1];
        for ($j=0;$j<count($myimg);$j++){
            if (!array_key_exists($j, $burnded)) {
                $colorint = imagecolorat($myimg[$j],$x,$y);
                if ($colorint === 1) {$r[$i] =true; $burnded[]=$j; $fillcolors[$j] = true;break;}
            }
        }
        if ($r[$i]===false){
            $result=false;
            //break;
        }
    }

    foreach($r as $rs){
        if ($rs === false) {$result = false;break;}
    }

    foreach($myimg as &$img){
        imagedestroy($img);
    }

	$return["Answer"] = $result && count($useranswer) > 0 ? "correct" : "wrong";
    $isblocked = strval($xml->page[intval($Page)]["blocked"]);
    $return["PaintShapes"] = ($isblocked === "yes" && $return["Answer"] === "wrong") ? "" : GetShapesFromImage($Page, $xml);
    $return["Fill"] = $fillcolors;
    return $return;
}

function CheckImageBurned($j, $burned)
{
    $r = false;
    foreach($burned as $b){
        if ($j===$b) { }
    }
}


function GetHotSpotImage($PageID,$xml){
    $quizimage = null;
    foreach ($xml->page[intval($PageID)]->widget as $key => $value) {
        if (strval($value["type"]) == "image") {
            $quizimage = $value;
        }
    }
    //initialize image
    $width = strval($quizimage->image["width"]);
    $height =strval($quizimage->image["height"]);
    $myimg = array();
    //fb($quizimage);
    if (isset($quizimage -> image["hotspots"])) {
        if (strval($quizimage -> image["hotspots"]) === "yes") {
            $return = array();
            foreach (get_object_vars($quizimage->image) as $key => $value) {
                $tempimg = imagecreate($width,$height);
                $white = imagecolorallocate($tempimg,255,255,255);
                $black = imagecolorallocate($tempimg,0,0,0);
                imagefill($tempimg,0,0,$white);
                switch ($key) {
                    case 'circle' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                PaintCircle($tempimg,$bvalue,$black);
                            }
                        } else {
                            PaintCircle($tempimg,$value,$black);
                        }
                        $myimg[] = $tempimg;
                        break;
                    case 'rect' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                PaintRect($tempimg,$bvalue,$black);
                           }
                        } else {
                            PaintRect($tempimg,$value,$black);
                        }
                        $myimg[] = $tempimg;
                        break;
                    case 'polygon' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                PaintPolygon($tempimg,$bvalue,$black);
                            }
                        } else {
                            PaintPolygon($tempimg,$value,$black);
                        }
                        $myimg[] = $tempimg;
                        break;
                    case 'eclipse' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                PaintEclipse($tempimg,$bvalue,$black);
                            }
                        } else {
                            PaintEclipse($tempimg,$value,$black);

                        }
                        $myimg[] = $tempimg;
                        break;
                }
                //header('Content-Type: image/png');
                //imagepng($tempimg);

                //header('Content-Type: image/png');
                //imagepng($myimg[0]);

            }
        }
    }
    return $myimg;
}

function GetShapesFromImage($PageID, $xml) {
	$quizimage = null;
	foreach ($xml->page[intval($PageID)]->widget as $key => $value) {
		if (strval($value["type"]) === "image") {
			$quizimage = $value;
		}
	}
	$return = "";
	if (isset($quizimage -> image["showregions"])) {
		if (strval($quizimage -> image["showregions"]) === "yes") {
			$return = array();
			foreach (get_object_vars($quizimage->image) as $key => $value) {
				switch ($key) {
					case 'circle' :
						if (is_array($value)) {
							foreach ($value as $bkey => $bvalue) {
								$return[] = GetCircle($bvalue);
							}
						} else {
							$return[] = GetCircle($value);
						}
						break;
					case 'rect' :
						if (is_array($value)) {
							foreach ($value as $bkey => $bvalue) {
								$return[] = GetRect($bvalue);
							}
						} else {
							$return[] = GetRect($value);
						}
						break;
					case 'polygon' :
						if (is_array($value)) {
							foreach ($value as $bkey => $bvalue) {
								$return[] = GetPolygon($bvalue);
							}
						} else {
							$return[] = GetPolygon($value);
						}
						break;
					case 'eclipse' :
						if (is_array($value)) {
							foreach ($value as $bkey => $bvalue) {
								$return[] = GetEclipse($bvalue);
							}
						} else {
							$return[] = GetEclipse($value);
						}
						break;
				}
			}
		}
	}
	return $return;

}

function GetCircle($data) {
	$circle = array();
	$circle[0] = "Circle";
	$circle[1] = array("X" => strval($data -> center["x"]), "Y" => strval($data -> center["y"]));
	$circle[2] = strval($data["radius"]);
	return $circle;
}

function PaintCircle($imageone, $data, $color) {
	imagefilledellipse($imageone, strval($data -> center["x"]), strval($data -> center["y"]), 2*intval($data["radius"]), 2*intval($data["radius"]), $color);
	return $imageone;
}

function GetRect($data) {
	$rect = array();
	$rect[0] = "Rect";
	$rect[1] = array("X" => strval($data -> point["x"]), "Y" => strval($data -> point["y"]));
	$rect[2] = strval($data -> Width);
	$rect[3] = strval($data -> Height);
	return $rect;
}

function PaintRect($imageone, $data, $color) {
	imagefilledrectangle($imageone, strval($data -> point["x"]), strval($data -> point["y"]), strval($data -> point["x"]) + strval($data -> width), strval($data -> point["y"]) + strval($data -> height), $color);
	return $imageone;
}

function GetPolygon($data) {
	$rect = array();
	$rect[0] = "Polygon";
	$count = count($data -> point);
	for ($i = 0; $i < $count; $i++) {
		$rect[$i + 1] = array("X" => strval($data -> point[$i]["x"]), "Y" => strval($data -> point[$i]["y"]));
	}
	return $rect;
}

function PaintPolygon($imageone, $data, $color) {
	$count = count($data -> point);
	$points = array();
	for ($i = 0; $i < $count; $i++) {
		$points[] = strval($data -> point[$i]["x"]);
		$points[] = strval($data -> point[$i]["y"]);
	}
	imagefilledpolygon($imageone, $points, $count, $color);
	return $imageone;
}

function GetEclipse($data) {
	$eclipse = array();
	$eclipse[0] = "Eclipse";
	$eclipse[1] = array("X" => strval($data -> center["x"]), "Y" => strval($data -> center["y"]));
	$eclipse[2] = array("RadiusX" => strval($data["radiusx"]), "RadiusY" => strval($data["radiusy"]));
	return $eclipse;
}

function PaintEclipse($imageone, $data, $color) {
	imagefilledellipse($imageone, strval($data -> center["x"]),  intval($data -> center["y"]), 2*intval($data["radiusx"]), 2*intval($data["radiusy"]), $color);
	return $imageone;
}

function GetQuizXMLData($PageID, $xml) {
	$quizwidget = null;
	foreach ($xml->page[intval($PageID)]->widget as $key => $value) {
		if (strval($value["type"]) == "quiz") {
			$quizwidget = $value;
		}
	}
	$answer = "";
	$counter = 0;
	foreach ($quizwidget->answer as $key => $value) {
		if (strval($value["iscorrect"]) === "yes") {
			$answer .= ";".strval($counter);
		}
		$counter++;
	}
    return $answer;
}

function oldGetXMLData() {
	$filename = $_GET["name"];
    //$xml = simplexml_load_file("../Content/XML/".$filename.".xml");
	return $xml;
}

function getXMLData(){
	if (isset($_GET['old'])) return oldGetXMLData();
	$orthoeman_id = isset($_GET['orthoeman_id'])? (int)$_GET['orthoeman_id'] : -1;
	echo $orthoeman_id;
	$resource = get_database_data($orthoeman_id,-1);
	echo ($resource->data);
	//return simplexml_load_file(filename);
	return $resource->data;
}

function GetTemplateData($data) {
	$a = array();
	$a["attributes"]["id"] = strval($data["id"]);
	$a["attributes"]["Title"] = strval($data["title"]);
	$a["attributes"]["abstract"] = strval($data ->abstract);
	$index = 0;
	foreach ($data->page as $key => $value) {
		$a["Page"][$index]["attributes"]["Grade"] = strval($value["grade"]);
		$a["Page"][$index]["attributes"]["Title"] = strval($value["title"]);
        $a["Page"][$index]["attributes"]["Blocked"] = strval($value["blocked"]);
		$windex = 0;
		foreach ($value->widget as $wkey => $wvalue) {
			$widgetype = strval($wvalue["type"]);
			$a["Page"][$index]["Widget"][$windex]["type"] = $widgetype;
			switch ($widgetype) {
				case 'image' :
					$a["Page"][$index]["Widget"][$windex]["Image"] = GetDisplayComplexImg($wvalue -> image, $index , $windex);
					$a["Images"][] = array('id' => $index,'subid' => $windex,
                        'url' => strval($wvalue -> image -> imageuri),
                        'HotSpots' => strval($wvalue -> image["hotspots"]),
                        'MaxSpots'=>strval($wvalue->image["maxspots"]),
                        'ShowRegions' => strval($wvalue -> image["showregions"]),
                        'EnableTracking' => strval($wvalue -> image["enabletracking"])
                    );
					break;
				case 'quiz' :
					$a["Page"][$index]["Widget"][$windex]["Quiz"] = GetDisplayQuizImg($wvalue, $index , $windex);
					break;
				case 'text' :
					$a["Page"][$index]["Widget"][$windex]["Text"] = GetDisplayText($wvalue -> text);
					break;
                case 'video':
                    $a["Page"][$index]["Widget"][$windex]["Video"] = GetDisplayVideo($wvalue-> video, $index,$windex);
                    break;
                case "input" :
                    $a["Page"][$index]["Widget"][$windex]["Input"] = GetDisplayInput($wvalue->input,$index,$windex);
                    break;
			}
			$windex++;
		}
		$index++;
	}
	return $a;
}

function GetDisplayInput($data,$id,$subid) {
   $r = array();
    $r["Question"] = strval($data -> question);
    $r["id"] = $id;
    $r["subid"] = $subid;
    $min = (int)$data["minvalue"];
    $max = (int)$data["maxvalue"];
    $r["Min"] = ($min >=0 ) ? 0 : $min -  mt_rand(10,2* abs($min));
    $r["Max"] = $max + mt_rand(10,2*$max);
    return $r;
}
function GetDisplayVideo($data, $id, $subid) {
    $r = array();
    if (isset($data->videoid)) {
        $r["VideoID"] = strval($data->videoid);
        $r["LoadMethod"] = "ID";
    } else {
        $r["VideoURI"] = strval($data-> videouri);
        $r["LoadMethod"] = "URI";
    }
    $r["id"] = $id;
    $r["subid"] = $subid;
    $r["video_mp4"] = strval($data["video_mp4"]);
    $r["video_webm"] = strval($data["video_webm"]);
    $r["video_ogg"] = strval($data["video_ogg"]);
    return $r;
}

function GetDisplayComplexImg($data, $id ,$subid) {
	$r = array();
	if (isset($data -> imageid)) {
		$r["ImageID"] = strval($data -> imageid);
		$r["LoadMethod"] = "ID";
	} else {
		$r["ImageURI"] = strval($data -> imageuri);
		$r["LoadMethod"] = "URI";
	}

	$r["ShowRegions"] = strval($data["showregions"]);
	$r["HotSpots"] = strval($data["hotspots"]);
    $r["EnableTracking"] = strval($data["enabletracking"]);
	$r["id"] = $id;
    $r["subid"] = $subid;
	$r["width"] = strval($data["width"]);
	$r["height"] = strval($data["height"]);
	$r["ImageType"] = (count(get_object_vars($data)) > 2) ? "complex" : "simple";
	return $r;
}

function GetDisplayQuizImg($data, $id,$subid) {
	$r = array();
	$r["Question"] = strval($data -> question);
	$r["id"] = $id;
    $r["subid"] = $subid;
	$count = count($data -> answer);
	for ($i = 0; $i < $count; $i++)
		$r["Answer"][$i] = strval($data -> answer[$i]);
	return $r;
}

function GetDisplayText($data) {
	return strval($data);
}
?>