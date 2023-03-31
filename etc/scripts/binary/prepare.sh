#!/bin/bash

version_ffmpeg="$1"
version_pssuspend="$2"
dir_download="$3"

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

if [[ -z "$SCRIPT_FFMPEG" ]] ; then
	SCRIPT_FFMPEG="$(script_dir)/prepare-ffmpeg.sh"
fi

if [[ -z "$SCRIPT_PSSUSPEND" ]] ; then
	SCRIPT_PSSUSPEND="$(script_dir)/prepare-pssuspend.sh"
fi

"$SCRIPT_FFMPEG" "$version_ffmpeg" "$dir_download"
"$SCRIPT_PSSUSPEND" "$version_pssuspend" "$dir_download"
