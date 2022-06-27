@echo off
title Compress all

:get_version_ff
if "%1" == "" (
	set /p version_ff=Version ^(FFMpeg/FFProbe^): 
) else (
	set version_ff=%1
)

:compress_ff_win64
call compress_ff.bat win64 "%version_ff%"
:compress_ff_unx64
call compress_ff.bat unx64 "%version_ff%"
:compress_ff_mac64
call compress_ff.bat mac64 "%version_ff%"

:get_version_ps
if "%2" == "" (
	set /p version_ps=Version ^(PsSuspend^): 
) else (
	set version_ps=%2
)

:compress_ps_win64
call compress_ps.bat win64 "%version_ps%"
