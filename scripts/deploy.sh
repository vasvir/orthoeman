#!/bin/sh -e

RSYNC="rsync -avz --delete --exclude '*~'";

scriptdir=`dirname $0`;
rootdir="$scriptdir/..";
plugindir="$rootdir/moodle/orthoeman";
authoringtooldir="$rootdir/AuthoringTool/war/";
displaydir="$rootdir/Display";
contentdir="$rootdir/Content";
wwwdir="$rootdir/www";

$RSYNC --exclude AuthoringTool --exclude Display "$plugindir" www-data@orthoeman:/usr/share/moodle/mod;
$RSYNC --exclude WEB-INF "$authoringtooldir" www-data@orthoeman:/usr/share/moodle/mod/orthoeman/AuthoringTool;
#$RSYNC "$displaydir" www-data@orthoeman:/usr/share/moodle/mod/orthoeman;
$RSYNC "$wwwdir" www-data@orthoeman:/var/www;
$RSYNC "$contentdir/XML/orthoeman.xsd" www-data@orthoeman:/var/www;
