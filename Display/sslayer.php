<?php
ob_start();
//session_start();
require_once('fb.php');
require_once('../../../config.php');
require_once('../lib.php');

// check credentials
$orthoeman_id = optional_param('orthoeman_id', 0, PARAM_INT); // course_module ID, or
$my_cm = get_coursemodule_from_id('orthoeman', $orthoeman_id, 0, false, MUST_EXIST);
$my_course = $DB->get_record('course', array('id' => $my_cm->course), '*', MUST_EXIST);
$my_orthoeman = $DB->get_record('orthoeman', array('id' => $my_cm->instance), '*', MUST_EXIST);

require_login($my_course, true, $my_cm);
$my_context = get_context_instance(CONTEXT_MODULE, $my_cm->id);

require_view_capability($orthoeman_id, $my_context);

add_to_log($my_course->id, 'orthoeman', 'launch display', "display.html?id={$my_cm->id}", $my_orthoeman->name, $my_cm->id);

$totalAnswers = 0;
$totalTheory = 0;
$totalSum = 0;

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
        $displaydata["Tracking"] = getAnswersFromMoodle();
        putAnswerInMoodle(-1, 3, "{}");
        $displaydata["Timeout"] = getTimeout();
        $displaydata["final"] = isLessonFinished_totalanswers(count($displaydata["Tracking"]));
        echo json_encode($displaydata);
        break;
    case "2" :
        $type = $_GET["type"];
        $page = $_GET["Page"];
        $answer = new stdClass();
        switch ($type) {
            case 'quiz':
                $typeID = 2;
                $answer->userrespond = isset($_GET["answer"]) ? $_GET["answer"] : array();
                break;
            case 'input':
                $typeID = 0;
                $answer->userrespond = intval((int)$_GET["value"]);
                break;
            case 'hotspots':
                $typeID = 1;
                $answer->userrespond = isset($_GET["answer"]) ? $_GET["answer"] : array();
                break;
        }
        $answer->myanswer = GetAnswer($type, $grade);
        //fb($grade);
        $answer->grade = $grade;
        if ($answer->myanswer !== "error") {
            putAnswerInMoodle($page, $typeID, json_encode($answer));

        }
        $savedanswers = count(getAnswersFromMoodle());
        fb($savedanswers.",".$totalAnswers);
        $answer->myanswer["final"] = ($totalAnswers === $savedanswers) ? "true" : "false";
        echo json_encode($answer->myanswer);
        break;
    case "3":
        echo getTimeout();
        break;
}

function getTimeout()
{
    global $orthoeman_id;
    //$lessonDetails = get_lesson_details($orthoeman_id);
    //return $lessonDetails->timeout;
    return isLessonFinished() ? get_duration($orthoeman_id, 0) : get_timeleft($orthoeman_id, 0);
}

function isLessonFinished()
{
    global $totalAnswers;
    $tracking_ids = count(getAnswersFromMoodle());
    getXMLData();
    //fb($totalAnswers.",".$tracking_ids);
    return (intval($totalAnswers) === $tracking_ids) ? true : false;
}

function isLessonFinished_totalanswers($tracking_ids)
{
    global $totalAnswers;
    return (intval($totalAnswers) === $tracking_ids) ? true : false;
}

function putAnswerInMoodle($pageID, $typeID, $answer)
{
    global $orthoeman_id, $my_orthoeman;
    //check if there is another answer
    $oldAnswers = get_answers($my_orthoeman->id, intval($pageID) + 1);
    //and the remaining time
    $timeleft = get_timeleft($orthoeman_id, 0);
    if (count($oldAnswers) === 0 && $timeleft > 0 && !isLessonFinished()) {
        put_answer($orthoeman_id, 0, intval($pageID) + 1, intval($typeID), $answer);
    }

}

