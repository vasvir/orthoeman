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
 * Library of interface functions and constants for module orthoeman
 *
 * All the core Moodle functions, neeeded to allow the module to work
 * integrated in Moodle should be placed here.
 * All the orthoeman specific functions, needed to implement all the module
 * logic, should go to locallib.php. This will help to save some memory when
 * Moodle is performing actions across all modules.
 *
 * @package    mod
 * @subpackage orthoeman
 * @copyright  2011 Your Name
 * @license    http://www.gnu.org/copyleft/gpl.html GNU GPL v3 or later
 */

defined('MOODLE_INTERNAL') || die();


$ORTHOEMAN_TABLE = 'orthoeman';
$RESOURCE_TABLE = 'orthoeman_resource';
$ANSWER_TABLE = 'orthoeman_answer';
// this should be RESOURCE_TYPE_* really
$TYPE_XML = 'XML';
$TYPE_XML_VALUE = 0;
$TYPE_IMAGE = 'IMAGE';
$TYPE_IMAGE_VALUE = 1;
$TYPE_VIDEO = 'VIDEO';
$TYPE_VIDEO_VALUE = 2;
// answer_type
$ANSWER_TYPE_QUIZ = 'Quiz'
$ANSWER_TYPE_QUIZ_VALUE = 0;
$ANSWER_TYPE_HOTSPOT = 'Hotspot'
$ANSWER_TYPE_QUIZ_VALUE = 1;
$ANSWER_TYPE_INPUT = 'Input'
$ANSWER_TYPE_INPUT_VALUE = 2;

/** example constant */
//define('NEWMODULE_ULTIMATE_ANSWER', 42);

////////////////////////////////////////////////////////////////////////////////
// Moodle core API                                                            //
////////////////////////////////////////////////////////////////////////////////

/**
 * Returns the information on whether the module supports a feature
 *
 * @see plugin_supports() in lib/moodlelib.php
 * @param string $feature FEATURE_xx constant for requested feature
 * @return mixed true if the feature is supported, null if unknown
 */
function orthoeman_supports($feature) {
    switch($feature) {
        case FEATURE_MOD_INTRO:         return true;
        default:                        return null;
    }
}

/**
 * Saves a new instance of the orthoeman into the database
 *
 * Given an object containing all the necessary data,
 * (defined by the form in mod_form.php) this function
 * will create a new instance and return the id number
 * of the new instance.
 *
 * @param object $orthoeman An object from the form in mod_form.php
 * @param mod_orthoeman_mod_form $mform
 * @return int The id of the newly inserted orthoeman record
 */
function orthoeman_add_instance(stdClass $orthoeman, mod_orthoeman_mod_form $mform = null) {
    global $DB;

    $orthoeman->timecreated = time();

    # You may have to add extra stuff in here #

    return $DB->insert_record('orthoeman', $orthoeman);
}

/**
 * Updates an instance of the orthoeman in the database
 *
 * Given an object containing all the necessary data,
 * (defined by the form in mod_form.php) this function
 * will update an existing instance with new data.
 *
 * @param object $orthoeman An object from the form in mod_form.php
 * @param mod_orthoeman_mod_form $mform
 * @return boolean Success/Fail
 */
function orthoeman_update_instance(stdClass $orthoeman, mod_orthoeman_mod_form $mform = null) {
    global $DB;

    $orthoeman->timemodified = time();
    $orthoeman->id = $orthoeman->instance;

    # You may have to add extra stuff in here #

    return $DB->update_record('orthoeman', $orthoeman);
}

/**
 * Removes an instance of the orthoeman from the database
 *
 * Given an ID of an instance of this module,
 * this function will permanently delete the instance
 * and any data that depends on it.
 *
 * @param int $id Id of the module instance
 * @return boolean Success/Failure
 */
function orthoeman_delete_instance($id) {
    global $DB, $RESOURCE_TABLE;

    if (! $orthoeman = $DB->get_record('orthoeman', array('id' => $id))) {
        return false;
    }

    # Delete any dependent records here #

    //this does not work. beats me why?
    //$DB->delete_records($RESOURCE_TABLE, array('orthoeman_id' => $orthoeman->id));
    //error_log("XXX $RESOURCE_TABLE $orthoeman->id");
    $DB->delete_records('orthoeman_resource', array('orthoeman_id' => $orthoeman->id));

    $DB->delete_records('orthoeman', array('id' => $orthoeman->id));

    return true;
}

