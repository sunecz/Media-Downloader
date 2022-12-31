#!/bin/bash

dir_path="$1"
new_path="$(echo $dir_path | sed 's/\/$//')_gzip/"

rm -rf "$new_path"

esc_dir_path=$(echo "$dir_path" | sed -e 's/[]\/$*.^[]/\\&/g')
esc_new_path=$(echo "$new_path" | sed -e 's/[\/&]/\\&/g')

find "$dir_path" -type f -print0 |
while IFS= read -r -d '' f; do
	new_file=$(echo "$f" | sed "s/^$esc_dir_path/$esc_new_path/")
	echo $new_file
	7z a "$new_file." "$f" -tgzip -mx=9 -mfb=256 -bso0 -bsp1
done
