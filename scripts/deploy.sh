#!/bin/sh -e

RSYNC="rsync -avz --delete --exclude '*~'";

scriptdir=`dirname $0`;
rootdir="$scriptdir/..";
plugindir="$rootdir/orthoeman";
authoringtooldir="$rootdir/AuthoringTool/war/";
displaydir="$rootdir/Display";
wwwdir="$rootdir/www";

$RSYNC --exclude AuthoringTool --exclude Display "$plugindir" www-data@orthoeman:/usr/share/moodle/mod;
$RSYNC --exclude WEB-INF "$authoringtooldir" www-data@orthoeman:/usr/share/moodle/mod/orthoeman/AuthoringTool;
#$RSYNC "$displaydir" www-data@orthoeman:/usr/share/moodle/mod/orthoeman;
$RSYNC "$wwwdir" www-data@orthoeman:/var;
$RSYNC "$plugindir/orthoeman.xsd" www-data@orthoeman:/var/www;
