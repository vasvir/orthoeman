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
 * Gets an orthoeman resource (XML, Image, Video).
 *
 * The resource is byteserved so the HTML5 video players don't have to read
 * all the video to start playing.
 *
 * @package    mod
 * @subpackage orthoeman
 * @copyright  Vassilis Virvilis
 * @license    http://www.gnu.org/copyleft/gpl.html GNU GPL v3 or later
 */

require_once(dirname(dirname(dirname(__FILE__))).'/config.php');
require_once(dirname(__FILE__).'/lib.php');
require_once(dirname(__FILE__).'/byteserve.php');

$id = optional_param('id', 0, PARAM_INT); // course_module ID, or
$n  = optional_param('n', 0, PARAM_INT);  // orthoeman instance ID - it should be named as the first character of the module
$resource_id = optional_param('resource_id', -1, PARAM_INT); // resource_id -1 gets the XML

list($course, $cm, $orthoeman, $context) = get_moodle_data($id, $n);

$resource_rec = get_resource($course, $cm, $orthoeman, $context, true, $resource_id);

if ($resource_rec) {
  //do not send cache limiter header
  ini_set('session.cache_limiter','none');
 
  byteserve($resource_rec);
}
