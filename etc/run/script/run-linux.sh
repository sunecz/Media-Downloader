#!/bin/sh

cwd=$(pwd)
bin="$cwd/jre/bin/java"
jar="$cwd/media-downloader.jar"

if ! [ -x "$bin" ]
then
	chmod +x "$bin"
fi

"$bin" -jar "$jar"
