<?php
session_start();
$action = $_GET["action"]; // 1- transform xml to json

switch ($action)
{
    case "1":
        $lessonid = $_GET["lessonid"];
        $xml = simplexml_load_file("XML/XMLFile1.xml");
        echo str_replace("@attributes","attributes",json_encode($xml));
        break;

}


?>