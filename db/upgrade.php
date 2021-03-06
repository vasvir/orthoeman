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
 * This file keeps track of upgrades to the orthoeman module
 *
 * Sometimes, changes between versions involve alterations to database
 * structures and other major things that may break installations. The upgrade
 * function in this file will attempt to perform all the necessary actions to
 * upgrade your older installation to the current version. If there's something
 * it cannot do itself, it will tell you what you need to do.  The commands in
 * here will all be database-neutral, using the functions defined in DLL libraries.
 *
 * @package    mod
 * @subpackage orthoeman
 * @copyright  Vassilis Virvilis
 * @license    http://www.gnu.org/copyleft/gpl.html GNU GPL v3 or later
 */

defined('MOODLE_INTERNAL') || die();

/**
 * Execute orthoeman upgrade from the given old version
 *
 * @param int $oldversion
 * @return bool
 */
function xmldb_orthoeman_upgrade($oldversion) {
    global $DB;

    $dbman = $DB->get_manager(); // loads ddl manager and xmldb classes

    // And upgrade begins here. For each one, you'll need one
    // block of code similar to the next one. Please, delete
    // this comment lines once this file start handling proper
    // upgrade code.

    // if ($oldversion < YYYYMMDD00) { //New version in version.php
    //
    // }

    // Lines below (this included)  MUST BE DELETED once you get the first version
    // of your module ready to be installed. They are here only
    // for demonstrative purposes and to show how the orthoeman
    // iself has been upgraded.

    // For each upgrade block, the file orthoeman/version.php
    // needs to be updated . Such change allows Moodle to know
    // that this file has to be processed.

    // To know more about how to write correct DB upgrade scripts it's
    // highly recommended to read information available at:
    //   http://docs.moodle.org/en/Development:XMLDB_Documentation
    // and to play with the XMLDB Editor (in the admin menu) and its
    // PHP generation posibilities.

    // First example, some fields were added to install.xml on 2007/04/01
    if ($oldversion < 2007040100) {

        // Define field course to be added to orthoeman
        $table = new xmldb_table('orthoeman');
        $field = new xmldb_field('course', XMLDB_TYPE_INTEGER, '10', XMLDB_UNSIGNED, XMLDB_NOTNULL, null, '0', 'id');

        // Add field course
        if (!$dbman->field_exists($table, $field)) {
            $dbman->add_field($table, $field);
        }

        // Define field intro to be added to orthoeman
        $table = new xmldb_table('orthoeman');
        $field = new xmldb_field('intro', XMLDB_TYPE_TEXT, 'medium', null, null, null, null,'name');

        // Add field intro
        if (!$dbman->field_exists($table, $field)) {
            $dbman->add_field($table, $field);
        }

        // Define field introformat to be added to orthoeman
        $table = new xmldb_table('orthoeman');
        $field = new xmldb_field('introformat', XMLDB_TYPE_INTEGER, '4', XMLDB_UNSIGNED, XMLDB_NOTNULL, null, '0',
            'intro');

        // Add field introformat
        if (!$dbman->field_exists($table, $field)) {
            $dbman->add_field($table, $field);
        }

        // Once we reach this point, we can store the new version and consider the module
        // upgraded to the version 2007040100 so the next time this block is skipped
        upgrade_mod_savepoint(true, 2007040100, 'orthoeman');
    }

    // Second example, some hours later, the same day 2007/04/01
    // two more fields and one index were added to install.xml (note the micro increment
    // "01" in the last two digits of the version
    if ($oldversion < 2007040101) {

        // Define field timecreated to be added to orthoeman
        $table = new xmldb_table('orthoeman');
        $field = new xmldb_field('timecreated', XMLDB_TYPE_INTEGER, '10', XMLDB_UNSIGNED, XMLDB_NOTNULL, null, '0',
            'introformat');

        // Add field timecreated
        if (!$dbman->field_exists($table, $field)) {
            $dbman->add_field($table, $field);
        }

        // Define field timemodified to be added to orthoeman
        $table = new xmldb_table('orthoeman');
        $field = new xmldb_field('timemodified', XMLDB_TYPE_INTEGER, '10', XMLDB_UNSIGNED, XMLDB_NOTNULL, null, '0',
            'timecreated');

        // Add field timemodified
        if (!$dbman->field_exists($table, $field)) {
            $dbman->add_field($table, $field);
        }

        // Define index course (not unique) to be added to orthoeman
        $table = new xmldb_table('orthoeman');
        $index = new xmldb_index('courseindex', XMLDB_INDEX_NOTUNIQUE, array('course'));

        // Add index to course field
        if (!$dbman->index_exists($table, $index)) {
            $dbman->add_index($table, $index);
        }

        // Another save point reached
        upgrade_mod_savepoint(true, 2007040101, 'orthoeman');
    }

    // Third example, the next day, 2007/04/02 (with the trailing 00), some actions were performed to install.php,
    // related with the module
    if ($oldversion < 2007040200) {

        // insert here code to perform some actions (same as in install.php)

        upgrade_mod_savepoint(true, 2007040200, 'orthoeman');
    }


    // try to get a hook on the update procedure
    if ($oldversion < 2012111402) {

        // Define field intro to be added to orthoeman
        $table = new xmldb_table('orthoeman');
        $field = new xmldb_field('intro', XMLDB_TYPE_TEXT, 'big', null, null, null, null,'name');

        // Add field intro
        if (!$dbman->field_exists($table, $field)) {
            $dbman->add_field($table, $field);
        }

        // Define field introformat to be added to orthoeman
        $table = new xmldb_table('orthoeman');
        $field = new xmldb_field('introformat', XMLDB_TYPE_INTEGER, '4', XMLDB_UNSIGNED, XMLDB_NOTNULL, null, '0',
            'intro');

        // Add field introformat
        if (!$dbman->field_exists($table, $field)) {
            $dbman->add_field($table, $field);
        }


        // Define field timeout to be added to orthoeman
        $table = new xmldb_table('orthoeman');
        $field = new xmldb_field('timeout', XMLDB_TYPE_INTEGER, '10', XMLDB_UNSIGNED, XMLDB_NOTNULL, null, '7200',
            'introformat');

        // Add field introformat
        if (!$dbman->field_exists($table, $field)) {
            $dbman->add_field($table, $field);
        }


        // insert here code to perform some actions (same as in install.php)
        upgrade_mod_savepoint(true, 2012111402, 'orthoeman');
    }

    if ($oldversion < 2012112100) {
        // define field timeout to be added to orthoeman
        $table = new xmldb_table('orthoeman');
        $field = new xmldb_field('cruise', XMLDB_TYPE_INTEGER, '4', XMLDB_UNSIGNED, XMLDB_NOTNULL, null, '0', 'timeout');

        // Add field introformat
        if (!$dbman->field_exists($table, $field)) {
            $dbman->add_field($table, $field);
        }
    }


    if ($oldversion < 2013021903) {
       // Define table orthoeman_answer to be created
        $table = new xmldb_table('orthoeman_answer');

        // Adding fields to table orthoeman_answer
        $table->add_field('id', XMLDB_TYPE_INTEGER, '10', XMLDB_UNSIGNED, XMLDB_NOTNULL, XMLDB_SEQUENCE, null);
        $table->add_field('orthoeman_id', XMLDB_TYPE_INTEGER, '10', XMLDB_UNSIGNED, XMLDB_NOTNULL, null, null);
        $table->add_field('user_id', XMLDB_TYPE_INTEGER, '10', XMLDB_UNSIGNED, XMLDB_NOTNULL, null, null);
        $table->add_field('page_id', XMLDB_TYPE_INTEGER, '10', XMLDB_UNSIGNED, XMLDB_NOTNULL, null, null);
        $table->add_field('type', XMLDB_TYPE_INTEGER, '4', XMLDB_UNSIGNED, null, null, null);
        $table->add_field('answer', XMLDB_TYPE_CHAR, '512', null, null, null, null);

        // Adding keys to table orthoeman_answer
        $table->add_key('primary', XMLDB_KEY_PRIMARY, array('id'));

        // Adding indexes to table orthoeman_answer
        $table->add_index('orthoeman_id', XMLDB_INDEX_NOTUNIQUE, array('orthoeman_id'));
        $table->add_index('user_id', XMLDB_INDEX_NOTUNIQUE, array('user_id'));

        // Conditionally launch create table for orthoeman_answer
        if (!$dbman->table_exists($table)) {
            $dbman->create_table($table);
        }

        // orthoeman savepoint reached
        upgrade_mod_savepoint(true, 2013021903, 'orthoeman');
    }

    if ($oldversion < 2013031000) {
        // Define field timesubmitted to be added to orthoeman_answer
        $table = new xmldb_table('orthoeman_answer');
        $field = new xmldb_field('timesubmitted', XMLDB_TYPE_INTEGER, '10', XMLDB_UNSIGNED, XMLDB_NOTNULL, null, '0', 'answer');

        // Conditionally launch add field timesubmitted
        if (!$dbman->field_exists($table, $field)) {
            $dbman->add_field($table, $field);
        }

        // orthoeman savepoint reached
        upgrade_mod_savepoint(true, 2013031000, 'orthoeman');
    }


    if ($oldversion < 2013120500) {
	// Changing type of field answer on table orthoeman_answer to text
	$table = new xmldb_table('orthoeman_answer');
	$field = new xmldb_field('answer', XMLDB_TYPE_TEXT, 'big', null, null, null, null, 'type');

	// Launch change of type for field answer
	$dbman->change_field_type($table, $field);

	// orthoeman savepoint reached
	upgrade_mod_savepoint(true, 2013120500, 'orthoeman');
    }

    // And that's all. Please, examine and understand the 3 example blocks above. Also
    // it's interesting to look how other modules are using this script. Remember that
    // the basic idea is to have "blocks" of code (each one being executed only once,
    // when the module version (version.php) is updated.

    // Lines above (this included) MUST BE DELETED once you get the first version of
    // yout module working. Each time you need to modify something in the module (DB
    // related, you'll raise the version and add one upgrade block here.

    // Final return of upgrade result (true, all went good) to Moodle.
    return true;
}
