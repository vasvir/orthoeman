#!/bin/sh -e

RSYNC="rsync -avz --delete --exclude '*~'";
REMOTE_USER=root;
REMOTE_HOST=yamato;
REMOTE="$REMOTE_USER@$REMOTE_HOST";
REMOTE_DIR='/var/www/moodle.e-trauma.org/mod/orthoeman';

scriptdir=`dirname $0`;
rootdir="$scriptdir/..";
plugindir="$rootdir";
authoringtooldir="$plugindir/AuthoringTool/war/";

$RSYNC --exclude AuthoringTool --exclude scripts --exclude .git --exclude .gitignore --exclude doc "$plugindir" "$REMOTE:$REMOTE_DIR";
$RSYNC --exclude WEB-INF "$authoringtooldir" "$REMOTE:$REMOTE_DIR/AuthoringTool";

wwwdir="$rootdir/www";
#echo $RSYNC "$wwwdir" $REMOTE:/var;
#echo $RSYNC "$plugindir/orthoeman.xsd" $REMOTE:/var/www;
