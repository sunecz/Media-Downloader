#!/bin/bash

# Creates a hash list that is used by Media Downloader.
#
# The list is a collection of lines and each line represents a single file
# in the following format:
# {HASH}|{OS_NAME};{OS_ARCH}|{VERSION}|{PATH}
#     HASH    ... currently only SHA1 hash
#     OS_NAME ... win (Windows), unx (Linux), mac (Mac OS X)
#     OS_ARCH ... 64 (-bit), 32 (-bit)
#     VERSION ... any version string, often in format like x.y.z
#     PATH    ... the remote relative path, relative to the remote root path
#
# Additional notes:
#     The part '{OS_NAME};{OS_ARCH}' may be '***;**' that specifies
#         that the file may be used on any operating system.
#     The part '{VERSION}' may be empty.

if [[ -z "$HASH_EXE" ]] ; then
	# By default use SHA1 since the application uses that as well
	HASH_EXE=sha1sum
fi

directory="$1"
os_name="$2"
os_arch="$3"
version="$4"
root_directory="$5"

usage() {
	NAME=$(basename "$0")
	echo "Usage: $NAME directory os_name os_arch version [root_directory]"
	echo "Arguments:"
	echo "    os_name        = win, unx, mac"
	echo "    os_arch        = 64, 32"
	echo "    version        = version of the files, often in format like x.y.z"
	echo "    root_directory = from which directory to get the relative path"
	return 0
}

if [[ -z "$directory" ]] || [[ -z "$os_name" ]] || [[ -z "$os_arch" ]] || [[ -z "$version" ]] ; then
	usage "$0"
	exit 255
fi

if [[ -z "$root_directory" ]] ; then
	root_directory="$(pwd)"
fi

find "$directory" -type f -print0 |
while IFS= read -r -d '' f; do
	hash=$("$HASH_EXE" "$f" | cut -d ' ' -f 1)
	relative_path=$(realpath --relative-to="$root_directory" "$f")
	printf '%s|%s;%s|%s|%s\n' "$hash" "$os_name" "$os_arch" "$version" "$relative_path"
done
