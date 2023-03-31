#!/bin/bash

dir_path="$1"
new_path="$2"

usage() {
	NAME=$(basename "$0")
	echo "Usage: $NAME directory [output_directory]"
	return 0
}

if [[ -z "$dir_path" ]] ; then
	usage "$0"
	exit 255
fi

if [[ -z "$new_path" ]] ; then
	new_path="$(printf '%s' "$dir_path" | sed 's/\/$//')_gzip/"
fi

rm -rf "$new_path"

esc_dir_path=$(printf '%s' "$dir_path" | sed -e 's/[]\/$*.^[]/\\&/g')
esc_new_path=$(printf '%s' "$new_path" | sed -e 's/[\/&]/\\&/g')

find "$dir_path" -type f -print0 |
while IFS= read -r -d '' f; do
	new_file=$(printf '%s' "$f" | sed "s/^$esc_dir_path/$esc_new_path/")
	printf '%s\n' "$new_file"
	7z a "$new_file." "$f" -tgzip -mx=9 -mfb=256 -bso0 -bsp1
done
