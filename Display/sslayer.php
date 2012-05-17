<?php
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
	$return["PaintShapes"] = InvestigateQuizImage($Page, $xml);
	return $return;
}

function GetHotspotsAnswer() {
	$useranswer = isset($_GET["answer"]) ? $_GET["answer"] : array();
	$return = array();
	$Page = $_GET["Page"];
    $xml = GetXMLData();
    $myimg = GetHotSpotImage($Page,$xml);
    //header('Content-Type: image/png');
    //imagepng($myimg);
    $result = true;
    for ($i=0;$i< count($useranswer);$i++) {
       $colorint = imagecolorat($myimg,$useranswer[$i][0],$useranswer[$i][1]);
        if ($colorint === 0) {$result=false;}
    }
    imagedestroy($myimg);
	$return["Answer"] = $result && count($useranswer) > 0 ? "correct" : "wrong";
    return $return;
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

    $myimg = imagecreate($width,$height);
    $white = imagecolorallocate($myimg,255,255,255);
    $black = imagecolorallocate($myimg,0,0,0);
    imagefill($myimg,0,0,$white);
    if (isset($quizimage -> Image["HotSpots"])) {
        if (strval($quizimage -> Image["HotSpots"]) === "yes") {
            $return = array();
            foreach (get_object_vars($quizimage->Image) as $key => $value) {

                switch ($key) {
                    case 'Circle' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                PaintCircle($myimg,$bvalue,$black);
                            }
                        } else {
                            PaintCircle($myimg,$value,$black);
                        }
                        break;
                    case 'Rect' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                PaintRect($myimg,$bvalue,$black);
                           }
                        } else {
                            PaintRect($myimg,$value,$black);
                        }
                        break;
                    case 'Polygon' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                PaintPolygon($myimg,$bvalue,$black);
                            }
                        } else {
                            PaintPolygon($myimg,$value,$black);
                        }
                        break;
                    case 'Eclipse' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                PaintEclipse($myimg,$bvalue,$black);
                            }
                        } else {
                            PaintEclipse($myimg,$value,$black);
                        }
                        break;
                }
            }
        }
    }
    return $myimg;
}

function InvestigateQuizImage($PageID, $xml) {
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
	imagefilledellipse($imageone, strval($data -> Center["X"]), strval($data -> Center["Y"]), strval($data["Radius"]), strval($data["Radius"]), $color);
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
	imagefilledellipse($imageone, strval($data -> Center["X"]), strval($data -> Center["Y"]), strval($data["RadiusX"]), strval($data["RadiusY"]), $color);
	return $imageone;
}

function GetQuizXMLData($PageID, $xml) {
	$quizwidget = null;
	foreach ($xml->Page[intval($PageID)]->Widget as $key => $value) {
		if (strval($value["type"]) == "quiz") {
			$quizwidget = $value;
		}
	}
	//print_r($quizwidget);
	$answer = "";
	$counter = 0;
	foreach ($quizwidget->Answer as $key => $value) {
		if (strval($value["IsCorrect"]) === "yes") {
			$answer .= strval($counter);
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
		$windex = 0;
		foreach ($value->Widget as $wkey => $wvalue) {
			$widgetype = strval($wvalue["type"]);
			$a["Page"][$index]["Widget"][$windex]["type"] = $widgetype;
			switch ($widgetype) {
				case 'compleximage' :
					$a["Page"][$index]["Widget"][$windex]["Image"] = GetDisplayComplexImg($wvalue -> Image, $index . $windex);
					$a["Images"][] = array('id' => $index . $windex, 'url' => strval($wvalue -> Image -> ImageURI), 'HotSpots' => strval($wvalue -> Image["HotSpots"]), 'ShowRegions' => strval($wvalue -> Image["ShowRegions"]));
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