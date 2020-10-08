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
 * Sends a bug report to the admin of the moodle installation
 *
 * The bug report consists of a user part and a dump of moodle infomration.
 * The subject is optional in the url.
 * The user part of the body must be transmited with a POST http request
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
$subject = optional_param('subject', '', PARAM_TEXT);

list($course, $cm, $orthoeman, $context) = get_moodle_data($id, $n);

header("Content-type: text/plain");

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $text = urldecode(file_get_contents('php://input'));
    $result = report_bug($course, $cm, $orthoeman, $context, $subject, $text);
} else {
    $result = new stdClass();
    $result->msg = "Invalid use of orthoeman plugin resource";
    $result->status = 403; 
}
            
echo($result->msg);
http_response_code($result->status);
