#!/bin/sh

# Included files:
#     - etc/jre/jre/$OS/*
#     - media-downloader.jar
#     - Media Downloader executable for $OS
# Optional files, not added by default:
#     - lib/*.jar
#     - etc/binary/original/* for $OS

DIR=$(pwd)
DIR_DIST="$DIR/dist"

INCLUDE_LIB=false
INCLUDE_BIN=false

# ----- Vars: Windows
OS_WIN_ARF="windows-x64"
OS_WIN_EXF="mdext"
OS_WIN_SFX="7zWin.sfx"
OS_WIN_JRE="windows-x64"
OS_WIN_BIN="win64"
OS_WIN_RUN="mdrun-windows.exe"
OS_WIN_RNM="Media Downloader.exe"
# -----

# ----- Vars: Linux
OS_LIN_ARF="linux-x64"
OS_LIN_EXF="mdext"
OS_LIN_SFX="7zUnx.sfx"
OS_LIN_JRE="linux-x64"
OS_LIN_BIN="unx64"
OS_LIN_RUN="mdrun-linux"
OS_LIN_RNM="Media Downloader"
# -----

# ----- Vars: Mac OS X
OS_OSX_ARF="osx-x64"
OS_OSX_EXF="mdext"
OS_OSX_SFX="7zOSX.sfx"
OS_OSX_JRE="osx-x64"
OS_OSX_BIN="mac64"
OS_OSX_RUN="mdrun-osx"
OS_OSX_RNM="Media Downloader"
# -----

# ----- Common vars
PATH_JRE="$DIR/etc/jre/jre"
PATH_BIN="$DIR/etc/binary/original"
PATH_RUN="$DIR/etc/run"
PATH_JAR="$DIR/build"
PATH_LIB="$DIR/lib"
PATH_RES="$DIR/resources"
PATH_SFX="$DIR/etc/dist"
JAR_NAME="media-downloader.jar"
LIB_EXCLUDE="sune-utils-load.jar"
# -----

# ----- Common vars for apps, without file extension
APP_VERSION_FFMPEG="5.0.1"
APP_VERSION_FFPROBE="5.0.1"
APP_VERSION_PSSUSPEND="1.07"
APP_NAME_FFMPEG="ffmpeg"
APP_NAME_FFPROBE="ffprobe"
APP_NAME_PSSUSPEND="pssuspend"
# -----

OS=$1
case "$OS" in
	"windows")
		NAM=$OS_WIN_ARF
		ARF=$DIR_DIST/$OS_WIN_ARF
		EXE=$DIR_DIST/$OS_WIN_EXF
		SFX=$PATH_SFX/$OS_WIN_SFX
		JRE=$OS_WIN_JRE
		BIN=$OS_WIN_BIN
		RUN=$OS_WIN_RUN
		RNM=$OS_WIN_RNM
		;;
	"linux")
		NAM=$OS_LIN_ARF
		ARF=$DIR_DIST/$OS_LIN_ARF
		EXE=$DIR_DIST/$OS_LIN_EXF
		SFX=$PATH_SFX/$OS_LIN_SFX
		JRE=$OS_LIN_JRE
		BIN=$OS_LIN_BIN
		RUN=$OS_LIN_RUN
		RNM=$OS_LIN_RNM
		;;
	"osx")
		NAM=$OS_OSX_ARF
		ARF=$DIR_DIST/$OS_OSX_ARF
		EXE=$DIR_DIST/$OS_OSX_EXF
		SFX=$PATH_SFX/$OS_OSX_SFX
		JRE=$OS_OSX_JRE
		BIN=$OS_OSX_BIN
		RUN=$OS_OSX_RUN
		RNM=$OS_OSX_RNM
		;;
	*)
		echo "Unknown OS: '$OS'."
		;;
esac

# ----- Function: fn_copydir src, dst
# Copy a directory at path "src" to path "dst"
fn_copydir() {
	echo "Copy directory:"
	echo "    From: '$1'"
	echo "    To:   '$2'"
	cp -a "$1/." "$2/"
}
# -----

# ----- Function: fn_copyfile src, dst
# Copy a file at path "src" to path "dst"
fn_copyfile() {
	echo "Copy file:"
	echo "    From: '$1'"
	echo "    To:   '$2'"
	cp "$1" "$2"
}
# -----

# ----- Function: fn_copyfiles src, dst, name
# Copy all files with name "name" from directory "src" to directory "dst"
fn_copyfiles() {
	echo "Copy files:"
	echo "    From: '$1'"
	echo "    To:   '$2'"
	echo "    Name: '$3'"
	cp "$1/$3" "$2"
}
# -----

