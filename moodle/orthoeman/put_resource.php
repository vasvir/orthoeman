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

//require_capability("mod/orthoeman:read", $context);

add_to_log($course->id, 'orthoeman', 'put_resource', "view.php?id={$cm->id}", $orthoeman->name, $cm->id);

/// Print the page header

$PAGE->set_url('/mod/orthoeman/view.php', array('id' => $cm->id));
$PAGE->set_title(format_string($orthoeman->name));
$PAGE->set_heading(format_string($course->fullname));
$PAGE->set_context($context);

// other things you may want to set - remove if not needed
//$PAGE->set_cacheable(false);
//$PAGE->set_focuscontrol('some-html-id');
//$PAGE->add_body_class('orthoeman-'.$somevar);

require_capability('mod/orthoeman:write', $context);

// Output starts here
//echo $OUTPUT->header();


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
            $resource_id = $DB->insert_record($RESOURCE_TABLE, $resource_rec);
            $resource_rec->id = $resource_id;
        }
        echo "$resource_rec->id";
    } else if ($type == $TYPE_IMAGE) {
        $url = preg_replace('/^\.\./', '', urldecode(file_get_contents('php://input')));
        //echo "url $url<BR>";
        $current_url = get_current_url();
        //echo "current_url $current_url<BR>";
        $img_url = preg_replace('/\/put_resource.php.*$/', '', $current_url) . $url;
        //echo "img_url $img_url<BR>";
        $img = file_get_contents($img_url);
        //echo "img XXX $img XXX<BR>";
        $md5 = md5($img);
        $resource_rec = $DB->get_record($RESOURCE_TABLE, array('type' => $TYPE_IMAGE_VALUE, 'md5' => $md5));
        if (!$resource_rec) {
            $resource_rec = new Object();
            $resource_rec->orthoeman_id = $orthoeman->id;
            $resource_rec->type = $TYPE_IMAGE_VALUE;
            $resource_rec->data = $img;
            $resource_rec->md5 = $md5;
            $resource_id = $DB->insert_record($RESOURCE_TABLE, $resource_rec);
            $resource_rec->id = $resource_id;
        }
        echo "$resource_rec->id";
    } else if ($type == $TYPE_VIDEO) {
        $url = preg_replace('/^\.\./', '', urldecode(file_get_contents('php://input')));
        //echo "url $url<BR>";
        $current_url = get_current_url();
        //echo "current_url $current_url<BR>";
        $video_url = preg_replace('/\/put_resource.php.*$/', '', $current_url) . $url;
        //echo "img_url $img_url<BR>";
        $video = file_get_contents($video_url);
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
            $resource_id = $DB->insert_record($RESOURCE_TABLE, $resource_rec);

            $ids[] = $resource_id;

            $formats = array('h264', 'ogg', 'webm');
            foreach ($formats as $format) {
                $resource_rec = new Object();
                $resource_rec->orthoeman_id = $orthoeman->id;
                $resource_rec->type = $TYPE_VIDEO_VALUE;
                $ffmpeg = preg_replace('/&/', '\\&', "ffmpeg -i $video_url -f $format -");
                $resource_rec->data = `$ffmpeg`;
                $resource_rec->md5 = md5($resource_rec->data);
                $resource_id = $DB->insert_record($RESOURCE_TABLE, $resource_rec);
                $ids[] = $resource_id;
            }
        } else {
            $ids[] = $resource_rec->id;
        }
        print_r($ids);
    } else {
        echo "Undefined resource type $type";
        http_response_code(403);
    }
} else {
    echo "Invalid use of orthoeman plugin resource";
    http_response_code(403);
}
