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
require_once($CFG->dirroot . '/backup/util/includes/restore_includes.php');
require_once(dirname(__FILE__).'/lib.php');

$id = optional_param('id', 0, PARAM_INT); // course_module ID, or
$n  = optional_param('n', 0, PARAM_INT);  // orthoeman instance ID - it should be named as the first character of the module

list($course, $cm, $orthoeman, $context) = get_moodle_data($id, $n);

require_login($course, true, $cm);
require_capability('moodle/restore:restorecourse', $context);

global $DB, $USER;
  
$transaction = $DB->start_delegated_transaction();

$fullname = "VV restored course";
$shortname = "VVrst";
$categoryid = 2;
$folder = "test-backup";

// Create new course
$courseid = restore_dbops::create_new_course($fullname, $shortname, $categoryid);

// Restore backup into course
$controller = new restore_controller($folder, $courseid, 
  backup::INTERACTIVE_NO, backup::MODE_GENERAL, $USER->id,
  backup::TARGET_NEW_COURSE);
$controller->execute_precheck();
$controller->execute_plan();
// Commit
$transaction->allow_commit();