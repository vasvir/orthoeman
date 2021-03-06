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
 * The main orthoeman configuration form
 *
 * It uses the standard core Moodle formslib. For more info about them, please
 * visit: http://docs.moodle.org/en/Development:lib/formslib.php
 *
 * moodle plugin skeleton file
 *
 * @package    mod
 * @subpackage orthoeman
 * @copyright  Vassilis Virvilis
 * @license    http://www.gnu.org/copyleft/gpl.html GNU GPL v3 or later
 */

defined('MOODLE_INTERNAL') || die();

require_once($CFG->dirroot.'/course/moodleform_mod.php');
require_once(dirname(__FILE__).'/lib.php');

/**
 * Module instance settings form
 */
class mod_orthoeman_mod_form extends moodleform_mod {

    /**
     * Defines forms elements
     */
    public function definition() {
        $mform = $this->_form;
        $cm = $this->_cm;

        if (isset($cm)) {
            // don't show anything when creating the activity
            $authoring_tool_url = "../mod/orthoeman/AuthoringTool/AuthoringTool.html?id=$cm->id";
            $mform->addElement('html', get_orthoeman_frame($authoring_tool_url, "none", true));
        }

        //-------------------------------------------------------------------------------
        // Adding the "general" fieldset, where all the common settings are showed
        $mform->addElement('header', 'general', get_string('general', 'form'));

        // Adding the standard "name" field
        $mform->addElement('text', 'name', get_string('orthoemanname', 'orthoeman'), array('size'=>'64'));
        if (!empty($CFG->formatstringstriptags)) {
            $mform->setType('name', PARAM_TEXT);
        } else {
            $mform->setType('name', PARAM_CLEAN);
        }

        $mform->addRule('name', null, 'required', null, 'client');
        $mform->addRule('name', get_string('maximumchars', '', 255), 'maxlength', 255, 'client');
        $mform->addHelpButton('name', 'orthoemanname', 'orthoeman');

        // Adding the standard "intro" and "introformat" fields
        $this->add_intro_editor();

        $mform->addElement('duration', 'timeout', get_string('timeout', 'orthoeman'));
        $mform->setDefault('timeout', 7200);
        $mform->addHelpButton('timeout', 'timeout', 'orthoeman');

        $mform->addElement('checkbox', 'cruise', get_string('cruise', 'orthoeman'));
        $mform->addHelpButton('cruise', 'cruise', 'orthoeman');

        //-------------------------------------------------------------------------------
        // add standard elements, common to all modules
        $this->standard_coursemodule_elements();
        //-------------------------------------------------------------------------------
        // add standard buttons, common to all modules
        $this->add_action_buttons();
    }
}
