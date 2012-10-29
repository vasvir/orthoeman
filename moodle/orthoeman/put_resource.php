<?php

// This file is part of Moodle - http://moodle.org/
//
// Moodle is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// Moodle is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Moodle.  If not, see <http://www.gnu.org/licenses/>.

/**
 * Prints a particular instance of orthoeman
 *
 * You can have a rather longer description of the file as well,
 * if you like, and it can span multiple lines.
 *
 * @package    mod
 * @subpackage orthoeman
 * @copyright  2011 Your Name
 * @license    http://www.gnu.org/copyleft/gpl.html GNU GPL v3 or later
 */

/// (Replace orthoeman with the name of your module and remove this line)

require_once(dirname(dirname(dirname(__FILE__))).'/config.php');
require_once(dirname(__FILE__).'/lib.php');

$id = optional_param('id', 0, PARAM_INT); // course_module ID, or
$n  = optional_param('n', 0, PARAM_INT);  // orthoeman instance ID - it should be named as the first character of the module

if ($id) {
    $cm         = get_coursemodule_from_id('orthoeman', $id, 0, false, MUST_EXIST);
    $course     = $DB->get_record('course', array('id' => $cm->course), '*', MUST_EXIST);
    $orthoeman  = $DB->get_record('orthoeman', array('id' => $cm->instance), '*', MUST_EXIST);
} elseif ($n) {
    $orthoeman  = $DB->get_record('orthoeman', array('id' => $n), '*', MUST_EXIST);
    $course     = $DB->get_record('course', array('id' => $orthoeman->course), '*', MUST_EXIST);
    $cm         = get_coursemodule_from_instance('orthoeman', $orthoeman->id, $course->id, false, MUST_EXIST);
} else {
    error('You must specify a course_module ID or an instance ID');
}

$type = required_param('type', PARAM_ALPHA);

require_login($course, true, $cm);
$context = get_context_instance(CONTEXT_MODULE, $cm->id);

require_capability('mod/orthoeman:write', $context);

add_to_log($course->id, 'orthoeman', 'put_resource', "put_resource.php?id={$cm->id}", $orthoeman->name, $cm->id);

/* gets the data from a URL */
function get_url_data($url) {
    $ch = curl_init();
    //$timeout = 5;
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
    //curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, $timeout);
    $result = new Object();
    
    $result->data = curl_exec($ch);
    $info = curl_getinfo($ch);
    $result->content_type = $info['content_type'];
    
    curl_close($ch);
    return $result;
}

