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
global $ANSWER_TABLE;
$ANSWER_TABLE = 'orthoeman_answer';
// this should be RESOURCE_TYPE_* really
$TYPE_XML = 'XML';
global $TYPE_XML_VALUE;
$TYPE_XML_VALUE = 0;
$TYPE_IMAGE = 'IMAGE';
$TYPE_IMAGE_VALUE = 1;
$TYPE_VIDEO = 'VIDEO';
$TYPE_VIDEO_VALUE = 2;
// answer_type
$ANSWER_TYPE_QUIZ = 'Quiz';
$ANSWER_TYPE_QUIZ_VALUE = 0;
$ANSWER_TYPE_HOTSPOT = 'Hotspot';
$ANSWER_TYPE_QUIZ_VALUE = 1;
$ANSWER_TYPE_INPUT = 'Input';
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
        case FEATURE_GRADE_HAS_GRADE:	return true;
        case FEATURE_BACKUP_MOODLE2:	return true;
        case FEATURE_SHOW_DESCRIPTION:	return true;
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
    $DB->delete_records('orthoeman_answer', array('orthoeman_id' => $orthoeman->id));
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
    return false;
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
    return false;
}

function orthoeman_reset_course_form_definition(&$mform) {
}

function orthoeman_reset_course_form_defaults($course) {
    return array();
}