/**
 * Returns a small object with summary information about what a
 * user has done with a given particular instance of this module
 * Used for user activity reports.
 * $return->time = the time they did it
 * $return->info = a short text description
 *
 * @return stdClass|null
 */
function orthoeman_user_outline($course, $user, $mod, $orthoeman) {

    $return = new stdClass();
    $return->time = 0;
    $return->info = '';
    return $return;
}

/**
 * Prints a detailed representation of what a user has done with
 * a given particular instance of this module, for user activity reports.
 *
 * @param stdClass $course the current course record
 * @param stdClass $user the record of the user we are generating report for
 * @param cm_info $mod course module info
 * @param stdClass $orthoeman the module instance record
 * @return void, is supposed to echp directly
 */
function orthoeman_user_complete($course, $user, $mod, $orthoeman) {
}

/**
 * Given a course and a time, this module should find recent activity
 * that has occurred in orthoeman activities and print it out.
 * Return true if there was output, or false is there was none.
 *
 * @return boolean
 */
function orthoeman_print_recent_activity($course, $viewfullnames, $timestart) {
    return false;  //  True if anything was printed, otherwise false
}

/**
 * Prepares the recent activity data
 *
 * This callback function is supposed to populate the passed array with
 * custom activity records. These records are then rendered into HTML via
 * {@link orthoeman_print_recent_mod_activity()}.
 *
 * @param array $activities sequentially indexed array of objects with the 'cmid' property
 * @param int $index the index in the $activities to use for the next record
 * @param int $timestart append activity since this time
 * @param int $courseid the id of the course we produce the report for
 * @param int $cmid course module id
 * @param int $userid check for a particular user's activity only, defaults to 0 (all users)
 * @param int $groupid check for a particular group's activity only, defaults to 0 (all groups)
 * @return void adds items into $activities and increases $index
 */
function orthoeman_get_recent_mod_activity(&$activities, &$index, $timestart, $courseid, $cmid, $userid=0, $groupid=0) {
}

/**
 * Prints single activity item prepared by {@see orthoeman_get_recent_mod_activity()}

 * @return void
 */
function orthoeman_print_recent_mod_activity($activity, $courseid, $detail, $modnames, $viewfullnames) {
}

/**
 * Function to be run periodically according to the moodle cron
 * This function searches for things that need to be done, such
 * as sending out mail, toggling flags etc ...
 *
 * @return boolean
 * @todo Finish documenting this function
 **/
function orthoeman_cron () {
    return true;
}

/**
 * Returns all other caps used in the module
 *
 * @example return array('moodle/site:accessallgroups');
 * @return array
 */
function orthoeman_get_extra_capabilities() {
    return array();
}

////////////////////////////////////////////////////////////////////////////////
// Gradebook API                                                              //
////////////////////////////////////////////////////////////////////////////////

/**
 * Is a given scale used by the instance of orthoeman?
 *
 * This function returns if a scale is being used by one orthoeman
 * if it has support for grading and scales. Commented code should be
 * modified if necessary. See forum, glossary or journal modules
 * as reference.
 *
 * @param int $orthoemanid ID of an instance of this module
 * @return bool true if the scale is used by the given orthoeman instance
 */
function orthoeman_scale_used($orthoemanid, $scaleid) {
    global $DB;

    /** @example */
    if ($scaleid and $DB->record_exists('orthoeman', array('id' => $orthoemanid, 'grade' => -$scaleid))) {
        return true;
    } else {
        return false;
    }
}

/**
 * Checks if scale is being used by any instance of orthoeman.
 *
 * This is used to find out if scale used anywhere.
 *
 * @param $scaleid int
 * @return boolean true if the scale is used by any orthoeman instance
 */
function orthoeman_scale_used_anywhere($scaleid) {
    global $DB;

    /** @example */
    if ($scaleid and $DB->record_exists('orthoeman', array('grade' => -$scaleid))) {
        return true;
    } else {
        return false;
    }
}

/**
 * Creates or updates grade item for the give orthoeman instance
 *
 * Needed by grade_update_mod_grades() in lib/gradelib.php
 *
 * @param stdClass $orthoeman instance object with extra cmidnumber and modname property
 * @return void
 */
function orthoeman_grade_item_update(stdClass $orthoeman) {
    global $CFG;
    require_once($CFG->libdir.'/gradelib.php');

    /** @example */
    $item = array();
    $item['itemname'] = clean_param($orthoeman->name, PARAM_NOTAGS);
    $item['gradetype'] = GRADE_TYPE_VALUE;
    $item['grademax']  = $orthoeman->grade;
    $item['grademin']  = 0;

    grade_update('mod/orthoeman', $orthoeman->course, 'mod', 'orthoeman', $orthoeman->id, 0, null, $item);
}

