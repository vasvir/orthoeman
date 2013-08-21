<?php
ob_start();
//session_start();
require_once('fb.php');
require_once('../../../config.php');
require_once('../lib.php');

// check credentials
$orthoeman_id = optional_param('orthoeman_id', 0, PARAM_INT); // course_module ID, or
/// TODO $orthoeman_id should be $id, $n like the rest of the scripts
list($my_course, $my_cm, $my_orthoeman, $my_context) = get_moodle_data($orthoeman_id, 0);

require_view_capability($my_orthoeman, $my_context);

add_to_log($my_course->id, 'orthoeman', 'launch display', "display.html?id={$my_cm->id}", $my_orthoeman->name, $my_cm->id);

$totalAnswers = 0;
$totalTheory = 0;
$totalSum = 0;

$old = 0;
$action = isset($_GET["action"]) ? $_GET["action"] : "0";
//fb(get_user_grades_from_orthoeman_id($my_orthoeman->di,3));
//getCorrectAnswers (getXMLData());



switch ($action) {
    case "1" :
        //$lessonid = $_GET["lessonid"];
        $xml = getXMLData();
        $displaydata = GetTemplateData($xml);
        
        //if ($displaydata["attributes"]["cuiseMode"] == 1)  {
        //    $displaydata["Tracking"] = getCorrectAnswers($xml);
       //     $displaydata["Timeout"] = 0;
       //     $displaydata["final"] = true;
       // }

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
        //fb($savedanswers.",".$totalAnswers);
        $answer->myanswer["final"] = ($totalAnswers === $savedanswers) ? "true" : "false";
        echo json_encode($answer->myanswer);
        submit_grade($my_orthoeman, $my_context);
        break;
    case "3":
        echo getTimeout();
        break;
    case "4":
        echo count(get_answers($my_orthoeman->id, -1));
        break;
}

function getTimeout()
{
    global $my_orthoeman;
    //$lessonDetails = get_lesson_details($orthoeman_id);
    //return $lessonDetails->timeout;
    return isLessonFinished() ? get_duration($my_orthoeman) : get_timeleft($my_orthoeman);
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
    $timeleft = get_timeleft($my_orthoeman);
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
    $answer_recs = get_answers($my_orthoeman->id, -1);
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
    $xml = getXMLData();
    $Page = $_GET["Page"];
    $return = null;
    switch ($type) {
        case 'quiz' :
            $return = GetQuizAnswer($xml,$Page,$_GET["answer"], $grade);
            break;
        case 'hotspots':
            $useranswer = isset($_GET["answer"]) ? $_GET["answer"] : array();
            $return = GetHotspotsAnswer($xml,$Page, $useranswer, $grade);
            break;
        case 'input':
            $return = getInputAnswer($xml,$Page, intval((int)$_GET["value"]), $grade);
            break;
        default :
            $return = "error";
            break;
    }
    return $return;
}

function getCorrectAnswers ($xml) 
{
    $r = array();
    $r[0] = new stdClass();
    $r[0]->type = 3;
    $r[0]->answer = "{}";
    $Page = 1;

    for ($i=0;$i< count($xml->Page);$i++)
    {
        echo $i;
        foreach ($xml->Page[$i] as $wkey => $wvalue)
        {
            $type = getWidgetType($wkey);
            echo $type;
            switch ($type)
            {

                case 'quiz' :
                    //$return = GetQuizAnswer($xml,$Page,$_GET["answer"], $grade);
                    break;
                case 'hotspots':
                    //$useranswer = isset($_GET["answer"]) ? $_GET["answer"] : array();
                    //$return = GetHotspotsAnswer($xml,$Page, $useranswer, $grade);
                    break;
                case 'input':
                    //$return = getInputAnswer($xml,$Page, intval((int)$_GET["value"]), $grade);
                    break;
                default :
                    //$return = "error";
                    break;
            }
        }

    }



}


