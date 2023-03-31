#!/bin/bash

# In the folder where this script is, it is required to have following directories:
# javafx      - contains subdirectories with JavaFX .jar files for specific OS, for more information see jdk-os-info.txt
# javafx-mods - contains subdirectories with JavaFX .jmod files for specific OS, for more information see jdk-os-info.txt
# jdk         - contains subdirectories with JDKs for specific OS, for more information see jdk-os-info.txt
# lib         - contains all .jar libraries that are used by the application
# Then call this script:
#     build-jre.bat [jar_name] [os_name]
# Where:
#     jar_name - the name of the JAR for which to create the JRE
#     os_name  - the name of subdirectory in jdk directory, this script then uses its jmods to create the JRE 

jar_path="$1"
os_name="$2"
output_directory="$3"

usage() {
	NAME=$(basename "$0")
	echo "Usage: $NAME jar_path os_name output_directory"
	echo "Arguments:"
	echo "    os_name = windows-x64, linux-x64, osx-x64"
	return 0
}

if [[ -z "$jar_path" ]] || [[ -z "$os_name" ]] || [[ -z "$output_directory" ]] ; then
	usage "$0"
	exit 255
fi

dir_jar=$(dirname "$jar_path")
path_lib="$dir_jar/lib"
path_java="$dir_jar/jdk/$os_name/jmods"
path_javafx="$dir_jar/javafx/$os_name"
path_javafx_jmods="$dir_jar/javafx-jmods/$os_name"
output="$output_directory/$os_name"

manual_modules="infomas.asl,org.jsoup,ssdf2,sune.memory,sune.api.process,sune.util.load"
manual_modules_java="java.net.http,java.management,java.sql,java.naming,java.compiler,java.instrument,jdk.jdi,jdk.sctp,jdk.localedata,jdk.accessibility,jdk.scripting.nashorn,jdk.crypto.ec"

echo Getting dependencies from the JAR...
jdeps=$(jdeps --module-path "$path_javafx:$path_lib" --add-modules="$manual_modules" --print-module-deps --ignore-missing-deps "$jar_path")
jdeps="$jdeps,$manual_modules_java"

echo Dependencies: "$jdeps"

echo Creating JRE...
jlink --module-path "$path_java:$path_javafx_jmods:$path_lib" --no-header-files --no-man-pages --compress=2 --strip-debug --add-modules="$jdeps" --output "$output"