function putAnswerInMoodle_old($pageID, $typeID, $answer)
{
    global $orthoeman_id, $USER, $DB, $ANSWER_TABLE;
    $answer_rec = new stdClass();
    $answer_rec->orthoeman_id = $orthoeman_id;
    $answer_rec->user_id = $USER->id;
    $answer_rec->page_id = $pageID;
    $answer_rec->type = $typeID;
    $answer_rec->answer = $answer;
    $DB->insert_record($ANSWER_TABLE, $answer_rec);
}

function getAnswersFromMoodle()
{
    global $orthoeman_id, $my_orthoeman;
    $answer_recs = get_answers($my_orthoeman->id, -2);
    //fb($answer_recs);
    $r = array();
    foreach ($answer_recs as $page) {
        if ($page->page_id > 0) {
            $r[$page->page_id - 1] = new stdClass();
            $r[$page->page_id - 1]->type = $page->type;
            $r[$page->page_id - 1]->answer = $page->answer;
        }
    }
    return $r;
}

function getAnswersFromMoodle_old()
{
    global $DB, $USER, $ANSWER_TABLE, $orthoeman_id;
    $match_array = array('orthoeman_id' => $orthoeman_id, 'user_id' => $USER->id);
    //$DB->delete_records($ANSWER_TABLE, $match_array);
    $answer_recs = $DB->get_records($ANSWER_TABLE, $match_array);
    $r = array();
    foreach ($answer_recs as $page) {
        $r[$page->page_id] = new stdClass();
        $r[$page->page_id]->type = $page->type;
        $r[$page->page_id]->answer = $page->answer;
    }
    return $r;
}


function GetAnswer($type, &$grade)
{
    //sleep(2);

    $return = null;
    switch ($type) {
        case 'quiz' :
            $return = GetQuizAnswer($grade);
            break;
        case 'hotspots':
            $return = GetHotspotsAnswer($grade);
            break;
        case 'input':
            $return = getInputAnswer($grade);
            break;
        default :
            $return = "error";
            break;
    }
    return $return;
}


function GetQuizAnswer(&$grade)
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
    $grade = getNormalizeGrade($Page, $xml, $return["Answer"]);
    return $return;
}

function getInputAnswer(&$grade)
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
    $grade = getNormalizeGrade($Page, $xml, $return["Answer"]);
    return $return;
}

function GetHotspotsAnswer(&$grade)
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
    $grade = getNormalizeGrade($Page, $xml, $return["Answer"]);
    return $return;
}