function GetQuizAnswer($xml, $Page, $useranswer , &$grade)
{
    $return = array();
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

function getInputAnswer($xml,$Page, $myvalue, &$grade)
{
    $return = array();
    foreach ($xml->Page[intval($Page)] as $key => $value) {
        if ($key === "RangeQuiz") {
            $min = intval($value["minValue"]);
            $max = intval($value["maxValue"]);
        }
    }
    $isblocked = strval($xml->Page[intval($Page)]["block"]);
    $return["Answer"] = ($myvalue >= $min && $myvalue <= $max) ? "correct" : "wrong";
    $return["CorrectAnswer"] = $min + round(($max - $min)/2);
    $grade = getNormalizeGrade($Page, $xml, $return["Answer"]);
    return $return;
}

function GetHotspotsAnswer($xml,$Page,$useranswer, &$grade)
{
    $return = array();
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
    //each shape is a different image.
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
    //var_dump($hotspotsNumber);
    if ($hotspotsNumber > 0) {
        $return = array();
        foreach (get_object_vars($quizimage) as $key => $value) {
            if ($key != "@attributes") {
                switch ($key) {
                    case 'Circle' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                if (strval($bvalue["isHotSpot"]) === "yes") {
                                    $tempimg = imagecreate($width, $height);
                                    $white = imagecolorallocate($tempimg, 255, 255, 255);
                                    $black = imagecolorallocate($tempimg, 0, 0, 0);
                                    imagefill($tempimg, 0, 0, $white);
                                    PaintCircle($tempimg, $bvalue, $black);
                                    $myimg[] = $tempimg;
                                }
                            }
                        } else {
                            if (strval($value["isHotSpot"]) === "yes") {
                                $tempimg = imagecreate($width, $height);
                                $white = imagecolorallocate($tempimg, 255, 255, 255);
                                $black = imagecolorallocate($tempimg, 0, 0, 0);
                                imagefill($tempimg, 0, 0, $white);
                                PaintCircle($tempimg, $value, $black);
                                $myimg[] = $tempimg;
                            }
                        }

                        break;
                    case 'Rectangle' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                if (strval($bvalue["isHotSpot"]) === "yes") {
                                    $tempimg = imagecreate($width, $height);
                                    $white = imagecolorallocate($tempimg, 255, 255, 255);
                                    $black = imagecolorallocate($tempimg, 0, 0, 0);
                                    imagefill($tempimg, 0, 0, $white);
                                    PaintRect($tempimg, $bvalue, $black);
                                    $myimg[] = $tempimg;
                                }

                            }
                        } else {
                            if (strval($value["isHotSpot"]) === "yes") {
                                $tempimg = imagecreate($width, $height);
                                $white = imagecolorallocate($tempimg, 255, 255, 255);
                                $black = imagecolorallocate($tempimg, 0, 0, 0);
                                imagefill($tempimg, 0, 0, $white);
                                PaintRect($tempimg, $value, $black);
                                $myimg[] = $tempimg;
                            }

                        }
                        break;
                    case 'Polygon' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                if (strval($bvalue["isHotSpot"]) === "yes") {
                                    $tempimg = imagecreate($width, $height);
                                    $white = imagecolorallocate($tempimg, 255, 255, 255);
                                    $black = imagecolorallocate($tempimg, 0, 0, 0);
                                    imagefill($tempimg, 0, 0, $white);
                                    PaintPolygon($tempimg, $bvalue, $black);
                                    $myimg[] = $tempimg;
                                }

                            }
                        } else {
                            if (strval($value["isHotSpot"]) === "yes") {
                                $tempimg = imagecreate($width, $height);
                                $white = imagecolorallocate($tempimg, 255, 255, 255);
                                $black = imagecolorallocate($tempimg, 0, 0, 0);
                                imagefill($tempimg, 0, 0, $white);
                                PaintPolygon($tempimg, $value, $black);
                                $myimg[] = $tempimg;
                            }

                        }
                        break;
                    case 'Ellipse' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                if (strval($bvalue["isHotSpot"]) === "yes") {
                                    $tempimg = imagecreate($width, $height);
                                    $white = imagecolorallocate($tempimg, 255, 255, 255);
                                    $black = imagecolorallocate($tempimg, 0, 0, 0);
                                    imagefill($tempimg, 0, 0, $white);
                                    PaintEclipse($tempimg, $bvalue, $black);
                                    $myimg[] = $tempimg;
                                }
                            }
                        } else {
                            if (strval($value["isHotSpot"]) === "yes") {
                                $tempimg = imagecreate($width, $height);

                                $white = imagecolorallocate($tempimg, 255, 255, 255);
                                $black = imagecolorallocate($tempimg, 0, 0, 0);
                                imagefill($tempimg, 0, 0, $white);
                                PaintEclipse($tempimg, $value, $black);
                                $myimg[] = $tempimg;
                            }

                        }

                        break;
                }
                //header('Content-Type: image/png');
                //imagepng($tempimg);


            }
        }

    }
    //header('Content-Type: image/png');
    //imagepng($myimg[4]);
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
                                if (strval($bvalue["isHotSpot"]) === "yes") {
                                    $return[] = GetCircle($bvalue);
                                }
                            }
                        } else {
                            if (strval($value["isHotSpot"]) === "yes") {
                                $return[] = GetCircle($value);
                            }
                        }
                        break;
                    case 'Rectangle' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                if (strval($bvalue["isHotSpot"]) === "yes") {
                                    $return[] = GetRect($bvalue);
                                }
                            }
                        } else {
                            if (strval($value["isHotSpot"]) === "yes") {
                                $return[] = GetRect($value);
                            }
                        }
                        break;
                    case 'Polygon' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                if (strval($bvalue["isHotSpot"]) === "yes") {
                                    $return[] = GetPolygon($bvalue);
                                }
                            }
                        } else {
                            if (strval($value["isHotSpot"]) === "yes") {
                                $return[] = GetPolygon($value);
                            }
                        }
                        break;
                    case 'Ellipse' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                if (strval($bvalue["isHotSpot"]) === "yes") {
                                    $return[] = GetEclipse($bvalue);
                                }
                            }
                        } else {
                            if (strval($value["isHotSpot"]) === "yes") {
                                $return[] = GetEclipse($value);
                            }
                        }
                        break;
                }
            }
        }
    }
    return $return;

}

