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

require_once($CFG->dirroot . '/mod/orthoeman/backup/moodle2/backup_orthoeman_stepslib.php'); // Because it exists (must)
require_once($CFG->dirroot . '/mod/orthoeman/backup/moodle2/backup_orthoeman_settingslib.php'); // Because it exists (optional)

/**
 * orthoeman backup task that provides all the settings and steps to perform one
 * complete backup of the activity
 */
class backup_orthoeman_activity_task extends backup_activity_task {

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
        // orthoeman only has one structure step
        $this->add_step(new backup_orthoeman_activity_structure_step('orthoeman structure', 'orthoeman.xml'));
    }

    /**
     * Code the transformations to perform in the activity in
     * order to get transportable (encoded) links
     */
    static public function encode_content_links($content) {
        global $CFG;

        $base = preg_quote($CFG->wwwroot,"/");

        // Link to the list of orthoemans
        $search="/(".$base."\/mod\/orthoeman\/index.php\?id\=)([0-9]+)/";
        $content= preg_replace($search, '$@ORTHOEMANINDEX*$2@$', $content);

        // Link to orthoeman view by moduleid
        $search="/(".$base."\/mod\/orthoeman\/view.php\?id\=)([0-9]+)/";
        $content= preg_replace($search, '$@ORTHOEMANVIEWBYID*$2@$', $content);

        // Link to orthoeman view by orthoemanid
        $search="/(".$base."\/mod\/orthoeman\/view.php\?f\=)([0-9]+)/";
        $content= preg_replace($search, '$@ORTHOEMANVIEWBYF*$2@$', $content);

        // Link to orthoeman discussion with parent syntax
        $search="/(".$base."\/mod\/orthoeman\/discuss.php\?d\=)([0-9]+)\&parent\=([0-9]+)/";
        $content= preg_replace($search, '$@ORTHOEMANDISCUSSIONVIEWPARENT*$2*$3@$', $content);

        // Link to orthoeman discussion with relative syntax
        $search="/(".$base."\/mod\/orthoeman\/discuss.php\?d\=)([0-9]+)\#([0-9]+)/";
        $content= preg_replace($search, '$@ORTHOEMANDISCUSSIONVIEWINSIDE*$2*$3@$', $content);

        // Link to orthoeman discussion by discussionid
        $search="/(".$base."\/mod\/orthoeman\/discuss.php\?d\=)([0-9]+)/";
        $content= preg_replace($search, '$@ORTHOEMANDISCUSSIONVIEW*$2@$', $content);

        return $content;
    }
}
