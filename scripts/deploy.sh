#!/bin/sh -e

RSYNC="rsync -avz --delete --exclude '*~'";
REMOTE_USER=root;
REMOTE_HOST=yamato;
REMOTE="$REMOTE_USER@$REMOTE_HOST";
REMOTE_DIR='/var/www/moodle.e-trauma.org/mod';

scriptdir=`dirname $0`;
rootdir="$scriptdir/..";
plugindir="$rootdir/orthoeman";
authoringtooldir="$plugindir/AuthoringTool/war/";
displaydir="$plugindir/Display";
wwwdir="$rootdir/www";

EXCLUDE_DISPLAY='--exclude Display';

$RSYNC --exclude AuthoringTool $EXCLUDE_DISPLAY "$plugindir" $REMOTE:$REMOTE_DIR;
$RSYNC --exclude WEB-INF "$authoringtooldir" $REMOTE:$REMOTE_DIR/orthoeman/AuthoringTool;
#echo $RSYNC "$wwwdir" $REMOTE:/var;
#echo $RSYNC "$plugindir/orthoeman.xsd" $REMOTE:/var/www;
