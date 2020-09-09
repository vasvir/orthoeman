#!/bin/sh -e

[ -n "$1" -a -f "$1" ] || echo "Syntax: $0 {config/biovista-lib.ini}" 1>&2;

CONFIG="$1";

awk -vFS='\t' 'BEGIN {
	ivy="ivy.xml";
	ivysettings="ivysettings.xml";
	ivybuild="ivybuild.xml";
	RepositorySection = 1;
	PackageSection = 2;
	#print "start";
	
	#format: dir name org repo exclude rev
	destdir_idx = 1;
	name_idx = 2;
	package_idx = 3;
	repository_idx = 4;
	excludes_idx = 5
	revision_idx = 6;

	lines = 0;
}

/^#/ {
	#print "Comment:", $0;
	next;
}

/^\s*$/ {
	next;
}

/\[Repositories\]/ {
	section = RepositorySection;
	next;
}

/\[Packages\]/ {
	section = PackageSection;
	next;
}

section == RepositorySection {
	#print "Repository:", $0;
	repos[$1] = $2;
}

section == PackageSection {
	#print "Package:", $0;

	destdir = $1;
	name = $2;
	package = $3;
	repository = $4 ? $4 : "central";
	excludes = $5;
	revision = $6 ? $6 : "latest.integration";

	name2dir[name] = destdir;
	name2package[name] = package;
	if (name2repo[name] && name2repo[name] != repository) {
		printf("jar %s was already declared in %s not in %s\n", name, name2repo[name], repository) > "/dev/stderr";
		exit(1);
	}
	name2repo[name] = repository;
	name2excludelist[name] = excludes;
	name2rev[name]= revision;

	jar[lines][destdir_idx] = destdir;
	jar[lines][name_idx] = name;
	jar[lines][package_idx] = package
	jar[lines][repository_idx] = repository;
	jar[lines][excludes_idx] = excludes;
	jar[lines][revision_idx] = revision;

	if (!(repository in repos)) {
		printf("Undeclared repository %s\n", repository) > "/dev/stderr";
		exit(1);
	}

	destdirs[destdir] = 1;
	names[name] = 1;
	repositories[repository] = 1;

	lines++;
}

END {
	#print "writing ", ivysettings;

	print "\
<ivysettings>\n\
	<settings defaultResolver=\"central\"/>\n\
	<resolvers>\
" > ivysettings;

	for (name in repos) {
		if (repos[name]) {
			printf("\t\t<ibiblio name=\"%s\" m2compatible=\"true\" root=\"%s\"/>\n", name, repos[name]) > ivysettings;
		} else {
			printf("\t\t<ibiblio name=\"%s\" m2compatible=\"true\"/>\n", name) > ivysettings;
		}
	}
	print "\
	</resolvers>\n\
	<modules>\
" > ivysettings;

	for (name in name2repo) {
		printf("\t\t<module organisation=\"%s\" name=\"%s\" resolver=\"%s\"/>\n", name2package[name], name, name2repo[name]) > ivysettings;
	}

	print "\
	</modules>\n\
</ivysettings>\
" > ivysettings;

	print "\
<ivy-module version=\"2.0\">\n\
	<info organisation=\"com.biovista\" module=\"biovista-lib\"/>\n\
	<configurations>\
" > ivy;

	for (dir in destdirs) {
		printf("\t\t<conf name=\"%s-src\"  description=\"%s source jars\"/>\n", dir, dir) > ivy;
		printf("\t\t<conf name=\"%s-bin\" description=\"%s binary jars\"/>\n", dir, dir) > ivy;
	}

	print "\
	</configurations>\n\
	<dependencies>\
" > ivy;

	for (line = 0; line < lines; line++) {
		printf("\t\t<dependency org=\"%s\" name=\"%s\" rev=\"%s\" conf=\"%s-bin->default\">\n", jar[line][package_idx], jar[line][name_idx], jar[line][revision_idx], jar[line][destdir_idx]) > ivy;
		if (jar[line][excludes_idx]) {
			split(jar[line][excludes_idx], list, ",");
			for (i in list) {
				gsub(/[[:space:]]*/,"",list[i]);
				split(list[i], type_value, ":");
				if (length(type_value) == 1) {
					type = "name";
					value = type_value[1];
				} else {
					type = type_value[1];
					value = type_value[2];
				}
				printf("\t\t\t<exclude %s=\"%s\"/>\n", type, value) > ivy;
			}
		}
		printf("\t\t</dependency>\n") > ivy;
		printf("\t\t<dependency org=\"%s\" name=\"%s\" rev=\"%s\" conf=\"%s-src->sources\">\n", jar[line][package_idx], jar[line][name_idx], jar[line][revision_idx], jar[line][destdir_idx]) > ivy;
		printf("\t\t</dependency>\n") > ivy;
	}
	printf("\t\t<override module=\"txw2\" rev=\"2.4.0-b180830.0438\"/>\n") > ivy;
	printf("\t\t<override module=\"commonj.sdo\" rev=\"2.1.1\"/>\n") > ivy;

	print "\
	</dependencies>\n\
</ivy-module>\
" > ivy;

	print "\
<project name=\"ivybuild\" xmlns:ivy=\"antlib:org.apache.ivy.ant\">\n\
	<target name=\"resolve\">\
" > ivybuild;
	for (dir in destdirs) {
		printf("\t\t<ivy:retrieve conf=\"%s-src\" pattern=\"lib/%s/[artifact]-src-[revision].[ext]\"/>\n", dir, dir) > ivybuild;
		printf("\t\t<ivy:retrieve conf=\"%s-bin\" pattern=\"lib/%s/[artifact]-[revision].[ext]\"/>\n", dir, dir) > ivybuild;
	}

	print "\
	</target>\n\
</project>\
" > ivybuild;


}
' < "$CONFIG"; 