function getInfoShapes($quizimage)
{
    //var_dump($quizimage);

    //die();
    $return = array();
    if (isset($quizimage["showRegions"])) {
        if (strval($quizimage["showRegions"]) === "yes") {
            $return = array();
            foreach (get_object_vars($quizimage) as $key => $value) {
                switch ($key) {
                    case 'Circle' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                if (strval($bvalue["isHotSpot"]) === "no") {
                                    $return[] = GetCircle($bvalue);
                                }
                            }
                        } else {
                            if (strval($value["isHotSpot"]) === "no") {
                                $return[] = GetCircle($value);
                            }
                        }
                        break;
                    case 'Rectangle' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                if (strval($bvalue["isHotSpot"]) === "no") {
                                    $return[] = GetRect($bvalue);
                                }
                            }
                        } else {
                            if (strval($value["isHotSpot"]) === "no") {
                                $return[] = GetRect($value);
                            }
                        }
                        break;
                    case 'Polygon' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                if (strval($bvalue["isHotSpot"]) === "no") {
                                    $return[] = GetPolygon($bvalue);
                                }
                            }
                        } else {
                            if (strval($value["isHotSpot"]) === "no") {
                                $return[] = GetPolygon($value);
                            }
                        }
                        break;
                    case 'Ellipse' :
                        if (is_array($value)) {
                            foreach ($value as $bkey => $bvalue) {
                                if (strval($bvalue["isHotSpot"]) === "no") {
                                    $return[] = GetEclipse($bvalue);
                                }
                            }
                        } else {
                            if (strval($value["isHotSpot"]) === "no") {
                                $return[] = GetEclipse($value);
                            }
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
    imagefilledrectangle($imageone, strval($data->Point["x"]), strval($data->Point["y"]), intval($data->Point["x"]) + intval($data["width"]), intval($data->Point["y"]) + intval($data["height"]), $color);
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
    $lessonDetails = get_details($my_orthoeman);
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
                        'InfoShapes' => getInfoShapes($wvalue)
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
            if (is_array($value)) {
                foreach ($value as $key => $avalue) {
                    if (strval($avalue["isHotSpot"]) === "yes") {
                        $hotSpots++;
                    }
                }
            } else if (strval($value["isHotSpot"]) === "yes") {
                $hotSpots++;
            }
            // }

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