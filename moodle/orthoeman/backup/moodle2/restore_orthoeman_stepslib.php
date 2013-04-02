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
require_once(dirname(__FILE__).'/../../lib.php');

class restore_orthoeman_activity_structure_step extends restore_activity_structure_step {
    private $resource_id_map;
    private $xml_id_map;
    private $orthoeman_id;

    protected function define_structure() {

        $paths = array();
        $userinfo = $this->get_setting_value('userinfo');

        $paths[] = new restore_path_element('orthoeman', '/activity/orthoeman');
        $paths[] = new restore_path_element('orthoeman_resource', '/activity/orthoeman/resources/resource');
        if ($userinfo) {
            $paths[] = new restore_path_element('orthoeman_answer', '/activity/orthoeman/answers/answer');
        }

        $this->resource_id_map = array();
        $this->xml_id_map = array();

        // Return the paths wrapped into standard activity structure
        return $this->prepare_activity_structure($paths);
    }

    protected function process_orthoeman($data) {
        global $DB;

        $data = (object)$data;
        $oldid = $data->id;
        $data->course = $this->get_courseid();

        $newitemid = $DB->insert_record('orthoeman', $data);
        $this->orthoeman_id = $newitemid;
        $this->apply_activity_instance($newitemid);
    }

    protected function process_orthoeman_resource($data) {
        global $DB, $TYPE_XML_VALUE;

        $data = (object)$data;
        $oldid = $data->id;
        $data->course = $this->get_courseid();
        $data->orthoeman_id = $this->get_new_parentid('orthoeman');
        $data->data = $data->hex_data;
        //error_log("resource: " . json_encode($data));

        $newitemid = $DB->insert_record('orthoeman_resource', $data);
        $this->resource_id_map[$oldid] = $newitemid;
        //error_log("data->type: " . $data->type . " TYPE_XML_VALUE: " . $TYPE_XML_VALUE);
        if ($data->type == $TYPE_XML_VALUE) {
            $this->xml_id_map[$oldid] = $newitemid;
        }
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
        global $DB;

        //error_log("orthoeman_id: " . json_encode($this->orthoeman_id));
        //error_log("resources: " . json_encode($this->resource_id_map));
        //error_log("xmls: " . json_encode($this->xml_id_map));

        // unhex the data
        $DB->execute('UPDATE {orthoeman_resource} SET data = unhex(data) 
            WHERE orthoeman_id = ? AND id IN (' . implode(', ', $this->resource_id_map)  . ')', array($this->orthoeman_id));

        foreach ($this->resource_id_map as $oldid => $newid) {
            // map the parent ids
            $DB->execute('UPDATE {orthoeman_resource} SET parent_id = ? 
                WHERE orthoeman_id = ? AND parent_id = ?', array($newid, $this->orthoeman_id, $oldid));
        }

        // map the ids inside the xml
        foreach ($this->xml_id_map as $old_xml_id => $new_xml_id) {
            $replace_str = 'data';
            $replace_cnt = 0;
            foreach ($this->resource_id_map as $oldid => $newid) {
                if ($oldid == $old_xml_id)
                    continue;
                $replace_str = "REPLACE($replace_str, 'id=\"$oldid\"', 'id=\"$newid\"')";
                $replace_cnt++;
            }
            if ($replace_cnt) {
                $DB->execute('UPDATE {orthoeman_resource} SET data = ' . $replace_str  . '
                    WHERE orthoeman_id = ? AND id = ? AND type = 0', array($this->orthoeman_id, $new_xml_id));
                $DB->execute('UPDATE {orthoeman_resource} SET md5 = md5(data) 
                    WHERE orthoeman_id = ? AND id = ? AND type = 0', array($this->orthoeman_id, $new_xml_id));
                //error_log("replace_str: " . $replace_str);
            }
        }

        //error_log(json_encode($this->get_mapping('orthoeman_resource', $old_xml_id)));
    }
}
