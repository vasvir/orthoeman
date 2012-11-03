<?php

require_once(dirname(dirname(dirname(__FILE__))).'/config.php');
require_once(dirname(__FILE__).'/lib.php');

$id = optional_param('id', 0, PARAM_INT); // course_module ID, or
$n  = optional_param('n', 0, PARAM_INT);  // orthoeman instance ID - it should be named as the first character of the module
$resource_id = optional_param('resource_id', -1, PARAM_INT); // resource_id -1 gets the XML

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

require_capability("mod/orthoeman:view", $context);

if ($resource_id == -1) {
  require_capability("mod/orthoeman:read", $context);
}

add_to_log($course->id, 'orthoeman', 'get_resource', "get_resource.php?id={$cm->id}", $orthoeman->name, $cm->id);
$resource_rec = get_database_data($orthoeman->id, $resource_id);
header('Content-type: ' . $resource_rec->content_type);
echo $resource_rec->data;