# ----- Function: fn_delfile file
# Deletes a file
fn_delfile() {
	echo "Delete file: '$1'"
	rm -f "$1"
}
# -----

# ----- Function: fn_deldir dir
# Deletes a directory
fn_deldir() {
	echo "Delete directory: '$1'"
	rm -rf "$1"
}
# -----

# ----- Function: fn_mkdir dir
# Creates a directory
fn_mkdir() {
	echo "Create directory: '$1'"
	mkdir -p "$1"
}
# -----

OUT="$ARF"
OUT_EXE="$EXE"
OUT_DIR="$DIR_DIST/$NAM"
OUT_JRE="$OUT_DIR/jre"
OUT_LIB="$OUT_DIR/lib"
OUT_RES="$OUT_DIR/resources"
OUT_BIN="$OUT_RES/binary"

echo "Creating distribution archive file..."
echo "    OS:          '$OS'"
echo "    Output path: '$OUT_EXE'"
echo ""

echo "Deleting the old archive..."
if [ "$OS" = "windows" ] ; then
	fn_delfile "$OUT.zip"
else
	fn_delfile "$OUT.tar.gz"
fi

echo "Deleting the old extractor..."
fn_delfile "$OUT_EXE"

echo "Deleting the old directory..."
fn_deldir "$OUT_DIR"

echo "Creating the parent directory..."
fn_mkdir "$OUT_DIR"

echo "Adding custom JRE..."
fn_copydir "$PATH_JRE/$JRE" "$OUT_JRE"

if [ "$INCLUDE_BIN" = "true" ] ; then
	echo "Adding binary resources: FFMpeg..."
	fn_copydir "$PATH_BIN/$BIN/$APP_NAME_FFMPEG/$APP_VERSION_FFMPEG" "$OUT_BIN"

	echo "Adding binary resources: FFProbe..."
	fn_copydir "$PATH_BIN/$BIN/$APP_NAME_FFPROBE/$APP_VERSION_FFPROBE" "$OUT_BIN"

	if [ "$OS" = "windows" ] ; then
		echo "Adding binary resources: PsSuspend..."
		fn_copydir "$PATH_BIN/$BIN/$APP_NAME_PSSUSPEND/$APP_VERSION_PSSUSPEND" "$OUT_BIN"
	fi
fi

if [ "$INCLUDE_LIB" = "true" ] ; then
	echo "Adding libraries..."
	fn_copyfiles "$PATH_LIB" "$OUT_LIB" "*.jar"

	for f in $(echo "$LIB_EXCLUDE" | sed 's/,/ /g') ; do
		fn_delfile "$OUT_LIB/$f"
	done
fi

echo "Adding the JAR file..."
fn_copyfile "$PATH_JAR/$JAR_NAME" "$OUT_DIR/$JAR_NAME"

echo "Creating the archive..."
7z a -t7z -mx=9 -myx=9 -mfb=273 -md=1536m -ms -mmf=bt3 -mmc=10000 -mpb=0 -mlp=0 -mlc=0 -mtm=- -mmt -mmtf -r -bso0 -bsp1 "$OUT.7z" "$OUT_DIR/*"

echo "Creating the extractor..."
cat "$SFX" "$OUT.7z" > "$OUT_EXE"

echo "Adding the executable file..."
fn_copyfile "$PATH_RUN/$RUN" "$DIR_DIST/$RUN"
mv "$DIR_DIST/$RUN" "$DIR_DIST/$RNM"

if [ "$OS" = "windows" ] ; then
	7z a "$OUT.zip" "$OUT_EXE" "$DIR_DIST/$RNM" -r -tzip -bso0 -bsp1
else
	7z a "$OUT.tar" "$OUT_EXE" "$DIR_DIST/$RNM" -r -ttar -bso0 -bsp1
fi

if [ ! "$OS" = "windows" ] ; then
	echo "Compressing the archive..."
	7z a "$OUT.tar.gz" "$OUT.tar" -tgzip -mx=9 -mfb=256 -bso0 -bsp1
	echo "Deleting the uncompressed archive..."
	fn_delfile "$OUT.tar"
fi

echo "Deleting the extractor..."
fn_delfile "$OUT_EXE"

echo "Deleting the executable file..."
fn_delfile "$DIR_DIST/$RNM"

echo "Deleting the archive..."
fn_delfile "$OUT.7z"

echo "Deleting the temporary directory..."
fn_deldir "$OUT_DIR"

echo "Done!"