function getNormalizeGrade($Page, $xml, $answer)
{
    global $totalSum;
    $grade = ($answer === "correct") ?
        intval(strval($xml->Page[intval($Page)]["positiveGrade"])) :
        -intval(strval($xml->Page[intval($Page)]["negativeGrade"]));
    //fb("original grade:".$grade);
    //$sumGrade = 0;
    //foreach ($xml->Page as $key => $value) {
    //    $sumGrade +=  intval(strval($value["positiveGrade"]));
    // }
    //fb("sumGrade:".$sumGrade);
    $ratio = 100 / $totalSum;
    //fb(round($grade*$ratio,2));
    //fb($totalSum);
    return round($grade * $ratio, 1);

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
    global $old, $my_orthoeman, $orthoeman_id, $totalAnswers, $totalTheory;
    if ($old != 0) return oldGetXMLData();
    //global $DB;
    //$id = optional_param('orthoeman_id', 0, PARAM_INT); // course_module ID, or
    //$cm = get_coursemodule_from_id('orthoeman', $id, 0, false, MUST_EXIST);
    //$course = $DB->get_record('course', array('id' => $cm->course), '*', MUST_EXIST);
    //$orthoeman = $DB->get_record('orthoeman', array('id' => $cm->instance), '*', MUST_EXIST);

    $resource = get_database_data($my_orthoeman->id, -1);
    // Inject into xml the course details from the moodle database

    $xmldata = simplexml_load_string($resource->data);
    $lessonDetails = get_lesson_details($orthoeman_id);
    $xmldata["cruiseMode"] = $lessonDetails->cruise;
    $xmldata["title"] = $lessonDetails->name;
    $xmldata["id"] = $lessonDetails->course;
    $xmldata->Abstract = $lessonDetails->intro;
    /** @var $totalAnswers int */
    $totalAnswers = getTotalAnswers($xmldata);
    $totalTheory = count($xmldata->Page) - $totalAnswers;
    //fb("Total Answers:".$totalTheory);
    //print_r($lessonDetails);
    return $xmldata;
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

function getTotalAnswers($data)
{
    global $totalSum;
    $count = 0;
    $index = 0;
    foreach ($data->Page as $key => $value) {
        $count++;
        $totalSum += intval(strval($value["positiveGrade"]));
        $widget = array();
        $maxspots = 0;
        $windex = 0;
        foreach ($value as $wkey => $wvalue) {
            $widget[$windex] = getWidgetType($wkey);
            if ($widget[$windex] === "image") {
                $maxspots = getHotSpotsNumber($wvalue);
            }
            $windex++;
        }
        if ($widget[0] === "video" || $widget[1] === "video") {
            if ($widget[0] === "text" || $widget[1] === "text") {
                $count--;
                $totalSum -= intval(strval($value["positiveGrade"]));
            }
        } else if ($widget[0] === "text" && $widget[1] === "text") {
            $count--;
            $totalSum -= intval(strval($value["positiveGrade"]));
        } else if ($widget[0] === "image" || $widget[1] === "image") {
            if ($widget[0] === "text" || $widget[1] === "text") {
                if ($maxspots === 0) {
                    $count--;
                    $totalSum -= intval(strval($value["positiveGrade"]));
                }


            }
        }
        $index++;
    }
    return $count;
}

function GetTemplateData($data)
{
    global $orthoeman_id;
    $a = array();
    //$a["attributes"]["id"] = strval($data["id"]);
    $a["attributes"]["Title"] = strval($data["title"]);
    $a["attributes"]["abstract"] = strval($data->Abstract);
    $a["attributes"]["id"] = strval($data["id"]);
    $a["attributes"]["cruiseMode"] = strval($data["cruiseMode"]);
    $index = 0;
    foreach ($data->Page as $key => $value) {
        $a["Page"][$index]["attributes"]["Grade"] = strval($value["positiveGrade"]);
        $a["Page"][$index]["attributes"]["negativeGrade"] = strval($value["negativeGrade"]);
        $a["Page"][$index]["attributes"]["Title"] = strval($value["title"]);
        $a["Page"][$index]["attributes"]["Blocked"] = "no"; // strval($value["block"]);
        $windex = 0;
        foreach ($value as $wkey => $wvalue) {
            $widgetype = getWidgetType($wkey);
            $a["Page"][$index]["Widget"][$windex]["type"] = $widgetype;
            switch ($widgetype) {
                case 'image' :
                    $a["Page"][$index]["Widget"][$windex]["Image"] = GetDisplayComplexImg($wvalue, $index, $windex);
                    $a["Images"][] = array('id' => $index, 'subid' => $windex,
                        //'url' => $a["Page"][$index]["Widget"][$windex]["Image"]["ImageURI"],
                        'url' => '../get_resource.php?id=' . $orthoeman_id . '&resource_id=' . $a["Page"][$index]["Widget"][$windex]["Image"]["ImageURI"],
                        'HotSpots' => $a["Page"][$index]["Widget"][$windex]["Image"]["HotSpots"],
                        'MaxSpots' => $a["Page"][$index]["Widget"][$windex]["Image"]["MaxSpots"],
                        'ShowRegions' => $a["Page"][$index]["Widget"][$windex]["Image"]["ShowRegions"],
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
                $r["mp4"] = '../get_resource.php?id=' . $orthoeman_id . '&resource_id=' . strval($value["id"]);
                $r["video_mp4"] = "yes";
                break;
            case 'video/ogg':
                $r["ogg"] = '../get_resource.php?id=' . $orthoeman_id . '&resource_id=' . strval($value["id"]);
                $r["video_ogg"] = "yes";
                break;
            case 'video/webm':
                $r["webm"] = '../get_resource.php?id=' . $orthoeman_id . '&resource_id=' . strval($value["id"]);
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
