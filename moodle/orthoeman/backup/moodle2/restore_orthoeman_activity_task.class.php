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

defined('MOODLE_INTERNAL') || die();

//require_once($CFG->dirroot . '/mod/orthoeman/backup/moodle2/restore_orthoeman_stepslib.php'); // Because it exists (must)

/**
 * orthoeman restore task that provides all the settings and steps to perform one
 * complete restore of the activity
 */
class restore_orthoeman_activity_task extends restore_activity_task {

    /**
     * Define (add) particular settings this activity can have
     */
    protected function define_my_settings() {
        // No particular settings for this activity
    }

    /**
     * Define (add) particular steps this activity can have
     */
    protected function define_my_steps() {
        // Choice only has one structure step
        $this->add_step(new restore_orthoeman_activity_structure_step('orthoeman_structure', 'orthoeman.xml'));
    }

    /**
     * Define the contents in the activity that must be
     * processed by the link decoder
     */
    static public function define_decode_contents() {
        $contents = array();

        $contents[] = new restore_decode_content('orthoeman', array('intro'), 'orthoeman');
        $contents[] = new restore_decode_content('orthoeman_posts', array('message'), 'orthoeman_post');

        return $contents;
    }

    /**
     * Define the decoding rules for links belonging
     * to the activity to be executed by the link decoder
     */
    static public function define_decode_rules() {
        $rules = array();

        // List of orthoemans in course
        $rules[] = new restore_decode_rule('ORTHOEMANINDEX', '/mod/orthoeman/index.php?id=$1', 'course');
        // orthoeman by cm->id and orthoeman->id
        $rules[] = new restore_decode_rule('ORTHOEMANVIEWBYID', '/mod/orthoeman/view.php?id=$1', 'course_module');
        $rules[] = new restore_decode_rule('ORTHOEMANVIEWBYF', '/mod/orthoeman/view.php?f=$1', 'orthoeman');
        // Link to orthoeman discussion
        $rules[] = new restore_decode_rule('ORTHOEMANDISCUSSIONVIEW', '/mod/orthoeman/discuss.php?d=$1', 'orthoeman_discussion');
        // Link to discussion with parent and with anchor posts
        $rules[] = new restore_decode_rule('ORTHOEMANDISCUSSIONVIEWPARENT', '/mod/orthoeman/discuss.php?d=$1&parent=$2',
                                           array('orthoeman_discussion', 'orthoeman_post'));
        $rules[] = new restore_decode_rule('ORTHOEMANDISCUSSIONVIEWINSIDE', '/mod/orthoeman/discuss.php?d=$1#$2',
                                           array('orthoeman_discussion', 'orthoeman_post'));

        return $rules;
    }

    /**
     * Define the restore log rules that will be applied
     * by the {@link restore_logs_processor} when restoring
     * orthoeman logs. It must return one array
     * of {@link restore_log_rule} objects
     */
    static public function define_restore_log_rules() {
        $rules = array();

        $rules[] = new restore_log_rule('orthoeman', 'add', 'view.php?id={course_module}', '{orthoeman}');
        $rules[] = new restore_log_rule('orthoeman', 'update', 'view.php?id={course_module}', '{orthoeman}');
        $rules[] = new restore_log_rule('orthoeman', 'view', 'view.php?id={course_module}', '{orthoeman}');
        $rules[] = new restore_log_rule('orthoeman', 'view orthoeman', 'view.php?id={course_module}', '{orthoeman}');
        $rules[] = new restore_log_rule('orthoeman', 'mark read', 'view.php?f={orthoeman}', '{orthoeman}');
        $rules[] = new restore_log_rule('orthoeman', 'start tracking', 'view.php?f={orthoeman}', '{orthoeman}');
        $rules[] = new restore_log_rule('orthoeman', 'stop tracking', 'view.php?f={orthoeman}', '{orthoeman}');
        $rules[] = new restore_log_rule('orthoeman', 'subscribe', 'view.php?f={orthoeman}', '{orthoeman}');
        $rules[] = new restore_log_rule('orthoeman', 'unsubscribe', 'view.php?f={orthoeman}', '{orthoeman}');
        $rules[] = new restore_log_rule('orthoeman', 'subscriber', 'subscribers.php?id={orthoeman}', '{orthoeman}');
        $rules[] = new restore_log_rule('orthoeman', 'subscribers', 'subscribers.php?id={orthoeman}', '{orthoeman}');
        $rules[] = new restore_log_rule('orthoeman', 'view subscribers', 'subscribers.php?id={orthoeman}', '{orthoeman}');
        $rules[] = new restore_log_rule('orthoeman', 'add discussion', 'discuss.php?d={orthoeman_discussion}', '{orthoeman_discussion}');
        $rules[] = new restore_log_rule('orthoeman', 'view discussion', 'discuss.php?d={orthoeman_discussion}', '{orthoeman_discussion}');
        $rules[] = new restore_log_rule('orthoeman', 'move discussion', 'discuss.php?d={orthoeman_discussion}', '{orthoeman_discussion}');
        $rules[] = new restore_log_rule('orthoeman', 'delete discussi', 'view.php?id={course_module}', '{orthoeman}',
                                        null, 'delete discussion');
        $rules[] = new restore_log_rule('orthoeman', 'delete discussion', 'view.php?id={course_module}', '{orthoeman}');
        $rules[] = new restore_log_rule('orthoeman', 'add post', 'discuss.php?d={orthoeman_discussion}&parent={orthoeman_post}', '{orthoeman_post}');
        $rules[] = new restore_log_rule('orthoeman', 'update post', 'discuss.php?d={orthoeman_discussion}#p{orthoeman_post}&parent={orthoeman_post}', '{orthoeman_post}');
        $rules[] = new restore_log_rule('orthoeman', 'prune post', 'discuss.php?d={orthoeman_discussion}', '{orthoeman_post}');
        $rules[] = new restore_log_rule('orthoeman', 'delete post', 'discuss.php?d={orthoeman_discussion}', '[post]');

        return $rules;
    }

    /**
     * Define the restore log rules that will be applied
     * by the {@link restore_logs_processor} when restoring
     * course logs. It must return one array
     * of {@link restore_log_rule} objects
     *
     * Note this rules are applied when restoring course logs
     * by the restore final task, but are defined here at
     * activity level. All them are rules not linked to any module instance (cmid = 0)
     */
    static public function define_restore_log_rules_for_course() {
        $rules = array();

        $rules[] = new restore_log_rule('orthoeman', 'view orthoemans', 'index.php?id={course}', null);
        $rules[] = new restore_log_rule('orthoeman', 'subscribeall', 'index.php?id={course}', '{course}');
        $rules[] = new restore_log_rule('orthoeman', 'unsubscribeall', 'index.php?id={course}', '{course}');
        $rules[] = new restore_log_rule('orthoeman', 'user report', 'user.php?course={course}&id={user}&mode=[mode]', '{user}');
        $rules[] = new restore_log_rule('orthoeman', 'search', 'search.php?id={course}&search=[searchenc]', '[search]');

        return $rules;
    }
}
