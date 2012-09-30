#!/bin/sh -e


scriptdir=`dirname $0`;
plugindir="$scriptdir/../moodle/orthoeman";
rsync -av "$plugindir" root@orthoeman:/usr/share/moodle/mod/;
ssh root@orthoeman '
	chown root.www-data /usr/share/moodle/mod/orthoeman/db/install.xml /usr/share/moodle/mod/orthoeman/db;
	chmod 664 /usr/share/moodle/mod/orthoeman/db/install.xml;
	chmod 775 /usr/share/moodle/mod/orthoeman/db;
';
