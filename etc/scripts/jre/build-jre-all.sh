#!/bin/bash

jar_path="$1"
output_directory="$2"
oses=("windows-x64" "linux-x64" "osx-x64")

usage() {
	NAME=$(basename "$0")
	echo "Usage: $NAME jar_path output_directory"
	return 0
}

if [[ -z "$jar_path" ]] || [[ -z "$output_directory" ]] ; then
	usage "$0"
	exit 255
fi

if [[ -z "$SCRIPT" ]] ; then
	SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
	SCRIPT="$SCRIPT_DIR/build-jre.sh"
fi

for ((i = 0; i < ${#oses[@]}; i++)) ; do
	os_name="${oses[$i]}"
	"$SCRIPT" "$jar_path" "$os_name" "$output_directory"
done
