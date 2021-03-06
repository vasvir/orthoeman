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
 * Define all the backup steps that will be used by the backup_orthoeman_activity_task
 */

/**
 * Define the complete orthoeman structure for backup, with file and id annotations
 */
class backup_orthoeman_activity_structure_step extends backup_activity_structure_step {

    protected function define_structure() {

        // To know if we are including userinfo
        $userinfo = $this->get_setting_value('userinfo');

        // Define each element separated

        $orthoeman = new backup_nested_element('orthoeman', array('id'), array(
            'name', 'intro', 'introformat', 'timeout', 'cruise', 'timecreated', 'timemodified'));

        $resources = new backup_nested_element('resources');

        $resource = new backup_nested_element('resource', array('id'), array(
            'orthoeman_id', 'type', 'md5', 'hex_data', 'content_type',
            'codecs', 'parent_id'));

        $answers = new backup_nested_element('answers');

        $answer = new backup_nested_element('answer', array('id'), array(
            'orthoeman_id', 'user_id', 'page_id', 'type', 'answer',
            'timesubmitted'));

        // Build the tree

        $orthoeman->add_child($resources);
        $resources->add_child($resource);

        $orthoeman->add_child($answers);
        $answers->add_child($answer);

        // Define sources
        $orthoeman->set_source_table('orthoeman', array('id' => backup::VAR_ACTIVITYID));
        $resource->set_source_sql('SELECT id, orthoeman_id, type, md5,
            hex(data) AS hex_data, content_type, codecs, parent_id FROM {orthoeman_resource}
            WHERE orthoeman_id = ? ORDER BY id', array(backup::VAR_ACTIVITYID));

        if ($userinfo) {
            $answer->set_source_sql('SELECT id, orthoeman_id, user_id, 
                page_id, type, answer, timesubmitted FROM {orthoeman_answer}
                WHERE orthoeman_id = ? ORDER BY id', array(backup::VAR_ACTIVITYID));
        }

        //annotate ids
        $answer->annotate_ids('user', 'user_id');

        // Return the root element (orthoeman), wrapped into standard activity structure
        return $this->prepare_activity_structure($orthoeman);
    }
}
