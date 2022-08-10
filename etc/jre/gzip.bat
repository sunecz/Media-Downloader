@echo off
title Compress files (gzip)

:get_dir_path
if "%~1" == "" (
	set /p dir_path=Directory path: 
) else (
	set "dir_path=%~1"
)

set "new_path=%dir_path%_gzip"
rd /s /q "%new_path%"

for /R "%dir_path%" %%f in (*) do (
	setlocal EnableDelayedExpansion

	set "fo=%%f"
	set "fi=!fo:%dir_path%=!"
	set "fi=!fi:~1!"
	echo !fi!

	set "fn=!fo:%dir_path%=%new_path%!"
	7z a "!fn!." "!fo!" -tgzip -mx=9 -mfb=256 -bso0

	endlocal
)

:eof
