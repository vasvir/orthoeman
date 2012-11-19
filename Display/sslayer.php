<?php
ob_start();
//session_start();
require_once('fb.php');
require_once('../../../config.php');
require_once('../lib.php');

$orthoeman_id = optional_param('orthoeman_id', 0, PARAM_INT); // course_module ID, or
$old = 0;

$action = $_GET["action"];
// 1- transform xml to json

switch ($action) {
    case "1" :
        //$lessonid = $_GET["lessonid"];
        $xml = getXMLData();
        $displaydata = GetTemplateData($xml);
        //print_r($displaydata);
        //Here is definding a dummy tracking 
        //$displaydata["Tracking"] = getTracking();
        echo json_encode($displaydata);
        break;
    case "2" :
        echo json_encode(GetAnswer());
        break;
}

function GetAnswer()
{
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

function getTracking()
{
    $r[0]["Type"] = 1;
    $r[0]["Hotspots"][0]["x"] = 70;
    $r[0]["Hotspots"][0]["y"] = 40;
    $r[0]["Result"] = false;
    return $r;
}

function GetQuizAnswer()
{
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

    $isblocked = strval($xml->Page[intval($Page)]["block"]);
    if ($isblocked === "yes" && $return["Answer"] === "wrong") {
        $return["PaintShapes"] = "";
        $return["CorrectAnswer"] = "";
    } else {
        $return["PaintShapes"] = GetShapesFromImage($Page, $xml);
        $return["CorrectAnswer"] = $xmlquizanswer;
    }
    return $return;
}

function getInputAnswer()
{
    $return = array();
    $xml = getXMLData();
    $myvalue = intval((int)$_GET["value"]);
    $Page = $_GET["Page"];
    foreach ($xml->Page[intval($Page)] as $key => $value) {
        if ($key === "RangeQuiz") {
            $min = intval($value["minValue"]);
            $max = intval($value["maxValue"]);
        }
    }
    $isblocked = strval($xml->Page[intval($Page)]["block"]);
    $return["Answer"] = ($myvalue >= $min && $myvalue <= $max) ? "correct" : "wrong";

    return $return;
}

function GetHotspotsAnswer()
{
    $useranswer = isset($_GET["answer"]) ? $_GET["answer"] : array();
    $return = array();
    $Page = $_GET["Page"];
    $xml = getXMLData();
    $myimg = GetHotSpotImage($Page, $xml);
    $result = true;
    $burnded = array();
    $fillcolors = array();
    for ($j = 0; $j < count($myimg); $j++) {
        $fillcolors[$j] = false;
    }
    $r = array();
    for ($i = 0; $i < count($useranswer); $i++) {
        $r[$i] = false;
        $x = $useranswer[$i][0];
        $y = $useranswer[$i][1];
        for ($j = 0; $j < count($myimg); $j++) {
            if (!array_key_exists($j, $burnded)) {
                $colorint = imagecolorat($myimg[$j], $x, $y);
                if ($colorint === 1) {
                    $r[$i] = true;
                    $burnded[] = $j;
                    $fillcolors[$j] = true;
                    break;
                }
            }
        }
        if ($r[$i] === false) {
            $result = false;
            //break;
        }
    }

    foreach ($r as $rs) {
        if ($rs === false) {
            $result = false;
            break;
        }
    }

    foreach ($myimg as &$img) {
        imagedestroy($img);
    }

    $return["Answer"] = $result && count($useranswer) > 0 ? "correct" : "wrong";
    $isblocked = strval($xml->Page[intval($Page)]["block"]);
    $return["PaintShapes"] = ($isblocked === "yes" && $return["Answer"] === "wrong") ? "" : GetShapesFromImage($Page, $xml);
    $return["Fill"] = $fillcolors;
    return $return;
}

function GetHotSpotImage($PageID, $xml)
{
    $quizimage = null;
    foreach ($xml->Page[intval($PageID)] as $key => $value) {

        if ($key === "Image") {
            $quizimage = $value;
        }
    }

    //initialize image
    $width = strval($quizimage["width"]);
    $height = strval($quizimage["height"]);
    $myimg = array();
    //fb($quizimage);
    $hotspotsNumber = getHotSpotsNumber($quizimage);
    if ($hotspotsNumber > 0) {
        $return = array();
        foreach (get_object_vars($quizimage) as $key => $value) {
            if ($key != "@attributes") {
                $tempimg = imagecreate($width, $height);
                $white = imagecolorallocate($tempimg, 255, 255, 255);
                $black = imagecolorallocate($tempimg, 0, 0, 0);
                imagefill($tempimg, 0, 0, $white);
                switch ($key) {
                    case 'Circle' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                PaintCircle($tempimg, $bvalue, $black);
                            }
                        } else {
                            PaintCircle($tempimg, $value, $black);
                        }
                        $myimg[] = $tempimg;
                        break;
                    case 'Rectangle' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                PaintRect($tempimg, $bvalue, $black);
                            }
                        } else {
                            PaintRect($tempimg, $value, $black);
                        }
                        $myimg[] = $tempimg;
                        break;
                    case 'Polygon' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                PaintPolygon($tempimg, $bvalue, $black);
                            }
                        } else {
                            PaintPolygon($tempimg, $value, $black);
                        }
                        $myimg[] = $tempimg;
                        break;
                    case 'Ellipse' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                PaintEclipse($tempimg, $bvalue, $black);
                            }
                        } else {
                            PaintEclipse($tempimg, $value, $black);

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

function GetShapesFromImage($PageID, $xml)
{
    $quizimage = null;
    foreach ($xml->Page[intval($PageID)] as $key => $value) {
        if ($key === "Image") {
            $quizimage = $value;
        }
    }
    $return = "";
    if (isset($quizimage["showRegions"])) {
        if (strval($quizimage["showRegions"]) === "yes") {
            $return = array();
            foreach (get_object_vars($quizimage) as $key => $value) {
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
                    case 'Rectangle' :
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
                    case 'Ellipse' :
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

function GetCircle($data)
{
    $circle = array();
    $circle[0] = "Circle";
    $circle[1] = array("X" => strval($data->Center["x"]), "Y" => strval($data->Center["y"]));
    $circle[2] = strval($data["radius"]);
    return $circle;
}

function PaintCircle($imageone, $data, $color)
{
    imagefilledellipse($imageone, strval($data->Center["x"]), strval($data->Center["y"]), 2 * intval($data["radius"]), 2 * intval($data["radius"]), $color);
    return $imageone;
}

function GetRect($data)
{
    $rect = array();
    $rect[0] = "Rect";
    $rect[1] = array("X" => strval($data->Point["x"]), "Y" => strval($data->Point["y"]));
    $rect[2] = strval($data["width"]);
    $rect[3] = strval($data["height"]);
    return $rect;
}

function PaintRect($imageone, $data, $color)
{
    imagefilledrectangle($imageone, strval($data->Point["x"]), strval($data->Point["y"]), strval($data->Point["x"]) + strval($data["width"]), strval($data->Point["y"]) + strval($data["height"]), $color);
    return $imageone;
}

function GetPolygon($data)
{
    $rect = array();
    $rect[0] = "Polygon";
    $count = count($data->Point);
    for ($i = 0; $i < $count; $i++) {
        $rect[$i + 1] = array("X" => strval($data->Point[$i]["x"]), "Y" => strval($data->Point[$i]["y"]));
    }
    return $rect;
}

function PaintPolygon($imageone, $data, $color)
{
    $count = count($data->Point);
    $points = array();
    for ($i = 0; $i < $count; $i++) {
        $points[] = strval($data->Point[$i]["x"]);
        $points[] = strval($data->Point[$i]["y"]);
    }
    imagefilledpolygon($imageone, $points, $count, $color);
    return $imageone;
}

function GetEclipse($data)
{
    $eclipse = array();
    $eclipse[0] = "Eclipse";
    $eclipse[1] = array("X" => strval($data->Center["x"]), "Y" => strval($data->Center["y"]));
    $eclipse[2] = array("RadiusX" => strval($data["radiusX"]), "RadiusY" => strval($data["radiusY"]));
    return $eclipse;
}

function PaintEclipse($imageone, $data, $color)
{
    imagefilledellipse($imageone, strval($data->Center["x"]), intval($data->Center["y"]), 2 * intval($data["radiusX"]), 2 * intval($data["radiusY"]), $color);
    return $imageone;
}

function GetQuizXMLData($PageID, $xml)
{
    $quizwidget = null;
    foreach ($xml->Page[intval($PageID)] as $key => $value) {
        if ($key === "Quiz") {
            $quizwidget = $value;
        }
    }
    $answer = "";
    $counter = 0;
    foreach ($quizwidget->Answer as $key => $value) {
        if (strval($value["isCorrect"]) === "yes") {
            $answer .= ";" . strval($counter);
        }
        $counter++;
    }
    return $answer;
}

function oldGetXMLData()
{
    $filename = $_GET["name"];
    $xml = simplexml_load_file("../Content/XML/" . $filename . ".xml");
    return $xml;
}

function getXMLData()
{
    global $old;
    if ($old != 0) return oldGetXMLData();
    global $DB;
    $id = optional_param('orthoeman_id', 0, PARAM_INT); // course_module ID, or
    $cm = get_coursemodule_from_id('orthoeman', $id, 0, false, MUST_EXIST);
    $course = $DB->get_record('course', array('id' => $cm->course), '*', MUST_EXIST);
    $orthoeman = $DB->get_record('orthoeman', array('id' => $cm->instance), '*', MUST_EXIST);
    $resource = get_database_data($orthoeman->id, -1);
    //return simplexml_load_file(filename);
    //print_r(simplexml_load_string($resource->data));
    //echo($resource->data);
    return simplexml_load_string($resource->data);
}

function getWidgetType($key)
{
    $type = "";
    switch ($key) {
        case "Image":
            $type = "image";
            break;
        case "Text":
            $type = "text";
            break;
        case "Quiz":
            $type = "quiz";
            break;
        case "Video":
            $type = "video";
            break;
        case "RangeQuiz":
            $type = "input";
            break;
    }
    return $type;
}

function GetTemplateData($data)
{
    global $orthoeman_id;
    $a = array();
    //$a["attributes"]["id"] = strval($data["id"]);
    $a["attributes"]["Title"] = strval($data["title"]);
    $a["attributes"]["abstract"] = strval($data->Abstract);
    $index = 0;
    foreach ($data->Page as $key => $value) {
        $a["Page"][$index]["attributes"]["Grade"] = strval($value["positiveGrade"]);
        $a["Page"][$index]["attributes"]["negativeGrade"] = strval($value["negativeGrade"]);
        $a["Page"][$index]["attributes"]["Title"] = strval($value["title"]);
        $a["Page"][$index]["attributes"]["Blocked"] = strval($value["block"]);
        $windex = 0;
        foreach ($value as $wkey => $wvalue) {
            $widgetype = getWidgetType($wkey);
            $a["Page"][$index]["Widget"][$windex]["type"] = $widgetype;
            switch ($widgetype) {
                case 'image' :
                    $a["Page"][$index]["Widget"][$windex]["Image"] = GetDisplayComplexImg($wvalue, $index, $windex);
                    $a["Images"][] = array('id' => $index, 'subid' => $windex,
                        //'url' => $a["Page"][$index]["Widget"][$windex]["Image"]["ImageURI"],
                        'url' => '../get_resource.php?id='.$orthoeman_id.'&resource_id='.$a["Page"][$index]["Widget"][$windex]["Image"]["ImageURI"],
                        'HotSpots' => $a["Page"][$index]["Widget"][$windex]["Image"]["HotSpots"],
                        'MaxSpots' => $a["Page"][$index]["Widget"][$windex]["Image"]["MaxSpots"],
                        'ShowRegions' => $a["Page"][$index]["Widget"][$windex]["Image"]["ShowRegions"],
                        'EnableTracking' => $a["Page"][$index]["Widget"][$windex]["Image"]["EnableTracking"]
                    );
                    break;
                case 'quiz' :
                    $a["Page"][$index]["Widget"][$windex]["Quiz"] = GetDisplayQuizImg($wvalue, $index, $windex);
                    break;
                case 'text' :
                    $a["Page"][$index]["Widget"][$windex]["Text"] = GetDisplayText($wvalue);
                    break;
                case 'video':
                    $a["Page"][$index]["Widget"][$windex]["Video"] = GetDisplayVideo($wvalue, $index, $windex);
                    break;
                case "input" :
                    $a["Page"][$index]["Widget"][$windex]["Input"] = GetDisplayInput($wvalue, $index, $windex);
                    break;
            }
            $windex++;
        }
        $index++;
    }
    return $a;
}

function GetDisplayInput($data, $id, $subid)
{
    $r = array();
    $r["Question"] = strval($data->Question);
    $r["id"] = $id;
    $r["subid"] = $subid;
    $min = (int)$data["minValue"];
    $max = (int)$data["maxValue"];
    //fb($min, $max);
    $r["Min"] = ($min >= 0) ? 0 : $min - mt_rand(10, 2 * abs($min));
    $r["Max"] = $max + mt_rand(10, 11 + 2 * $max);
    return $r;
}

function GetDisplayVideo($data, $id, $subid)
{
    global $orthoeman_id;
    $r = array();
    $r["id"] = $id;
    $r["subid"] = $subid;
    $r["video_mp4"] = "no";
    $r["video_ogg"] = "no";
    $r["video_webm"] = "no";
    foreach ($data->Source as $value) {
        //print_r($value);
        
        switch (strval($value["type"])) {
            case 'video/mp4':
                $r["mp4"] = '../get_resource.php?id='.$orthoeman_id.'&resource_id='.strval($value["id"]);
                $r["video_mp4"] = "yes";
                break;
            case 'video/ogg':
                $r["ogg"] = '../get_resource.php?id='.$orthoeman_id.'&resource_id='.strval($value["id"]);
                $r["video_ogg"] = "yes";
                break;
            case 'video/webm':
                $r["webm"] = '../get_resource.php?id='.$orthoeman_id.'&resource_id='.strval($value["id"]);
                $r["video_webm"] = "yes";
                break;
        }
    }
    return $r;
}

function GetDisplayComplexImg($data, $id, $subid)
{
    $r = array();
    $r["ImageURI"] = strval($data["id"]);
    $r["LoadMethod"] = "URI";
    $r["ShowRegions"] = strval($data["showRegions"]);
    $r["MaxSpots"] = getHotSpotsNumber($data);
    $r["HotSpots"] = ($r["MaxSpots"] == 0) ? "no" : "yes";
    $r["EnableTracking"] = strval($data["enableTracking"]);
    $r["id"] = $id;
    $r["subid"] = $subid;
    $r["width"] = strval($data["width"]);
    $r["height"] = strval($data["height"]);
    $r["ImageType"] = (count(get_object_vars($data)) >= 2) ? "complex" : "simple";
    return $r;
}

function getHotSpotsNumber($image)
{
    $hotSpots = 0;
    //fb(get_object_vars($image));
    foreach (get_object_vars($image) as $key => $value) {
        if ($key != "@attributes") {
            if (strval($value["isHotSpot"]) === "yes") {
                $hotSpots++;
            }
        }
    }

    return $hotSpots;
}

function GetDisplayQuizImg($data, $id, $subid)
{
    $r = array();
    $r["Question"] = strval($data->Question);
    $r["id"] = $id;
    $r["subid"] = $subid;
    $count = count($data->Answer);
    for ($i = 0; $i < $count; $i++)
        $r["Answer"][$i] = strval($data->Answer[$i]);
    return $r;
}

function GetDisplayText($data)
{
    return strval($data);
}
