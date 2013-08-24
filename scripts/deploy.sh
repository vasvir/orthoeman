#!/bin/sh -e

RSYNC="rsync -avz --delete --exclude '*~'";

scriptdir=`dirname $0`;
rootdir="$scriptdir/..";
plugindir="$rootdir/orthoeman";
authoringtooldir="$plugindir/AuthoringTool/war/";
displaydir="$plugindir/Display";
wwwdir="$rootdir/www";

EXCLUDE_DISPLAY='--exclude Display';

$RSYNC --exclude AuthoringTool $EXCLUDE_DISPLAY "$plugindir" www-data@orthoeman:/usr/share/moodle/mod;
$RSYNC --exclude WEB-INF "$authoringtooldir" www-data@orthoeman:/usr/share/moodle/mod/orthoeman/AuthoringTool;
$RSYNC "$wwwdir" www-data@orthoeman:/var;
$RSYNC "$plugindir/orthoeman.xsd" www-data@orthoeman:/var/www;
