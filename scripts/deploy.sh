#!/bin/sh -e

RSYNC="rsync -avz --delete --exclude '*~'";

scriptdir=`dirname $0`;
plugindir="$scriptdir/../moodle/orthoeman";
authoringtooldir="$scriptdir/../AuthoringTool/war/";
displaydir="$scriptdir/../Display";

$RSYNC --exclude AuthoringTool --exclude Display "$plugindir" www-data@orthoeman:/usr/share/moodle/mod/;
$RSYNC --exclude WEB-INF "$authoringtooldir" www-data@orthoeman:/usr/share/moodle/mod/orthoeman/AuthoringTool;
$RSYNC "$displaydir" www-data@orthoeman:/usr/share/moodle/mod/orthoeman;
