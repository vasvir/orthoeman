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
 * Lanuched the Display Tool of a particular instance of orthoeman
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

list($course, $cm, $orthoeman, $context) = get_moodle_data($id, $n);

add_to_log($course->id, 'orthoeman', 'view', "view.php?id={$cm->id}", $orthoeman->name, $cm->id);

/// Print the page header

$PAGE->set_url('/mod/orthoeman/view.php', array('id' => $cm->id));
$PAGE->set_title(format_string($orthoeman->name));
$PAGE->set_heading(format_string($course->fullname));
$PAGE->set_context($context);

// other things you may want to set - remove if not needed
//$PAGE->set_cacheable(false);
//$PAGE->set_focuscontrol('some-html-id');
//$PAGE->add_body_class('orthoeman-'.$somevar);

require_view_capability($orthoeman, $context);

// Output starts here
echo $OUTPUT->header();

echo get_orthoeman_frame("Display/index.html?id=$cm->id");

// Finish the page
echo $OUTPUT->footer();
