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
    private $resource_id_map;

    protected function define_structure() {

        $paths = array();
        $userinfo = $this->get_setting_value('userinfo');

        $paths[] = new restore_path_element('orthoeman', '/activity/orthoeman');
        $paths[] = new restore_path_element('orthoeman_resource', '/activity/orthoeman/resources/resource');
        if ($userinfo) {
            $paths[] = new restore_path_element('orthoeman_answer', '/activity/orthoeman/answers/answer');
        }

        $this->resource_id_map = array();

        // Return the paths wrapped into standard activity structure
        return $this->prepare_activity_structure($paths);
    }

    protected function process_orthoeman($data) {
        global $DB;

        $data = (object)$data;
        $oldid = $data->id;
        $data->course = $this->get_courseid();

        $newitemid = $DB->insert_record('orthoeman', $data);
        $this->apply_activity_instance($newitemid);
    }

    protected function process_orthoeman_resource($data) {
        global $DB;

        $data = (object)$data;
        $oldid = $data->id;
        $data->course = $this->get_courseid();

        $data->orthoeman_id = $this->get_new_parentid('orthoeman');

        $newitemid = $DB->insert_record('orthoeman_resource', $data);
        $this->resource_id_map[$oldid] = $newitemid;
        $this->set_mapping('orthoeman_resource', $oldid, $newitemid);
    }

    protected function process_orthoeman_answer($data) {
        global $DB;

        $data = (object)$data;
        $oldid = $data->id;

        $data->orthoeman_id = $this->get_new_parentid('orthoeman');
        $data->user_id = $this->get_mappingid('user', $data->user_id);

        $newitemid = $DB->insert_record('orthoeman_answer', $data);
    }

    protected function after_execute() {
        error_log(json_encode($this->resource_id_map));
        //error_log(json_encode($this->get_mapping('orthoeman_resource', 119)));
    }
}
