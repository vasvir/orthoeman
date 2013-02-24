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

$page_id = required_param('page_id', PARAM_INT);
$type = required_param('type', PARAM_INT);
$answer = required_param('answer', PARAM_TEXT);

require_login($course, true, $cm);
$context = get_context_instance(CONTEXT_MODULE, $cm->id);

$view_access = has_view_capability($id, $context);
$read_access = has_capability('mod/orthoeman:read', $context);
$write_access = has_capability('mod/orthoeman:submit', $context);

if (!$view_access && !$write_access && !$read_access) {
    throw new required_capability_exception($context, $capability, 'nopermissions', '');
}

add_to_log($course->id, 'orthoeman', 'put_resource', "put_answer.php?id={$cm->id}", $orthoeman->name, $cm->id);


if ($read_access) {
    echo "Read only. Nothing to do. Exiting...";
    return;
}

global $USER;

$answer_rec = new Object();
$answer_rec->orthoeman_id = $orthoeman->id;
$answer_rec->user_id = $USER->id;
$answer_rec->page_id = $page_id;
$answer_rec->type = $type;
$answer_rec->answer = $answer;
$resource_id = $DB->insert_record($ANSWER_TABLE, $answer_rec);

echo json_encode($answer_rec);
