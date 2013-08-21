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
require_once(dirname(__FILE__).'/byteserve.php');

$id = optional_param('id', 0, PARAM_INT); // course_module ID, or
$n  = optional_param('n', 0, PARAM_INT);  // orthoeman instance ID - it should be named as the first character of the module
$resource_id = optional_param('resource_id', -1, PARAM_INT); // resource_id -1 gets the XML

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

require_login($course, true, $cm);
$context = get_context_instance(CONTEXT_MODULE, $cm->id);

require_view_capability($orthoeman, $context);

if ($resource_id == -1) {
  require_capability("mod/orthoeman:read", $context);
}

add_to_log($course->id, 'orthoeman', 'get_resource', "get_resource.php?id={$cm->id}", $orthoeman->name, $cm->id);

$resource_rec = get_database_data($orthoeman->id, $resource_id);

if (!$resource_rec) {
  echo "";
} else {
  if ($resource_rec->type == $TYPE_XML_VALUE) {
    require_capability("mod/orthoeman:read", $context);
}

  //unset magic quotes; otherwise, file contents will be modified
  set_magic_quotes_runtime(0);
 
  //do not send cache limiter header
  ini_set('session.cache_limiter','none');
 
  byteserve($resource_rec);
}
