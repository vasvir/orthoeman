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

$subject = optional_param('subject', '', PARAM_TEXT);

require_login($course, true, $cm);
$context = get_context_instance(CONTEXT_MODULE, $cm->id);

require_capability('mod/orthoeman:write', $context);

add_to_log($course->id, 'orthoeman', 'report_bug', "report_bug?id={$cm->id}", $orthoeman->name, $cm->id);


global $USER;

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    header("Content-type: text/plain");

    $text = urldecode(file_get_contents('php://input'));
    $moodle_info = "Username: $USER->username
Name: $USER->firstname $USER->lastname
e-mail: $USER->email
skype: $USER->skype
url:$USER->url
Institution: $USER->institution
Department: $USER->department
Address: $USER->address
City: $USER->city
Country: $USER->country\ncm: " . print_r($cm, true) . "\ncourse: " . print_r($course, true) . "\northoeman: " . print_r($orthoeman, true) . "\nuser: " . print_r($USER, true);;
    $body = "$text\n\nMoodle Information\n\n$moodle_info\n";

    //echo $body;

    $to_user = get_admin();
    $subject = "[BUG: $course->shortname] $USER->firstname $USER->lastname: $subject";
    $success = email_to_user($to_user, $USER, $subject, $body);
    if ($success == "1") {  
        echo "E-mail successful sent to $to_user->firstname $to_user->lastname!";
    } else if ($success === "emailstop") {
        echo "E-mail of $to_user->firstname $to_user->lastname has been DISABLED! E-mail NOT sent";
        http_response_code(406);
    } else if (!$success) {
        echo "Error: E-mail unsuccessful.";
        http_response_code(404);
    }
} else {
    echo "Invalid use of orthoeman plugin resource";
    http_response_code(403);
}
