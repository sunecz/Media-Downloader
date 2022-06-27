@echo off
title Compress

set DIR_ORIGINAL=original
set DIR_COMPRESSED=compressed

:get_os
if "%1" == "" (
	set /p os=OS: 
) else (
	set os=%1
)

:get_app_name
if "%2" == "" (
	set /p app_name=Application name: 
) else (
	set app_name=%2
)

:get_file
if "%3" == "" (
	set /p file=File: 
) else (
	set file=%3
)

:get_version
if "%4" == "" (
	set /p version=Version: 
) else (
	set version=%4
)

:do_work
md "%cd%\%DIR_COMPRESSED%\%os%\%app_name%\%version%" 2>nul
:compress
7z a "%cd%\%DIR_COMPRESSED%\%os%\%app_name%\%version%\%file%." -tgzip -mx9 -bso0 -bsp1 "%cd%\%DIR_ORIGINAL%\%os%\%app_name%\%version%\%file%"
