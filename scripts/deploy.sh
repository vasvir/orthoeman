#!/bin/sh -e

RSYNC="rsync -avz --delete --exclude '*~'";
REMOTE_USER=root;
REMOTE_HOST=yamato;
REMOTE="$REMOTE_USER@$REMOTE_HOST";
REMOTE_DIR='/var/www/moodle.e-trauma.org/mod/orthoeman';

scriptdir=`dirname $0`;
plugindir="$scriptdir/..";
authoringtooldir="$plugindir/AuthoringTool/war/";

$RSYNC --exclude AuthoringTool --exclude scripts --exclude .git --exclude .gitignore --exclude doc "$plugindir" "$REMOTE:$REMOTE_DIR";
$RSYNC --exclude WEB-INF "$authoringtooldir" "$REMOTE:$REMOTE_DIR/AuthoringTool";

#TODO: what will happen to orthoeman.xsd
#TODO: documentation deployment
#TODO: release/zip creation

#wwwdir="$rootdir/www";
#echo $RSYNC "$wwwdir" $REMOTE:/var;
#echo $RSYNC "$plugindir/orthoeman.xsd" $REMOTE:/var/www;
