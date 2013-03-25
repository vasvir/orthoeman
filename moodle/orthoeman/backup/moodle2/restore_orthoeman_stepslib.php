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
 * Define all the restore steps that will be used by the restore_orthoeman_activity_task
 */

/**
 * Structure step to restore one orthoeman activity
 */
class restore_orthoeman_activity_structure_step extends restore_activity_structure_step {

    protected function define_structure() {

        $paths = array();
        $userinfo = $this->get_setting_value('userinfo');

        $paths[] = new restore_path_element('orthoeman', '/activity/orthoeman');
        if ($userinfo) {
            $paths[] = new restore_path_element('orthoeman_discussion', '/activity/orthoeman/discussions/discussion');
            $paths[] = new restore_path_element('orthoeman_post', '/activity/orthoeman/discussions/discussion/posts/post');
            $paths[] = new restore_path_element('orthoeman_rating', '/activity/orthoeman/discussions/discussion/posts/post/ratings/rating');
            $paths[] = new restore_path_element('orthoeman_subscription', '/activity/orthoeman/subscriptions/subscription');
            $paths[] = new restore_path_element('orthoeman_read', '/activity/orthoeman/readposts/read');
            $paths[] = new restore_path_element('orthoeman_track', '/activity/orthoeman/trackedprefs/track');
        }

        // Return the paths wrapped into standard activity structure
        return $this->prepare_activity_structure($paths);
    }

    protected function process_orthoeman($data) {
        global $DB;

        $data = (object)$data;
        $oldid = $data->id;
        $data->course = $this->get_courseid();

        $data->assesstimestart = $this->apply_date_offset($data->assesstimestart);
        $data->assesstimefinish = $this->apply_date_offset($data->assesstimefinish);
        if ($data->scale < 0) { // scale found, get mapping
            $data->scale = -($this->get_mappingid('scale', abs($data->scale)));
        }

        $newitemid = $DB->insert_record('orthoeman', $data);
        $this->apply_activity_instance($newitemid);
    }

    protected function process_orthoeman_discussion($data) {
        global $DB;

        $data = (object)$data;
        $oldid = $data->id;
        $data->course = $this->get_courseid();

        $data->orthoeman = $this->get_new_parentid('orthoeman');
        $data->timemodified = $this->apply_date_offset($data->timemodified);
        $data->timestart = $this->apply_date_offset($data->timestart);
        $data->timeend = $this->apply_date_offset($data->timeend);
        $data->userid = $this->get_mappingid('user', $data->userid);
        $data->groupid = $this->get_mappingid('group', $data->groupid);
        $data->usermodified = $this->get_mappingid('user', $data->usermodified);

        $newitemid = $DB->insert_record('orthoeman_discussions', $data);
        $this->set_mapping('orthoeman_discussion', $oldid, $newitemid);
    }

    protected function process_orthoeman_post($data) {
        global $DB;

        $data = (object)$data;
        $oldid = $data->id;

        $data->discussion = $this->get_new_parentid('orthoeman_discussion');
        $data->created = $this->apply_date_offset($data->created);
        $data->modified = $this->apply_date_offset($data->modified);
        $data->userid = $this->get_mappingid('user', $data->userid);
        // If post has parent, map it (it has been already restored)
        if (!empty($data->parent)) {
            $data->parent = $this->get_mappingid('orthoeman_post', $data->parent);
        }

        $newitemid = $DB->insert_record('orthoeman_posts', $data);
        $this->set_mapping('orthoeman_post', $oldid, $newitemid, true);

        // If !post->parent, it's the 1st post. Set it in discussion
        if (empty($data->parent)) {
            $DB->set_field('orthoeman_discussions', 'firstpost', $newitemid, array('id' => $data->discussion));
        }
    }