// Finish the page
//echo $OUTPUT->footer();
if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    if ($type == $TYPE_XML) {
        $xml = urldecode(file_get_contents('php://input'));

        $resource_rec = $DB->get_record($RESOURCE_TABLE, array('orthoeman_id' => $orthoeman->id, 'type' => $TYPE_XML_VALUE));
        if ($resource_rec) {
            $resource_rec->data = $xml;
            $resource_rec->md5 = md5($xml);
            $DB->update_record('orthoeman_resource', $resource_rec);
        } else {
            $resource_rec = new Object();
            $resource_rec->orthoeman_id = $orthoeman->id;
            $resource_rec->type = $TYPE_XML_VALUE;
            $resource_rec->data = $xml;
            $resource_rec->md5 = md5($xml);
            $resource_rec->content_type = 'text/xml; charset="utf-8"';
            $resource_rec->parent_id = 0;
            $resource_id = $DB->insert_record($RESOURCE_TABLE, $resource_rec);
            $resource_rec->id = $resource_id;
        }
        echo "$resource_rec->id:$resource_rec->content_type";
    } else if ($type == $TYPE_IMAGE) {
        $url = preg_replace('/^\.\./', '', urldecode(file_get_contents('php://input')));
        //echo "url $url<BR>";
        $current_url = get_current_url();
        //echo "current_url $current_url<BR>";
        $img_url = preg_replace('/\/put_resource.php.*$/', '', $current_url) . $url;
        //echo "img_url $img_url<BR>";
        $result = get_url_data($img_url);
        $img = $result->data;
        //echo "img XXX $img XXX<BR>";
        $md5 = md5($img);
        $resource_rec = $DB->get_record($RESOURCE_TABLE, array('type' => $TYPE_IMAGE_VALUE, 'md5' => $md5));
        if (!$resource_rec) {
            $resource_rec = new Object();
            $resource_rec->orthoeman_id = $orthoeman->id;
            $resource_rec->type = $TYPE_IMAGE_VALUE;
            $resource_rec->data = $img;
            $resource_rec->md5 = $md5;
            $resource_rec->content_type = $result->content_type;
            $resource_rec->parent_id = 0;
            $resource_id = $DB->insert_record($RESOURCE_TABLE, $resource_rec);
            $resource_rec->id = $resource_id;
        }
        echo "$resource_rec->id:$resource_rec->content_type";
    } else if ($type == $TYPE_VIDEO) {
        $url = preg_replace('/^\.\./', '', urldecode(file_get_contents('php://input')));
        //echo "url $url<BR>";
        $current_url = get_current_url();
        //echo "current_url $current_url<BR>";
        $video_url = preg_replace('/\/put_resource.php.*$/', '', $current_url) . $url;
        //echo "img_url $img_url<BR>";
        $result = get_url_data($video_url);
        $video = $result->data;
        //echo "img XXX $img XXX<BR>";
        $md5 = md5($video);
        $resource_rec = $DB->get_record($RESOURCE_TABLE, array('type' => $TYPE_VIDEO_VALUE, 'md5' => $md5));
        $ids = array();
        if (!$resource_rec) {
            $resource_rec = new Object();
            $resource_rec->orthoeman_id = $orthoeman->id;
            $resource_rec->type = $TYPE_VIDEO_VALUE;
            $resource_rec->data = $video;
            $resource_rec->md5 = $md5;
            $resource_rec->content_type = $result->content_type;
            $resource_rec->parent_id = 0;
            $resource_id = $DB->insert_record($RESOURCE_TABLE, $resource_rec);

            $ids[] = "$resource_id:$resource_rec->content_type";

            $formats = array('h264', 'ogg', 'webm');
            $format_to_content_type = array(
                "h264" => 'video/mp4; codecs="avc1.4D401E, mp4a.40.2"',
                "ogg" => 'video/ogg; codecs="theora, vorbis"',
                "webm" => 'video/webm; codecs="vp8.0, vorbis"'
            );
            foreach ($formats as $format) {
                $resource_rec = new Object();
                $resource_rec->orthoeman_id = $orthoeman->id;
                $resource_rec->type = $TYPE_VIDEO_VALUE;
                $ffmpeg = preg_replace('/&/', '\\&', "ffmpeg -i $video_url -f $format -");
                $resource_rec->data = `$ffmpeg`;
                $resource_rec->md5 = md5($resource_rec->data);
                $resource_rec->content_type = $format_to_content_type[$format];
                $resource_rec->parent_id = $ids[0];
                $resource_id = $DB->insert_record($RESOURCE_TABLE, $resource_rec);
                $ids[] = "$resource_id:$resource_rec->content_type";
            }
        } else {
            $ids[] = "$resource_rec->id:$resource_rec->content_type";
            $resource_recs = $DB->get_records($RESOURCE_TABLE, array('type' => $TYPE_VIDEO_VALUE, 'parent_id' => $resource_rec->id));
            foreach ($resource_recs as $resource_rec) {
                $ids[] = "$resource_rec->id:$resource_rec->content_type";
            }
        }
        echo implode("|", $ids);
    } else {
        echo "Undefined resource type $type";
        http_response_code(403);
    }
} else {
    echo "Invalid use of orthoeman plugin resource";
    http_response_code(403);
}
