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
 * Capability definitions for the orthoeman module
 *
 * The capabilities are loaded into the database table when the module is
 * installed or updated. Whenever the capability definitions are updated,
 * the module version number should be bumped up.
 *
 * The system has four possible values for a capability:
 * CAP_ALLOW, CAP_PREVENT, CAP_PROHIBIT, and inherit (not set).
 *
 * It is important that capability names are unique. The naming convention
 * for capabilities that are specific to modules and blocks is as follows:
 *   [mod/block]/<plugin_name>:<capabilityname>
 *
 * component_name should be the same as the directory name of the mod or block.
 *
 * Core moodle capabilities are defined thus:
 *    moodle/<capabilityclass>:<capabilityname>
 *
 * Examples: mod/forum:viewpost
 *           block/recent_activity:view
 *           moodle/site:deleteuser
 *
 * The variable name for the capability definitions array is $capabilities
 *
 * @package    mod
 * @subpackage orthoeman
 * @copyright  2011 Your Name
 * @license    http://www.gnu.org/copyleft/gpl.html GNU GPL v3 or later
 *
 *
 * There is a distinction between the functions and capabilities. The primary roles
 * are student and teacher. Student should not be able to read the XML and the teacher
 * should not submit but he should be able to see the course displayed from start to end
 * so a phony submit is required.
 *
 * get_resource.php?id=XXX&resource_id=XXX
 * put_resource.php?id=XXX&type=XXX
 * get_view?id=XXX...
 * submit.php?id=XXX...
 *  id - orthoeman_id or course_id 
 *  resource_id the id of the resource as it stored in the XML. -1 to get the XML
 *  type - 	XML, VIDEO, IMAGE
 *  resource data are on post
 *  ...unknown required parameters
 *
 * capabilities are:
 *  VIEW
 *  SUBMIT
 *  READ
 *  WRITE
 *
 * function\role		teacher		student		OTHER
 *  get_resource_xml		READ		NO		NO
 *  get_resource_other		VIEW		VIEW		NO
 *  put_resource_xml		WRITE		NO		NO
 *  put_resource_other		WRITE		NO		NO
 *  view			VIEW		VIEW		NO
 *  submit			NO*		SUBMIT		NO
 *
 * NO* means
 *	* no error if role has READ capability. In that case the teacher can
 *	preview the full activity
 *	* error If no READ capability is set
 *
 */

defined('MOODLE_INTERNAL') || die();

$capabilities = array(

/***************************** remove these comment marks and modify the code as needed
    'mod/orthoeman:view' => array(
        'captype' => 'read',
        'contextlevel' => CONTEXT_MODULE,
        'legacy' => array(
            'guest' => CAP_ALLOW,
            'student' => CAP_ALLOW,
            'teacher' => CAP_ALLOW,
            'editingteacher' => CAP_ALLOW,
            'admin' => CAP_ALLOW
        )
    ),

    'mod/orthoeman:submit' => array(
        'riskbitmask' => RISK_SPAM,
        'captype' => 'write',
        'contextlevel' => CONTEXT_MODULE,
        'legacy' => array(
            'student' => CAP_ALLOW
        )
    ),
******************************/
    'mod/orthoeman:view' => array(
        'captype' => 'read',
        'contextlevel' => CONTEXT_MODULE,
        'archetypes' => array(
            'student' => CAP_ALLOW,
            'teacher' => CAP_ALLOW,
            'editingteacher' => CAP_ALLOW
        )
    ),

    'mod/orthoeman:submit' => array(
        'captype' => 'write',
        'contextlevel' => CONTEXT_MODULE,
        'archetypes' => array(
            'student' => CAP_ALLOW,
        )
    ),

    'mod/orthoeman:read' => array(
        'captype' => 'read',
        'contextlevel' => CONTEXT_MODULE,
        'archetypes' => array(
            'teacher' => CAP_ALLOW,
            'editingteacher' => CAP_ALLOW
        )
    ),

    'mod/orthoeman:write' => array(
        'captype' => 'write',
        'contextlevel' => CONTEXT_MODULE,
        'archetypes' => array(
            'editingteacher' => CAP_ALLOW
        )
    )
);
