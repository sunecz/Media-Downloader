@echo off
title Compress executable for OS: FFMpeg/FFProbe

set FILE_FFMPEG=ffmpeg
set FILE_FFPROBE=ffprobe

:get_os
if "%1" == "" (
	set /p os=OS: 
) else (
	set os=%1
)

:get_version
if "%2" == "" (
	set /p version=Version: 
) else (
	set version=%2
)

:fix_file_names
if "%os%" == "win64" goto fix_file_names_exe
goto compress

:fix_file_names_exe
set FILE_FFMPEG=%FILE_FFMPEG%.exe
set FILE_FFPROBE=%FILE_FFPROBE%.exe

:compress
call compress.bat "%os%" "ffmpeg" "%FILE_FFMPEG%" "%version%"
call compress.bat "%os%" "ffprobe" "%FILE_FFPROBE%" "%version%"
