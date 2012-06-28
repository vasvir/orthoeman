<?php
 ob_start();
require_once('fb.php');
session_start();


$action = $_GET["action"];
// 1- transform xml to json

switch ($action) {
	case "1" :
		//$lessonid = $_GET["lessonid"];
		$xml = GetXMLData();
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
		default :
			$return = "error";
			break;
	}
	return $return;
}

function GetQuizAnswer() {
    $return = array();
	$xml = GetXMLData();
	$useranswer = $_GET["answer"];
	$Page = $_GET["Page"];
	$xmlquizanswer = GetQuizXMLData($Page, $xml);
    if ($xmlquizanswer === $useranswer) {
		$return["Answer"] = "correct";
	} else {
		$return["Answer"] = "wrong";
	}

    $isblocked = strval($xml->Page[intval($Page)]["Blocked"]);
    if ($isblocked === "yes" && $return["Answer"] === "wrong") {
        $return["PaintShapes"] = "";
        $return["CorrectAnswer"] = "";
    } else {
        $return["PaintShapes"] =GetShapesFromImage($Page, $xml);
        $return["CorrectAnswer"] = $xmlquizanswer;
    }
    return $return;
}

function GetHotspotsAnswer() {
	$useranswer = isset($_GET["answer"]) ? $_GET["answer"] : array();
	$return = array();
	$Page = $_GET["Page"];
    $xml = GetXMLData();
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
    $isblocked = strval($xml->Page[intval($Page)]["Blocked"]);
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
    foreach ($xml->Page[intval($PageID)]->Widget as $key => $value) {
        if (strval($value["type"]) == "compleximage") {
            $quizimage = $value;
        }
    }
    //initialize image
    $width = strval($quizimage->Image["width"]);
    $height =strval($quizimage->Image["height"]);
    $myimg = array();
    //fb($quizimage);
    if (isset($quizimage -> Image["HotSpots"])) {
        if (strval($quizimage -> Image["HotSpots"]) === "yes") {
            $return = array();
            foreach (get_object_vars($quizimage->Image) as $key => $value) {
                $tempimg = imagecreate($width,$height);
                $white = imagecolorallocate($tempimg,255,255,255);
                $black = imagecolorallocate($tempimg,0,0,0);
                imagefill($tempimg,0,0,$white);
                switch ($key) {
                    case 'Circle' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                PaintCircle($tempimg,$bvalue,$black);
                            }
                        } else {
                            PaintCircle($tempimg,$value,$black);
                        }
                        $myimg[] = $tempimg;
                        break;
                    case 'Rect' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                PaintRect($tempimg,$bvalue,$black);
                           }
                        } else {
                            PaintRect($tempimg,$value,$black);
                        }
                        $myimg[] = $tempimg;
                        break;
                    case 'Polygon' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                PaintPolygon($tempimg,$bvalue,$black);
                            }
                        } else {
                            PaintPolygon($tempimg,$value,$black);
                        }
                        $myimg[] = $tempimg;
                        break;
                    case 'Eclipse' :
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
	foreach ($xml->Page[intval($PageID)]->Widget as $key => $value) {
		if (strval($value["type"]) == "compleximage") {
			$quizimage = $value;
		}
	}
	$return = "";
	if (isset($quizimage -> Image["ShowRegions"])) {
		if (strval($quizimage -> Image["ShowRegions"]) === "yes") {
			$return = array();
			foreach (get_object_vars($quizimage->Image) as $key => $value) {
				switch ($key) {
					case 'Circle' :
						if (is_array($value)) {
							foreach ($value as $bkey => $bvalue) {
								$return[] = GetCircle($bvalue);
							}
						} else {
							$return[] = GetCircle($value);
						}
						break;
					case 'Rect' :
						if (is_array($value)) {
							foreach ($value as $bkey => $bvalue) {
								$return[] = GetRect($bvalue);
							}
						} else {
							$return[] = GetRect($value);
						}
						break;
					case 'Polygon' :
						if (is_array($value)) {
							foreach ($value as $bkey => $bvalue) {
								$return[] = GetPolygon($bvalue);
							}
						} else {
							$return[] = GetPolygon($value);
						}
						break;
					case 'Eclipse' :
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
	$circle[1] = array("X" => strval($data -> Center["X"]), "Y" => strval($data -> Center["Y"]));
	$circle[2] = strval($data["Radius"]);
	return $circle;
}

function PaintCircle($imageone, $data, $color) {
	imagefilledellipse($imageone, strval($data -> Center["X"]), strval($data -> Center["Y"]), 2*intval($data["Radius"]), 2*intval($data["Radius"]), $color);
	return $imageone;
}

function GetRect($data) {
	$rect = array();
	$rect[0] = "Rect";
	$rect[1] = array("X" => strval($data -> Point["X"]), "Y" => strval($data -> Point["Y"]));
	$rect[2] = strval($data -> Width);
	$rect[3] = strval($data -> Height);
	return $rect;
}

function PaintRect($imageone, $data, $color) {
	imagefilledrectangle($imageone, strval($data -> Point["X"]), strval($data -> Point["Y"]), strval($data -> Point["X"]) + strval($data -> Width), strval($data -> Point["Y"]) + strval($data -> Height), $color);
	return $imageone;
}

function GetPolygon($data) {
	$rect = array();
	$rect[0] = "Polygon";
	$count = count($data -> Point);
	for ($i = 0; $i < $count; $i++) {
		$rect[$i + 1] = array("X" => strval($data -> Point[$i]["X"]), "Y" => strval($data -> Point[$i]["Y"]));
	}
	return $rect;
}

function PaintPolygon($imageone, $data, $color) {
	$count = count($data -> Point);
	$points = array();
	for ($i = 0; $i < $count; $i++) {
		$points[] = strval($data -> Point[$i]["X"]);
		$points[] = strval($data -> Point[$i]["Y"]);
	}
	imagefilledpolygon($imageone, $points, $count, $color);
	return $imageone;
}

function GetEclipse($data) {
	$eclipse = array();
	$eclipse[0] = "Eclipse";
	$eclipse[1] = array("X" => strval($data -> Center["X"]), "Y" => strval($data -> Center["Y"]));
	$eclipse[2] = array("RadiusX" => strval($data["RadiusX"]), "RadiusY" => strval($data["RadiusY"]));
	return $eclipse;
}

function PaintEclipse($imageone, $data, $color) {
	imagefilledellipse($imageone, strval($data -> Center["X"]),  intval($data -> Center["Y"]), 2*intval($data["RadiusX"]), 2*intval($data["RadiusY"]), $color);
	return $imageone;
}

function GetQuizXMLData($PageID, $xml) {
	$quizwidget = null;
	foreach ($xml->Page[intval($PageID)]->Widget as $key => $value) {
		if (strval($value["type"]) == "quiz") {
			$quizwidget = $value;
		}
	}
	$answer = "";
	$counter = 0;
	foreach ($quizwidget->Answer as $key => $value) {
		if (strval($value["IsCorrect"]) === "yes") {
			$answer .= ";".strval($counter);
		}
		$counter++;
	}
    return $answer;
}

function GetXMLData() {
	$filename = $_GET["name"];
    $xml = simplexml_load_file("../Content/XML/".$filename.".xml");
	return $xml;
}

function GetTemplateData($data) {
	$a = array();
	$a["attributes"]["id"] = strval($data["id"]);
	$a["attributes"]["Title"] = strval($data["Title"]);
	$a["attributes"]["abstract"] = strval($data ->abstract);
	$index = 0;
	foreach ($data->Page as $key => $value) {
		$a["Page"][$index]["attributes"]["Grade"] = strval($value["Grade"]);
		$a["Page"][$index]["attributes"]["Title"] = strval($value["Title"]);
        $a["Page"][$index]["attributes"]["Blocked"] = strval($value["Blocked"]);
		$windex = 0;
		foreach ($value->Widget as $wkey => $wvalue) {
			$widgetype = strval($wvalue["type"]);
			$a["Page"][$index]["Widget"][$windex]["type"] = $widgetype;
			switch ($widgetype) {
				case 'compleximage' :
					$a["Page"][$index]["Widget"][$windex]["Image"] = GetDisplayComplexImg($wvalue -> Image, $index . $windex);
					$a["Images"][] = array('id' => $index . $windex, 'url' => strval($wvalue -> Image -> ImageURI), 'HotSpots' => strval($wvalue -> Image["HotSpots"]),'MaxSpots'=>strval($wvalue->Image["MaxSpots"]), 'ShowRegions' => strval($wvalue -> Image["ShowRegions"]));
					break;
					;
				case 'quiz' :
					$a["Page"][$index]["Widget"][$windex]["Quiz"] = GetDisplayQuizImg($wvalue, $index . $windex);
					break;
				case 'text' :
					$a["Page"][$index]["Widget"][$windex]["Text"] = GetDisplayText($wvalue -> Text);

					break;
			}
			$windex++;
		}
		$index++;
	}
	return $a;
}

function GetDisplayComplexImg($data, $id) {
	$r = array();
	if (isset($data -> ImageID)) {
		$r["ImageID"] = strval($data -> ImageID);
		$r["LoadMethod"] = "ID";
	} else {
		$r["ImageURI"] = strval($data -> ImageURI);
		$r["LoadMethod"] = "URI";
	}

	$r["ShowRegions"] = strval($data["ShowRegions"]);
	$r["HotSpots"] = strval($data["HotSpots"]);
	$r["id"] = $id;
	$r["width"] = strval($data["width"]);
	$r["height"] = strval($data["height"]);
	$r["ImageType"] = (count(get_object_vars($data)) > 2) ? "complex" : "simple";
	return $r;
}

function GetDisplayQuizImg($data, $id) {
	$r = array();
	$r["Question"] = strval($data -> Question);
	$r["id"] = $id;
	$count = count($data -> Answer);
	for ($i = 0; $i < $count; $i++)
		$r["Answer"][$i] = strval($data -> Answer[$i]);
	return $r;
}

function GetDisplayText($data) {
	return strval($data);
}
?>