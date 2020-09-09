#!/bin/sh -e

dirs=`ls lib | grep -v '^lib:' | sort -u`;
#echo $dirs;

pwd=`pwd`
root=`basename $pwd`;
#echo $root;

get_jars() {
	libdir="$1";
	#non GWT validation usage
	#jars=`ls $libdir/*.jar | grep -v '\-src\-'`;
	#GWT requires validation-api-src
	jars=`ls $libdir/*.jar | awk '{if (!index($1, "-src-") || index($1, "validation-api")) {print $0;}}'`;
	echo $jars;
}

write_entries() {
	subdir="$1";
	libdir="$2";
	if [ -d $subdir/$libdir ]; then

		jars=`get_jars $subdir/$libdir`;
		#echo $jars
		for jar in $jars; do
			#echo $jar;
			jar_basename=`echo $jar | sed 's/-[rv]\?[0-9].*//g'`;
			#echo $jar_basename;
			src_attr="";
			jar_source="$jar_basename*-src-*.jar";
			if test -f "$jar_source"; then
				jar_source_basename=`echo $jar_source | sed "s,$subdir/$libdir/,,g"`;
				#echo $jar_source_basename;
				src_attr="source=\"/$root/$subdir/$libdir/$jar_source_basename\"";
			fi
			echo "		<archive path=\"/$root/$jar\" $src_attr/>";
		done;
	fi;
}

write_dist_xml_entries() {
	subdir="$1";
	libdir="$2";
	if [ -d $subdir/$libdir ]; then

		jars=`get_jars $subdir/$libdir`;
		#echo $jars
		for jar in $jars; do
			#echo $jar;
			jar_basename=`echo $jar | sed 's/-[0-9].*//g'`;
			#echo $jar_basename;
			echo "		<pathelement location=\"../$root/$jar\"/>";
		done;
	fi;
}

update_eclipse_libraries() {
	cat << EOF
<?xml version="1.0" encoding="ISO-8859-7" standalone="no"?>
<eclipse-userlibraries version="2">
EOF

	for dir in $dirs; do
		echo "	<library name=\"$dir\" systemlibrary=\"false\">";
		write_entries lib "$dir";
		echo "	</library>";
	done;

	cat << EOF
</eclipse-userlibraries>
EOF
}

update_ant_libraries() {
	cat << EOF
<?xml version="1.0" encoding="UTF-8"?>
<project>
EOF

	for dir in $dirs; do
		echo "	<path id=\"$dir.userclasspath\">";
		write_dist_xml_entries lib "$dir";
		echo "	</path>";
	done;

	cat << EOF
</project>
EOF
}


[ -n "$2" ] && exec > "$2";

case "$1" in
	eclipse_libraries)
		update_eclipse_libraries;
		;;
	ant_libraries)
		update_ant_libraries;
		;;
	*)
		echo "Usage: $0 {eclipse_libraries|ant_libraries} [outfile]"
		;;

esac;