/**
 * Update orthoeman grades in the gradebook
 *
 * Needed by grade_update_mod_grades() in lib/gradelib.php
 *
 * @param stdClass $orthoeman instance object with extra cmidnumber and modname property
 * @param int $userid update grade of specific user only, 0 means all participants
 * @return void
 */
function orthoeman_update_grades(stdClass $orthoeman, $userid = 0) {
    global $CFG, $DB;
    require_once($CFG->libdir.'/gradelib.php');

    /** @example */
    $grades = array(); // populate array of grade objects indexed by userid

    grade_update('mod/orthoeman', $orthoeman->course, 'mod', 'orthoeman', $orthoeman->id, 0, $grades);
}

////////////////////////////////////////////////////////////////////////////////
// File API                                                                   //
////////////////////////////////////////////////////////////////////////////////

/**
 * Returns the lists of all browsable file areas within the given module context
 *
 * The file area 'intro' for the activity introduction field is added automatically
 * by {@link file_browser::get_file_info_context_module()}
 *
 * @param stdClass $course
 * @param stdClass $cm
 * @param stdClass $context
 * @return array of [(string)filearea] => (string)description
 */
function orthoeman_get_file_areas($course, $cm, $context) {
    return array();
}

/**
 * File browsing support for orthoeman file areas
 *
 * @package mod_orthoeman
 * @category files
 *
 * @param file_browser $browser
 * @param array $areas
 * @param stdClass $course
 * @param stdClass $cm
 * @param stdClass $context
 * @param string $filearea
 * @param int $itemid
 * @param string $filepath
 * @param string $filename
 * @return file_info instance or null if not found
 */
function orthoeman_get_file_info($browser, $areas, $course, $cm, $context, $filearea, $itemid, $filepath, $filename) {
    return null;
}

/**
 * Serves the files from the orthoeman file areas
 *
 * @package mod_orthoeman
 * @category files
 *
 * @param stdClass $course the course object
 * @param stdClass $cm the course module object
 * @param stdClass $context the orthoeman's context
 * @param string $filearea the name of the file area
 * @param array $args extra arguments (itemid, path)
 * @param bool $forcedownload whether or not force download
 * @param array $options additional options affecting the file serving
 */
function orthoeman_pluginfile($course, $cm, $context, $filearea, array $args, $forcedownload, array $options=array()) {
    global $DB, $CFG;

    if ($context->contextlevel != CONTEXT_MODULE) {
        send_file_not_found();
    }

    require_login($course, true, $cm);

    send_file_not_found();
}

////////////////////////////////////////////////////////////////////////////////
// Navigation API                                                             //
////////////////////////////////////////////////////////////////////////////////

/**
 * Extends the global navigation tree by adding orthoeman nodes if there is a relevant content
 *
 * This can be called by an AJAX request so do not rely on $PAGE as it might not be set up properly.
 *
 * @param navigation_node $navref An object representing the navigation tree node of the orthoeman module instance
 * @param stdClass $course
 * @param stdClass $module
 * @param cm_info $cm
 */
function orthoeman_extend_navigation(navigation_node $navref, stdclass $course, stdclass $module, cm_info $cm) {
}

/**
 * Extends the settings navigation with the orthoeman settings
 *
 * This function is called when the context for the page is a orthoeman module. This is not called by AJAX
 * so it is safe to rely on the $PAGE.
 *
 * @param settings_navigation $settingsnav {@link settings_navigation}
 * @param navigation_node $orthoemannode {@link navigation_node}
 */
function orthoeman_extend_settings_navigation(settings_navigation $settingsnav, navigation_node $orthoemannode=null) {
}

function get_current_url() {
    $pageURL = 'http';
    if (isset($_SERVER["HTTPS"]) && $_SERVER["HTTPS"] == "on") {
        $pageURL .= "s";
    }
    $pageURL .= "://";
    if ($_SERVER["SERVER_PORT"] != "80") {
        $pageURL .= $_SERVER["SERVER_NAME"].":".$_SERVER["SERVER_PORT"].$_SERVER["REQUEST_URI"];
    } else {
        $pageURL .= $_SERVER["SERVER_NAME"].$_SERVER["REQUEST_URI"];
    }
    return $pageURL;
}

