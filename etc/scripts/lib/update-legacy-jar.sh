#!/bin/bash

jar_path="$1"
module_name="$2"

usage() {
	NAME=$(basename "$0")
	echo "Usage: $NAME jar_path module_name"
	return 0
}

if [[ -z "$jar_path" ]] || [[ -z "$module_name" ]] ; then
	usage "$0"
	exit 255
fi

dir_jar=$(dirname "$jar_path")

echo Generating module-info.java...
jdeps --ignore-missing-deps --generate-module-info "$dir_jar" "$jar_path"
echo Patching the module...
javac --patch-module "$module_name=$jar_path" "$dir_jar/$module_name/module-info.java"
echo Updating the JAR file...
jar uf "$jar_path" -C "$dir_jar/$module_name" "module-info.class"
