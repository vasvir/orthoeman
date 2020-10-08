#!/bin/sh -e

usage() {
	[ -n "$1" ] && echo "$1" >&2;
	echo "Usage: $0 [-h|--help] [-v|--verbose] [-d|--debug] [-p|--port {port (default: 9876)}] EntryPoint";
	exit $2;
}

while true; do
    case "$1" in
	--) done_options=1;;

	-h | --help) usage;;	

	-d | --debug) 
		DEBUG="-style DETAILED";;
	-v | --verbose) 
		VERBOSE="-strict -logLevel TRACE";;
	-p | --port)
		PORT_OPTION="-port $2";
		shift;;

	-*) usage "unknown option $1" 1;;
	
	*) break;;
    esac
    shift	# each time around, pop off the option
done

[ -f .classpath ] || { usage "You have to run it from a project's root directory" 2;}

BASEDIR=$(dirname $0)

# Include files
. $BASEDIR/update-lib.sh

entry_point="$1";

if [ -z "$entry_point" ]; then
	srcpath=`get_srcpaths_s .classpath`;
	#echo $srcpath;
	echo "Possible EntryPoint are: ";
	find $srcpath -name '*.gwt.xml' | sed -e 's,./src/,	,g' -e 's,/,.,g' -e 's,\.gwt\.xml,,g';
	usage "No EntryPointfound: $entry_point" 3;
fi

classpath="`get_build_dir .classpath`";
libraries="`get_libraries .classpath`";
for library in $libraries; do
	libdir="$BASEDIR/../lib/$library/";
	classpath="$classpath:$libdir"'*';
done;
classpath="$classpath:$BASEDIR/../../../../gwt/gwt-2.9.0/"'*';
projects=`get_projects .classpath`;
for project in $projects; do
	classpath="$classpath:../$project/build";
done;

sources="";
srcpaths=`get_srcpaths .classpath`;
for srcpath in $srcpaths; do
	sources="$sources -src $srcpath";
done;

java -Xmx2512m -classpath "$classpath" com.google.gwt.dev.codeserver.CodeServer $VERBOSE $DEBUG $PORT_OPTION $sources $entry_point;
