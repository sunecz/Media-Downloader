#!/bin/bash

version_ffmpeg="$1"
version_pssuspend="$2"
dir_download="$3"

oses_ffmpeg=("win" "unx" "mac")
oses_pssuspend=("win")
os_arch="64"

usage() {
	NAME=$(basename "$0")
	echo "Usage: $NAME version_ffmpeg version_pssuspend [download_directory]"
	echo "Arguments:"
	echo "    version_ffmpeg = 5.1.2, 6.0, ..."
	echo "    version_pssuspend = 1.07, ..."
	return 0
}

if [[ -z "$version_ffmpeg" ]] || [[ -z "$version_pssuspend" ]] ; then
	usage "$0"
	exit 255
fi

if [[ -z "$dir_download" ]] ; then
	dir_download="$(pwd)/download"
fi

script_dir() {
	SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
	printf '%s' "$SCRIPT_DIR"
	return 0
}

if [[ -z "$SCRIPT_PREPARE_FFMPEG" ]] ; then
	SCRIPT_PREPARE_FFMPEG="$(script_dir)/prepare-ffmpeg.sh"
fi

if [[ -z "$SCRIPT_PREPARE_PSSUSPEND" ]] ; then
	SCRIPT_PREPARE_PSSUSPEND="$(script_dir)/prepare-pssuspend.sh"
fi

if [[ -z "$SCRIPT_LIST" ]] ; then
	SCRIPT_LIST="$(dirname "$(script_dir)")/list.sh"
fi

if [[ -z "$SCRIPT_COMPRESS" ]] ; then
	SCRIPT_COMPRESS="$(dirname "$(script_dir)")/gzip.sh"
fi

dir_original="$dir_download/original"
dir_compressed="$dir_download/compressed"
path_list="$dir_download/list"
dir_ffmpeg="$dir_original/ffmpeg"
dir_pssuspend="$dir_original/pssuspend"

"$SCRIPT_PREPARE_FFMPEG" "$version_ffmpeg" "$dir_ffmpeg"
"$SCRIPT_PREPARE_PSSUSPEND" "$version_pssuspend" "$dir_pssuspend"

for ((i = 0; i < ${#oses_ffmpeg[@]}; i++)) ; do
	os_name="${oses_ffmpeg[$i]}"
	dir_name="$os_name$os_arch"
	"$SCRIPT_LIST" "$dir_ffmpeg/$dir_name" "$os_name" "$os_arch" "$version_ffmpeg" "$dir_ffmpeg" >> "$path_list"
done

for ((i = 0; i < ${#oses_pssuspend[@]}; i++)) ; do
	os_name="${oses_pssuspend[$i]}"
	dir_name="$os_name$os_arch"
	"$SCRIPT_LIST" "$dir_pssuspend/$dir_name" "$os_name" "$os_arch" "$version_pssuspend" "$dir_pssuspend" >> "$path_list"
done

dir_merge="$dir_download/merge"
mkdir -p "$dir_merge"

cp -arlP "$dir_ffmpeg/." "$dir_merge"
cp -arlP "$dir_pssuspend/." "$dir_merge"
rm -r "$dir_original"
mv "$dir_merge" "$dir_original"

"$SCRIPT_COMPRESS" "$dir_original" "$dir_compressed"
