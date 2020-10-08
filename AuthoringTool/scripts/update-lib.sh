#!/bin/sh -e

get_srcpaths_s() {
	awk '/<classpathentry kind="src" path="/ {
		print $NF;
	}
	/<classpathentry.*excluding.* kind="src" path="/ {
		print $NF;
	}' < "$1" | sed -e 's/^.*path="//g' | sed -e "s,^,`dirname $1`/,g" -e 's/"\/>//g';
}

get_libraries_s() {
	awk '/<classpathentry kind="con" path="org.eclipse.jdt.USER_LIBRARY/ {
		print $NF;
	}' < "$1" | sed -e 's/^.*USER_LIBRARY\///g' | sed -e 's/"\/>//g';
	
	if grep -q com.google.gwt.eclipse.core.GWT_CONTAINER .classpath; then
		echo gwt;
	fi
}

get_projects_s() {
	awk '/<classpathentry combineaccessrules="false" kind="src" path="/ {
		print $NF;
	}' < "$1" | sed -e 's/^.*path="\///g' | sed -e 's/"\/>//g';
}

get_excluded_s() {
	awk '/<classpathentry excluding=/ {print $2;}' < "$1" | sed -e 's/excluding="//g' -e 's/"$/ /g' -e 's/|/ /g';
}

get_build_dir() {
	awk '/<classpathentry kind="output" path="/ {
		print $NF;
	}' < "$1" | sed -e 's/^.*path="//g' | sed -e 's/"\/>//g';
}

get_projects() {
	projects=`get_projects_s "$1"`;

	for project in $projects; do
		projects="$projects `get_projects ../$project/.classpath`";
	done;
	echo $projects | sed 's/ /\n/g' | sort -u;
}

get_srcpaths() {
	projects=`get_projects_s "$1"`;
	srcpaths=`get_srcpaths_s "$1"`;

	for project in $projects; do
		srcpaths="$srcpaths `get_srcpaths ../$project/.classpath`";
	done;
	echo $srcpaths | sed 's/ /\n/g' | sort -u;
}

get_libraries() {
	projects=`get_projects_s "$1"`;
	libraries=`get_libraries_s "$1"`;

	for project in $projects; do
		libraries="$libraries `get_libraries ../$project/.classpath`";
	done;
	echo $libraries | sed 's/ /\n/g' | sort -u;
}

#assumes correct directory
create_build_xml() {
	PROJECT_DIR=`pwd`;
	PROJECT=`basename $PROJECT_DIR`;
	[ "$PROJECT" = "biovista-lib" ] && return;
	projects=`get_projects .classpath`;
	libraries=`get_libraries .classpath`;
	excluded=`get_excluded_s .classpath`;
	build_dir=`get_build_dir .classpath`;
	srcpaths=`get_srcpaths .classpath`;
	src_basedirs=`get_srcpaths_s .classpath | awk '{printf("%s ", $1);}' | sed -s 's/ $//g'`;
	src_dir=`get_srcpaths_s .classpath | awk '{print $1; exit 0;}'`;

	cat << EOF
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!-- Automatically created by $0. Do not edit -->
<project basedir="." default="build" name="$PROJECT">
	<property name="src_dir" value="$src_dir"/>
	<property name="src_basedirs" value="$src_basedirs"/>
	<property name="build_dir" value="$build_dir"/>
EOF
	if [ -f dist.xml ]; then
		cat << EOF
	<import file="dist.xml" />
EOF
	fi

	cat << EOF
	<property name="excluded" value="$excluded \${excludes}"/>
	<import file="../biovista-lib/build-lib.xml" />
	<import file="../biovista-lib/build-common.xml" />

	<path id="$PROJECT.classpath">
		<pathelement location="\${build_dir}"/>
EOF

	for project in $projects; do
		echo "\t\t<pathelement location=\"../$project/build\"/>";
	done;

	for library in $libraries; do
		echo "\t\t<path refid=\"$library.userclasspath\"/>";
	done;

	cat << EOF
	</path>

EOF

	#srcpath - required by gwt
	cat << EOF
	<path id="$PROJECT.srcpath">
EOF
	for srcpath in $srcpaths; do
		gwtxmls=$(find $srcpath -type f -name "*.gwt.xml");
		if [ "x${gwtxmls}x" != "xx" ]; then
			echo "\t\t<pathelement location=\"$srcpath\"/>";
		fi
	done;
	cat << EOF
	</path>

EOF

	cat << EOF
	<target name="cleanall" description="cleans all dependendant projects" depends="clean">
EOF
	for project in $projects; do
		echo "\t\t<ant antfile=\"build.xml\" dir=\"../$project\" inheritAll=\"false\" target=\"cleanall\"/>";
	done;

	cat << EOF
	</target>

EOF

	cat << EOF
	<target name="build-subprojects">
EOF
	for project in $projects; do
		cat << EOF
	        <ant antfile="build.xml" dir="../$project" inheritAll="false" target="build">
			<propertyset>
				<propertyref name="build.compiler"/>
			</propertyset>
		</ant>
EOF
	done;

	cat << EOF
	</target> 

EOF

	cat << EOF
	<target name="build-project" depends="init">
		<echo message="\${ant.project.name}: \${ant.file}"/>
EOF
	for src_basedir in $src_basedirs; do
		cat << EOF
		<javac debug="true" debuglevel="\${debuglevel}" srcdir="${src_basedir}" destdir="\${build_dir}" excludes="\${excluded}" classpathref="$PROJECT.classpath" includeantruntime="false" source="\${source}" target="\${target}"/>
EOF
	done;
	cat << EOF
	</target>
</project>
EOF
}
