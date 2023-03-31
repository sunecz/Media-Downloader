#!/bin/bash

version="$1"
dir_download="$2"
oses=("windows")
osmap=("win64")

usage() {
	NAME=$(basename "$0")
	echo "Usage: $NAME version [download_directory]"
	echo "Arguments:"
	echo "    version = 1.07, ..."
	return 0
}

if [[ -z "$version" ]] ; then
	usage "$0"
	exit 255
fi

if [[ -z "$dir_download" ]] ; then
	dir_download="$(pwd)/download"
fi

if [[ -z "$SCRIPT" ]] ; then
	SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
	SCRIPT="$SCRIPT_DIR/download-pssuspend.sh"
fi

for ((i = 0; i < ${#oses[@]}; i++)) ; do
	os="${oses[$i]}"
	name="${osmap[$i]}"
	dir="$dir_download/$name/$version"
	mkdir -p "$dir"
	
	output_file="$(pwd)/$(basename "$SCRIPT").out"
	rm "$output_file" 1>/dev/null 2>&1
	OUTPUT="$output_file" "$SCRIPT" "$dir"

	while IFS= read -r f; do
		f=$(printf '%s' "$f" | tr -d '\n\r')
		f_dirname=$(dirname "$f")
		f_version=$(basename "$f_dirname")
		f_basename=$(basename "$f")
		f_filename="${f_basename%.*}"
		f_root=$(dirname "$f_dirname")
		new_path="$f_root/$f_filename/$f_version"
		mkdir -p "$new_path"
		mv "$f" "$new_path/$f_basename"
	done < "$output_file"

	rm -rf "$dir"
	rm "$output_file"
done
