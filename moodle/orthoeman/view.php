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

require_login($course, true, $cm);
$context = get_context_instance(CONTEXT_MODULE, $cm->id);

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

require_capability('mod/orthoeman:view', $context);

// Output starts here
echo $OUTPUT->header();

if ($orthoeman->intro) { // Conditions to show the intro can change to look for own settings or whatever
    echo $OUTPUT->box(format_module_intro('orthoeman', $orthoeman, $cm->id), 'generalbox mod_introbox', 'orthoemanintro');
}

// Request the launch content with an object tag
echo '<object id="orthoeman_display_frame" height="600px" width="100%" type="text/html" data="Display/index.html?id='.$cm->id.'"></object>';
//echo '<object id="orthoeman_display_frame" style="width:100%; height: 600px;" type="text/html" data="AuthoringTool/AuthoringTool.html?id='.$cm->id.'"></object>';
        
//Output script to make the object tag be as large as possible
$resize = '<script type="text/javascript">
            //<![CDATA[
                (function() {
                    //Take scrollbars off the outer document to prevent double scroll bar effect
                    document.body.style.overflow = "hidden";
                    var dom = YAHOO.util.Dom;
                    var frame = document.getElementById("orthoeman_display_frame");
                    var padding = 15; //The bottom of the iframe wasn\'t visible on some themes. Probably because of border widths, etc.
                    var lastHeight;
                    var resize = function() {
                        var viewportHeight = dom.getViewportHeight();
                        if (lastHeight !== Math.min(dom.getDocumentHeight(), viewportHeight)) {
                            frame.style.height = viewportHeight - dom.getY(frame) - padding + "px";
                            lastHeight = Math.min(dom.getDocumentHeight(), dom.getViewportHeight());
                        }
                    };
                    resize();
                    //setInterval(resize, 250);
                    onresize = resize;
                })();
            //]]
        </script>
';

echo $resize;

// Finish the page
echo $OUTPUT->footer();