function get_orthoeman_frame($url, $display = "block", $toggle_link = FALSE) {
    // Request the launch content with an object tag
    $frame_id = md5($url);
    $parent_id = "parent_$frame_id";
    $toggle_link_id = "toggle_link_$frame_id";
    $show_text = "Edit OrthoEMan Case...";
    $hide_text = "Hide OrthoEMan Case";

    //Output script to make the object tag be as large as possible
    $orthoeman_html = '<div id="'.$parent_id.'"><script type="text/javascript">
            //<![CDATA[
                var orthoeman_initialized = false;
                var orthoeman_display = "'.$display .'";
                var orthoeman_frame_id = "'.$frame_id.'";
                var orthoeman_toggle_link = '.($toggle_link ? "true" : "false").';
                var orthoeman_toggle_link_html = orthoeman_toggle_link ? "<a id=\"'.$toggle_link_id.'\" href=\"#\" onclick=\"toggle_orthoeman();\"></a>" : "";
                //var orthoeman_frame = orthoeman_toggle_link_html + "<object id=\"' . $frame_id . '\" style=\"width:100%; height: 600px;\" type=\"text/html\" data=\"' . $url . '\"></object>";
                var orthoeman_frame = orthoeman_toggle_link_html + "<iframe id=\"' . $frame_id . '\" style=\"width:100%; height: 600px;\" src=\"' . $url . '\" frameborder=\"0\"></iframe>";

                function init_orthoeman() {
                        if (orthoeman_initialized) {
                            return;
                        }

                        var parent = document.getElementById("'.$parent_id.'");
                        parent.innerHTML = orthoeman_frame;
                        if (orthoeman_toggle_link) {
                            var toggle_link = document.getElementById("'.$toggle_link_id.'");
                            toggle_link.innerHTML = "'.$hide_text.'";
                        }

                        //Take scrollbars off the outer document to prevent double scroll bar effect
                        document.body.style.overflow = "hidden";
                        var dom = YAHOO.util.Dom;
                        var frame = document.getElementById(orthoeman_frame_id);
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

                        orthoeman_initialized = true;
                }

                function toggle_orthoeman() {
                    init_orthoeman();
                    var element = document.getElementById(orthoeman_frame_id);
                    if (orthoeman_display == "none") {
                        orthoeman_display = "block";
                        element.style.display = orthoeman_display;
                        document.body.style.overflow = "hidden";
                        if (orthoeman_toggle_link) {
                            var toggle_link = document.getElementById("'.$toggle_link_id.'");
                            toggle_link.innerHTML = "'.$hide_text.'";
                        }
                    } else {
                        orthoeman_display = "none";
                        element.style.display = orthoeman_display;
                        document.body.style.overflow = "auto";
                        if (orthoeman_toggle_link) {
                            var toggle_link = document.getElementById("'.$toggle_link_id.'");
                            toggle_link.innerHTML = "'.$show_text.'";
                        }
                    }
                }

                (function() {
                    if (orthoeman_toggle_link) {
                        var parent = document.getElementById("'.$parent_id.'");
                        parent.innerHTML = orthoeman_toggle_link_html;
                        var toggle_link = document.getElementById("'.$toggle_link_id.'");
                        toggle_link.innerHTML = "'.$show_text.'";
                    }
                    // do we have to initialize now?
                    if (orthoeman_display == "block") {
                        init_orthoeman();
                    }
                })();
            //]]
        </script></div>';
    return $orthoeman_html;
}

function get_database_data($orthoeman_id, $resource_id) {
    global $DB, $RESOURCE_TABLE, $TYPE_XML_VALUE;

    if ($resource_id == -1) {
        $resource_rec = $DB->get_record($RESOURCE_TABLE, array('orthoeman_id' => $orthoeman_id, 'type' => $TYPE_XML_VALUE));
    } else {
        $resource_rec = $DB->get_record($RESOURCE_TABLE, array('id' => $resource_id, 'orthoeman_id' => $orthoeman_id));
    }
    return $resource_rec;
}

function get_lesson_details($id) {
    global $DB,$ORTHOEMAN_TABLE;
    $cm = get_coursemodule_from_id('orthoeman', $id, 0, false, MUST_EXIST);
    $orthoeman = $DB->get_record('orthoeman', array('id' => $cm->instance), '*', MUST_EXIST);
    return $DB->get_record($ORTHOEMAN_TABLE, array('id' => $orthoeman->id));
}
