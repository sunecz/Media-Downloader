#!/bin/bash

# OS = [ windows, linux, osx ]

version="$1"
os="$2"
dir_download="$3"

usage() {
	NAME=$(basename "$0")
	echo "Usage: $NAME version os [download_directory]"
	echo "Arguments:"
	echo "    version = 5.1.2, 6.0, ..."
	echo "    os      = windows, linux, osx"
	return 0
}

if [[ -z "$version" ]] || [[ -z "$os" ]] ; then
	usage "$0"
	exit 255
fi

if [[ -z "$dir_download" ]] ; then
	dir_download="$(pwd)/download"
fi

ext=""
if [[ "$os" == "windows" ]] ; then
	ext=".exe"
fi

urls=()
if [[ "$os" == "windows" ]] ; then
	# We can use GitHub releases to download a specific version.
	json=$(curl -s "https://api.github.com/repos/GyanD/codexffmpeg/releases")
	results=$(perl -ne 'print "$1\n" if /"browser_download_url": "([^"]+)"/s' <<< "$json")
	quoted_version=$(sed 's/\./\\./g' <<< "$version")
	url=$(grep -E -m 1 "/ffmpeg-$quoted_version-essentials_build.7z" <<< "$results")
	urls+=( $url )
elif [[ "$os" == "linux" ]] ; then
	# We can't use johnvansickle.com since it is no longer active.
	# Therefore we use the limited BtbN builds from GitHub.
	json=$(curl -s "https://api.github.com/repos/BtbN/FFmpeg-Builds/releases")
	results=$(perl -ne 'print "$1\n" if /"browser_download_url": "([^"]+)"/s' <<< "$json")
	quoted_version=$(sed 's/\./\\./g' <<< "$version")
	url=$(grep -E -m 1 "/ffmpeg-n$quoted_version-[^\-]+-[^\-]+-linux64-gpl-([0-9]+\.)+tar\.xz" <<< "$results")
	urls+=( $url )
elif [[ "$os" == "osx" ]] ; then
	# We can simply use direct links.
	urls+=(
		"https://evermeet.cx/pub/ffmpeg/ffmpeg-$version.7z"
		"https://evermeet.cx/pub/ffprobe/ffprobe-$version.7z"
	)
else
	echo "Invalid OS: '$os'."
	exit 255
fi

echo "Create directory: $dir_download"
mkdir -p "$dir_download"

paths=()
for url in ${urls[@]} ; do
	filename=$(basename "$url")
	extension="${filename##*.}"
	echo "Download: $url"
	curl -sL "$url" -O "$filename"

	if [[ "$extension" == "7z" ]] ; then
		echo "Extract: $filename"
		7z x "$filename" -o"$dir_download" -bso0 -bsp0 -y

		if [[ "$os" == "windows" ]] ; then
			echo "Flatten: $filename"
			dirname_extract="${filename%.*}"
			dir_extract="$dir_download/$dirname_extract"
			mv "$dir_extract/bin/ffmpeg$ext" "$dir_download"
			mv "$dir_extract/bin/ffprobe$ext" "$dir_download"
			rm -rf "$dir_extract"
		fi
	elif [[ "$extension" == "xz" ]] ; then
		echo "Extract: $filename"
		tar xf "$filename" -C "$dir_download"

		echo "Flatten: $filename"
		dirname_extract="$filename"
		dirname_extract="${dirname_extract%.*}"
		dirname_extract="${dirname_extract%.*}"
		dir_extract="$dir_download/$dirname_extract"
		mv "$dir_extract/bin/ffmpeg$ext" "$dir_download"
		mv "$dir_extract/bin/ffprobe$ext" "$dir_download"
		rm -rf "$dir_extract"
	fi

	paths+=(
		"$dir_download/ffmpeg$ext"
		"$dir_download/ffprobe$ext"
	)

	echo "Remove: $filename"
	rm "$filename"
done

# Print final files paths to stdout
for ((i = 0; i < ${#paths[@]}; i++)) ; do
    printf '%s\n' "${paths[$i]}"
done
