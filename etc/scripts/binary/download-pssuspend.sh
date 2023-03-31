#!/bin/bash

# Always downloads the latest available version.

dir_download="$1"

if [[ -z "$dir_download" ]] ; then
	dir_download="$(pwd)/download"
fi

url="https://download.sysinternals.com/files/PSTools.zip"
filename=$(basename "$url")

echo "Create directory: $dir_download"
mkdir -p "$dir_download"

echo "Download: $url"
curl -sL "$url" -O "$filename"

echo "Extract: $filename"
dirname_extract="${filename%.*}"
dir_extract="$dir_download/$dirname_extract"
7z x "$filename" -o"$dir_extract" -bso0 -bsp0 -y

echo "Flatten: $filename"
mv "$dir_extract/pssuspend64.exe" "$dir_download/pssuspend.exe"
rm -rf "$dir_extract"

echo "Remove: $filename"
rm "$filename"

path="$dir_download/pssuspend.exe"

if [[ -z "$OUTPUT" ]] ; then
	# Print final files paths to stdout
	printf '%s\n' "$path"
else
	printf '%s\n' "$path" >> "$OUTPUT"
fi