    protected function process_orthoeman_rating($data) {
        global $DB;

        $data = (object)$data;

        // Cannot use ratings API, cause, it's missing the ability to specify times (modified/created)
        $data->contextid = $this->task->get_contextid();
        $data->itemid    = $this->get_new_parentid('orthoeman_post');
        if ($data->scaleid < 0) { // scale found, get mapping
            $data->scaleid = -($this->get_mappingid('scale', abs($data->scaleid)));
        }
        $data->rating = $data->value;
        $data->userid = $this->get_mappingid('user', $data->userid);
        $data->timecreated = $this->apply_date_offset($data->timecreated);
        $data->timemodified = $this->apply_date_offset($data->timemodified);

        // We need to check that component and ratingarea are both set here.
        if (empty($data->component)) {
            $data->component = 'mod_orthoeman';
        }
        if (empty($data->ratingarea)) {
            $data->ratingarea = 'post';
        }

        $newitemid = $DB->insert_record('rating', $data);
    }

    protected function process_orthoeman_subscription($data) {
        global $DB;

        $data = (object)$data;
        $oldid = $data->id;

        $data->orthoeman = $this->get_new_parentid('orthoeman');
        $data->userid = $this->get_mappingid('user', $data->userid);

        $newitemid = $DB->insert_record('orthoeman_subscriptions', $data);
    }

    protected function process_orthoeman_read($data) {
        global $DB;

        $data = (object)$data;
        $oldid = $data->id;

        $data->orthoemanid = $this->get_new_parentid('orthoeman');
        $data->discussionid = $this->get_mappingid('orthoeman_discussion', $data->discussionid);
        $data->postid = $this->get_mappingid('orthoeman_post', $data->postid);
        $data->userid = $this->get_mappingid('user', $data->userid);

        $newitemid = $DB->insert_record('orthoeman_read', $data);
    }

    protected function process_orthoeman_track($data) {
        global $DB;

        $data = (object)$data;
        $oldid = $data->id;

        $data->orthoemanid = $this->get_new_parentid('orthoeman');
        $data->userid = $this->get_mappingid('user', $data->userid);

        $newitemid = $DB->insert_record('orthoeman_track_prefs', $data);
    }

    protected function after_execute() {
        global $DB;

        // Add orthoeman related files, no need to match by itemname (just internally handled context)
        $this->add_related_files('mod_orthoeman', 'intro', null);

        // If the orthoeman is of type 'single' and no discussion has been ignited
        // (non-userinfo backup/restore) create the discussion here, using orthoeman
        // information as base for the initial post.
        $orthoemanid = $this->task->get_activityid();
        $orthoemanrec = $DB->get_record('orthoeman', array('id' => $orthoemanid));
        if ($orthoemanrec->type == 'single' && !$DB->record_exists('orthoeman_discussions', array('orthoeman' => $orthoemanid))) {
            // Create single discussion/lead post from orthoeman data
            $sd = new stdclass();
            $sd->course   = $orthoemanrec->course;
            $sd->orthoeman    = $orthoemanrec->id;
            $sd->name     = $orthoemanrec->name;
            $sd->assessed = $orthoemanrec->assessed;
            $sd->message  = $orthoemanrec->intro;
            $sd->messageformat = $orthoemanrec->introformat;
            $sd->messagetrust  = true;
            $sd->mailnow  = false;
            $sdid = orthoeman_add_discussion($sd, null, $sillybyrefvar, $this->task->get_userid());
            // Mark the post as mailed
            $DB->set_field ('orthoeman_posts','mailed', '1', array('discussion' => $sdid));
            // Copy all the files from mod_foum/intro to mod_orthoeman/post
            $fs = get_file_storage();
            $files = $fs->get_area_files($this->task->get_contextid(), 'mod_orthoeman', 'intro');
            foreach ($files as $file) {
                $newfilerecord = new stdclass();
                $newfilerecord->filearea = 'post';
                $newfilerecord->itemid   = $DB->get_field('orthoeman_discussions', 'firstpost', array('id' => $sdid));
                $fs->create_file_from_storedfile($newfilerecord, $file);
            }
        }

        // Add post related files, matching by itemname = 'orthoeman_post'
        $this->add_related_files('mod_orthoeman', 'post', 'orthoeman_post');
        $this->add_related_files('mod_orthoeman', 'attachment', 'orthoeman_post');
    }
}
