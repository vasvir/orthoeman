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
 * Puts a resource in a particular instance of an orthoeman case
 *
 * @package    mod
 * @subpackage orthoeman
 * @copyright  Vassilis Virvilis
 * @license    http://www.gnu.org/copyleft/gpl.html GNU GPL v3 or later
 */

require_once(dirname(dirname(dirname(__FILE__))).'/config.php');
require_once(dirname(__FILE__).'/lib.php');

$id = optional_param('id', 0, PARAM_INT); // course_module ID, or
$n  = optional_param('n', 0, PARAM_INT);  // orthoeman instance ID - it should be named as the first character of the module
$type = required_param('type', PARAM_ALPHA);
$valid_resource_ids = optional_param('resource_ids', '', PARAM_TEXT);

list($course, $cm, $orthoeman, $context) = get_moodle_data($id, $n);

require_capability('mod/orthoeman:write', $context);

add_to_log($course->id, 'orthoeman', 'put_resource', "put_resource.php?id={$cm->id}", $orthoeman->name, $cm->id);

/* gets the data from a URL */
function get_url_data($url) {
    $ch = curl_init();
    //$timeout = 5;
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
    //curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, $timeout);
    $result = new stdClass();

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
        $xml = clean_xml(urldecode(file_get_contents('php://input')));

        $resource_rec = $DB->get_record($RESOURCE_TABLE, array('orthoeman_id' => $orthoeman->id, 'type' => $TYPE_XML_VALUE));
        if ($resource_rec) {
            $resource_rec->data = $xml;
            $resource_rec->md5 = md5($xml);
            $DB->update_record('orthoeman_resource', $resource_rec);
        } else {
            $resource_rec = new stdClass();
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

        // now let's delete non used resources
        if ($valid_resource_ids) {
            // first find valid parents
            $resource_array = $DB->get_records_select($RESOURCE_TABLE, "orthoeman_id = $orthoeman->id AND type <> 0 AND id IN ($valid_resource_ids)");
            $parent_ids = array();
            //print_r($resource_array);
            foreach ($resource_array as $resource_rec) {
                if ($resource_rec->parent_id) {
                    $parent_ids[$resource_rec->parent_id] = 1;
                }
            }
            foreach (array_keys($parent_ids) as $parent_id) {
                $valid_resource_ids .= "," . $parent_id;
            }
            $valid_resource_ids = implode(',', (array_unique(explode(',', $valid_resource_ids))));
        }

        $valid_resource_ids_sql = $valid_resource_ids ? "AND id NOT IN ($valid_resource_ids)" : "";
        $DB->delete_records_select($RESOURCE_TABLE, "orthoeman_id = $orthoeman->id AND type <> 0 $valid_resource_ids_sql");
   } else if ($type == $TYPE_IMAGE) {
        $content_type = $_FILES['uploadImage']['type'];
        $img = file_get_contents($_FILES['uploadImage']['tmp_name']);
        #error_log("img XXX " . $content_type . ":" . substr($img, 0, 3) . " XXX");
        $md5 = md5($img);
        $resource_rec = $DB->get_record($RESOURCE_TABLE, array('orthoeman_id' => $orthoeman->id, 'type' => $TYPE_IMAGE_VALUE, 'md5' => $md5));
        if (!$resource_rec) {
            $resource_rec = new stdClass();
            $resource_rec->orthoeman_id = $orthoeman->id;
            $resource_rec->type = $TYPE_IMAGE_VALUE;
            $resource_rec->data = $img;
            $resource_rec->md5 = $md5;
            $resource_rec->content_type = $content_type;
            $resource_rec->parent_id = 0;
            $resource_id = $DB->insert_record($RESOURCE_TABLE, $resource_rec);
            $resource_rec->id = $resource_id;
        }
        echo "$resource_rec->id:$resource_rec->content_type";
    } else if ($type == $TYPE_VIDEO) {
        $video_tmp_file = $_FILES['uploadVideo']['tmp_name'];
        $content_type = $_FILES['uploadVideo']['type'];
        $video = file_get_contents($video_tmp_file);
        error_log("video XXX " . $content_type . ":" . substr($video, 0, 3) . " XXX");
        $md5 = md5($video);
        $resource_rec = $DB->get_record($RESOURCE_TABLE, array('orthoeman_id' => $orthoeman->id, 'type' => $TYPE_VIDEO_VALUE, 'md5' => $md5));
        $id_map = array();
        if (!$resource_rec) {
            $resource_rec = new stdClass();
            $resource_rec->orthoeman_id = $orthoeman->id;
            $resource_rec->type = $TYPE_VIDEO_VALUE;
            $resource_rec->data = $video;
            $resource_rec->md5 = $md5;
            $resource_rec->content_type = $content_type;
            $resource_rec->codecs = "";
            $resource_rec->parent_id = 0;
            $resource_id = $DB->insert_record($RESOURCE_TABLE, $resource_rec);

            #$id_map[] = "$resource_id:$resource_rec->content_type:$resource_rec->codecs";
            $parent_id = $resource_id;

            $formats = array('h264', 'webm');
            $format_to_content_type = array(
                "h264" => 'video/mp4',
                "ogg" => 'video/ogg',
                "webm" => 'video/webm'
            );
            $format_to_codecs = array(
                "h264" => 'avc1.4D401E, mp4a.40.2',
                "ogg" => 'theora, vorbis',
                "webm" => 'vp8.0, vorbis'
            );
            foreach ($formats as $format) {
                $resource_rec = new stdClass();
                $resource_rec->orthoeman_id = $orthoeman->id;
                $resource_rec->type = $TYPE_VIDEO_VALUE;
                $ffmpeg = preg_replace('/&/', '\\&', "./video_convert.sh $video_tmp_file $format");
                $resource_rec->data = `$ffmpeg 2>> /tmp/video_convert.log`;
                $resource_rec->md5 = md5($resource_rec->data);
                $resource_rec->content_type = $format_to_content_type[$format];
                $resource_rec->codecs = $format_to_codecs[$format];
                $resource_rec->parent_id = $parent_id;
                $resource_id = $DB->insert_record($RESOURCE_TABLE, $resource_rec);
                $id_map[] = "$resource_id:$resource_rec->content_type:$resource_rec->codecs";
            }
        } else {
            #$id_map[] = "$resource_rec->id:$resource_rec->content_type:$resource_rec->codecs";
            $resource_recs = $DB->get_records($RESOURCE_TABLE, array('type' => $TYPE_VIDEO_VALUE, 'parent_id' => $resource_rec->id));
            foreach ($resource_recs as $resource_rec) {
                $id_map[] = "$resource_rec->id:$resource_rec->content_type:$resource_rec->codecs";
            }
        }
        echo implode("|", $id_map);
    } else {
        echo "Undefined resource type $type";
        http_response_code(403);
    }
} else {
    echo "Invalid use of orthoeman plugin resource";
    http_response_code(403);
}