function orthoeman_reset_userdata($data) {
    global $DB;
    
    $orthoemans = $DB->get_records_sql("SELECT o.*, cm.idnumber as cmidnumber, o.course as courseid
        FROM {modules} m 
        JOIN {course_modules} cm ON m.id = cm.module
        JOIN {orthoeman} o ON cm.instance = o.id
        WHERE m.name = 'orthoeman' AND cm.course = ?", array($data->courseid));
    foreach ($orthoemans as $orthoeman) {
        orthoeman_grade_item_update($orthoeman, 'reset');
    }

    $status[] = array(
        'component' => get_string('modulenameplural', 'orthoeman'),
        'item' => get_string('gradesdeleted', 'orthoeman'),
        'error' => false);
                                            
    return $status;
}

/**
 * Creates or updates grade item for the give orthoeman instance
 *
 * Needed by grade_update_mod_grades() in lib/gradelib.php
 *
 * @param stdClass $orthoeman instance object with extra cmidnumber and modname property
 * @return void
 */
function orthoeman_grade_item_update(stdClass $orthoeman, $grades=NULL) {
    global $CFG;
    require_once($CFG->libdir.'/gradelib.php');

    $params = array();
    $params['itemname'] = clean_param($orthoeman->name, PARAM_NOTAGS);
    $params['gradetype'] = GRADE_TYPE_VALUE;
    $params['grademax']  = 100;
    $params['grademin']  = 0;

    if ($grades  === 'reset') {
        $params['reset'] = true;
        $grades = NULL;

        delete_answers_from_orthoeman_id($orthoeman->id);
    }

    //error_log("orthoeman_grade_item_update: " . json_encode($grades));
    grade_update('mod/orthoeman', $orthoeman->course, 'mod', 'orthoeman', $orthoeman->id, 0, $grades, $params);
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
function orthoeman_update_grades(stdClass $orthoeman, $userid = 0, $nullifnone = true) {
    global $CFG, $DB;
    require_once($CFG->libdir.'/gradelib.php');

    if ($grades = orthoeman_get_user_grades($orthoeman, $userid)) {
        orthoeman_grade_item_update($orthoeman, $grades);
    } else if ($userid && $nullifnone) {
        $grade = new stdClass();
        $grade->userid = $userid;
        $grade->rawgrade = null;
        orthoeman_grade_item_update($orthoeman, $grade);
    } else {
        orthoeman_grade_item_update($orthoeman);
    }
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
    $addHeight = 0; //(substr($url, 0,7) === "Display") ? 140 : 0;
    //Output script to make the object tag be as large as possible
    $scroll_handler = $toggle_link ? "" : "onscroll = function() { scrollTo(0, 0); }";
    $orthoeman_html = '<div id="'.$parent_id.'"><script type="text/javascript">
            //<![CDATA[
                var orthoeman_initialized = false;
                var orthoeman_display = "'.$display .'";
                var orthoeman_frame_id = "'.$frame_id.'";
                var orthoeman_toggle_link = '.($toggle_link ? "true" : "false").';
                var orthoeman_toggle_link_html = orthoeman_toggle_link ? "<a id=\"'.$toggle_link_id.'\" href=\"#\" onclick=\"toggle_orthoeman();\"></a>" : "";
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
                                //console.log(viewportHeight, lastHeight, dom.getY(frame));
                                frame.style.height = viewportHeight -'.$addHeight.' - dom.getY(frame) - padding + "px";
                                lastHeight = Math.min(dom.getDocumentHeight(), dom.getViewportHeight());

                            }
                            // chrome needs to scrollTo top
                            scrollTo(0, 0);
                        };
                        setTimeout(resize, 0);
                        onresize = resize;
                        // chrome needs to scrollTo top for Display
                        '.$scroll_handler.'
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

    /**
    *  should be called like that
    *  list($course, $cm, $orthoeman, $context) = get_moodle_data($id, 0);
    */
function get_moodle_data($id, $n) {
    global $DB;

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
    
    return array($course, $cm, $orthoeman, $context);
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

function get_details($orthoeman) {
    global $DB, $ORTHOEMAN_TABLE;
    return $DB->get_record($ORTHOEMAN_TABLE, array('id' => $orthoeman->id));
}

function has_view_capability($orthoeman, context $context) {
    $details = get_details($orthoeman);
    if ($details->cruise)
        return true;

    return has_capability('mod/orthoeman:view', $context);
}

function require_view_capability($orthoeman, context $context) {
    if (!has_view_capability($orthoeman, $context)) {
        throw new required_capability_exception($context, 'mod/orthoeman:view', 'nopermissions', '');
    }
}

// userid == 0 means all users
function get_user_answers($orthoeman_id, $user_id, $page_id) {
    global $DB, $ANSWER_TABLE;
    
    $match_array = array('orthoeman_id' => $orthoeman_id);
    if ($user_id > 0) {
        $match_array['user_id'] = $user_id;
    }
    if ($page_id >= 0) {
        $match_array['page_id'] = $page_id;
    }

    $answers = $DB->get_records($ANSWER_TABLE, $match_array);
    ksort($answers, SORT_NUMERIC);
    
    return $answers;
}

function get_answers($orthoeman, context $context, $page_id) {
    require_view_capability($orthoeman, $context);
    //add_to_log($course->id, 'orthoeman', 'get_answers', "get_answers.php?id={$cm->id}", $orthoeman->name, $cm->id);
    
    global $USER;

    return get_user_answers($orthoeman->id, $USER->id, $page_id);
}

function has_submit_capability($orthoeman, $context) {
    $view_access = has_view_capability($orthoeman, $context);
    $read_access = has_capability('mod/orthoeman:read', $context);
    $write_access = has_capability('mod/orthoeman:submit', $context);

    if (!$view_access && !$write_access && !$read_access) {
        throw new required_capability_exception($context, 'mod/orthoeman:submit', 'nopermissions', '');
    }

    return !$read_access;
}

function put_answer($course, $cm, $orthoeman, context $context, $page_id, $type, $answer) {
    $submit_access = has_submit_capability($orthoeman, $context);

    if (!$submit_access) {
        //echo "Read only. Nothing to do. Exiting...";
        return;
    }

    $timeleft = get_timeleft($orthoeman, $context);

    if (!$timeleft) {
        //echo "time's up";
        return;
    }

    add_to_log($course->id, 'orthoeman', 'put_answer', "put_answer.php?id={$cm->id}", $orthoeman->name, $cm->id);

    global $DB, $USER, $ANSWER_TABLE;

    $answer_rec = new stdClass();
    $answer_rec->orthoeman_id = $orthoeman->id;
    $answer_rec->user_id = $USER->id;
    $answer_rec->page_id = $page_id;
    $answer_rec->type = $type;
    $answer_rec->answer = $answer;
    $answer_rec->timesubmitted = time();
    $resource_id = $DB->insert_record($ANSWER_TABLE, $answer_rec);

    return $answer_rec;
}

function delete_answers_from_orthoeman_id($orthoeman_id, $user_id = -1, $page_id = -1) {
    $match_array = array('orthoeman_id' => $orthoeman_id);

    if ($user_id >= 0) {
        $match_array['user_id'] = $user_id;
    }
    
    if ($page_id >= 0) {
        $match_array['page_id'] = $page_id;
    }

    global $DB, $ANSWER_TABLE;        
    $DB->delete_records($ANSWER_TABLE, $match_array);
}

function delete_answers($id, $n, $user_id = -1, $page_id = -1) {
    list($course, $cm, $orthoeman, $context) = get_moodle_data($id, $n);

    require_login($course, true, $cm);
    $context = get_context_instance(CONTEXT_MODULE, $cm->id);

    require_capability('mod/orthoeman:write', $context);

    add_to_log($course->id, 'orthoeman', 'delete_answers', "delete_answers.php?id={$cm->id}", $orthoeman->name, $cm->id);

    delete_answers_from_orthoeman_id($orthoeman->id, $user_id, $page_id);
}

function get_timeleft($orthoeman, context $context) {
    $timeout = get_details($orthoeman)->timeout;
    $answers = get_answers($orthoeman, $context, -1);
    if (empty($answers)) {
        return (int) $timeout;
    }
    $timeleft = $timeout - (time() - reset($answers)->timesubmitted);
    return $timeleft > 0 ? $timeleft : 0;
}

function get_duration($orthoeman, context $context) {
    $timeout = get_details($orthoeman)->timeout;
    $answers = get_answers($orthoeman, $context, -1);
    if (empty($answers)) {
        return (int) $timeout;
    }
    $duration = $timeout - (end($answers)->timesubmitted - reset($answers)->timesubmitted);
    return $duration;
}

// param int $userid update grade of specific user only, 0 means all participants
function orthoeman_get_user_grades($orthoeman, $userid) {
    $answers = get_user_answers($orthoeman->id, $userid, -1);

    $grades = array();
    foreach ($answers as $answer) {
        if (isset($grades[$answer->user_id])) {
            $grade =& $grades[$answer->user_id];
        } else {
            $grade = array();
            $grade['userid'] = $answer->user_id;
            $grade['rawgrade'] = 0;
            $grade['dategraded'] = 0;
            $grades[$answer->user_id] = $grade;
        }
        $answer_dec = json_decode($answer->answer);
        if (isset($answer_dec->grade)) {
            $grade['rawgrade'] += $answer_dec->grade;
            $grade['dategraded'] = $answer->timesubmitted;
        }
    }
    return $grades;
}

function submit_grade($orthoeman, context $context) {
    $submit_access = has_submit_capability($orthoeman, $context);

    if (!$submit_access) {
        //echo "Read only. Nothing to do. Exiting...";
        return;
    }

    global $USER;
    return orthoeman_update_grades($orthoeman, $USER->id);
}

function clean_xml($xml) {
    $pattern = '/[^\x{0009}\x{000a}\x{000d}\x{0020}-\x{D7FF}\x{E000}-\x{FFFD}]+/u';
    return preg_replace($pattern, '', $xml);
}

function report_bug($course, $cm, $orthoeman, context $context, $subject, $text) {
    require_capability('mod/orthoeman:write', $context);
    add_to_log($course->id, 'orthoeman', 'report_bug', "report_bug?id={$cm->id}", $orthoeman->name, $cm->id);

    $result = new stdClass();

    global $USER;

    $moodle_info = "Username: $USER->username
Name: $USER->firstname $USER->lastname
e-mail: $USER->email
skype: $USER->skype
url:$USER->url
Institution: $USER->institution
Department: $USER->department
Address: $USER->address
City: $USER->city
Country: $USER->country\ncm: " . print_r($cm, true) . "\ncourse: " . print_r($course, true) . "\northoeman: " . print_r($orthoeman, true) . "\nuser: " . print_r($USER, true);
    $body = "$text\n\nMoodle Information\n\n$moodle_info\n";

    //echo $body;

    $to_user = get_admin();
    $subject = "[BUG: $course->shortname] $USER->firstname $USER->lastname: $subject";
    $success = email_to_user($to_user, $USER, $subject, $body);
    if ($success == "1") {  
        $result->msg = "E-mail successful sent to $to_user->firstname $to_user->lastname!";
        $result->status = 200;
    } else if ($success === "emailstop") {
        $result->msg = "E-mail of $to_user->firstname $to_user->lastname has been DISABLED! E-mail NOT sent";
        $result->status = 406;
    } else if (!$success) {
        $result->msg = "Error: E-mail unsuccessful.";
        $result->status = 404;
    }

    return $result;
}
