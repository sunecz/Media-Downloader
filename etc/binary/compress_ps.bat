@echo off
title Compress executable for OS: PsSuspend

set FILE_PSSUSPEND=pssuspend

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
set FILE_PSSUSPEND=%FILE_PSSUSPEND%.exe

:compress
call compress.bat "%os%" "pssuspend" "%FILE_PSSUSPEND%" "%version%"
