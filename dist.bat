@echo off
setlocal
title Create distribution archive files

REM Included files:
REM     - etc\jre\jre\%OS%\*
REM     - media-downloader.jar
REM     - Media Downloader executable for %OS%
REM     - lib\*.jar
REM     - resources\binary\* for %OS%
REM     - resources\jre_version

:init
set DIR=%cd%
set DIR_DIST=%DIR%\dist

REM ----- Vars: Windows
set OS_WIN_ARF=windows-x64
set OS_WIN_JRE=windows-x64
set OS_WIN_BIN=win64
set OS_WIN_RUN=mdrun-windows.exe
set OS_WIN_RNM=Media Downloader.exe
REM -----

REM ----- Vars: Linux
set OS_LIN_ARF=linux-x64
set OS_LIN_JRE=linux-x64
set OS_LIN_BIN=unx64
set OS_LIN_RUN=mdrun-linux
set OS_LIN_RNM=Media Downloader
REM -----

REM ----- Vars: Mac OS X
set OS_OSX_ARF=osx-x64
set OS_OSX_JRE=osx-x64
set OS_OSX_BIN=mac64
set OS_OSX_RUN=mdrun-osx
set OS_OSX_RNM=Media Downloader
REM -----

REM ----- Common vars
set PATH_JRE=%DIR%\etc\jre\jre
set PATH_BIN=%DIR%\etc\binary\original
set PATH_RUN=%DIR%\etc\run
set PATH_JAR=%DIR%\jar
set PATH_LIB=%DIR%\lib
set PATH_RES=%DIR%\resources
set JAR_NAME=media-downloader.jar
set LIB_EXCLUDE=sune-utils-load.jar
REM -----

REM ----- Common vars for apps, without file extension
set APP_VERSION_FFMPEG=4.4.1
set APP_VERSION_FFPROBE=4.4.1
set APP_VERSION_PSSUSPEND=1.07
set APP_NAME_FFMPEG=ffmpeg
set APP_NAME_FFPROBE=ffprobe
set APP_NAME_PSSUSPEND=pssuspend
REM -----

:init_vars
set OS=%1
if "%OS%" == "windows" (
	set NAM=%OS_WIN_ARF%
	set ARF=%DIR_DIST%\%OS_WIN_ARF%.zip
	set JRE=%OS_WIN_JRE%
	set BIN=%OS_WIN_BIN%
	set RUN=%OS_WIN_RUN%
	set RNM=%OS_WIN_RNM%
) else (
	if "%OS%" == "linux" (
		set NAM=%OS_LIN_ARF%
		set ARF=%DIR_DIST%\%OS_LIN_ARF%.tar
		set JRE=%OS_LIN_JRE%
		set BIN=%OS_LIN_BIN%
		set RUN=%OS_LIN_RUN%
		set RNM=%OS_LIN_RNM%
	) else (
		if "%OS%" == "osx" (
			set NAM=%OS_OSX_ARF%
			set ARF=%DIR_DIST%\%OS_OSX_ARF%.tar
			set JRE=%OS_OSX_JRE%
			set BIN=%OS_OSX_BIN%
			set RUN=%OS_OSX_RUN%
			set RNM=%OS_OSX_RNM%
		) else (
			echo Unknown OS: "%OS%".
			pause > nul
			goto end
		)
	)
)

REM ----- Skip functions
goto create

REM ----- Function: fn_copydir src, dst
REM Copy a directory at path "src" to path "dst"
:fn_copydir
echo Copy directory:
echo     From: "%~1"
echo     To:   "%~2"
robocopy "%~1" "%~2" /E /COPY:DAT /DCOPY:DAT /MT /W:0 /NOOFFLOAD /NS /NC /NP /NFL /NDL /NJH /NJS >nul 2>&1
REM Supresses errors 1, 2, and 4; not real errors at robocopy
set /A ERRLVL="%ERRORLEVEL% & 24"
exit /B %ERRLVL%
REM -----

REM ----- Function: fn_copyfile src, dst
REM Copy a file at path "src" to path "dst"
:fn_copyfile
echo Copy file:
echo     From: "%~1"
echo     To:   "%~2"
REM Get only the file name and parent directories to be used in the robocopy call
for %%i in ("%~1") do set file_name=%%~ni%%~xi
for %%i in ("%~1") do set dir_src=%%~di%%~pi
for %%i in ("%~2") do set dir_dst=%%~di%%~pi
REM Remove trailing slash, if it exists in the directory paths
if %dir_src:~-1% == \ set dir_src=%dir_src:~0,-1%
if %dir_dst:~-1% == \ set dir_dst=%dir_dst:~0,-1%
robocopy "%dir_src%" "%dir_dst%" "%file_name%" /COPY:DAT /MT /W:0 /NOOFFLOAD /NS /NC /NP /NFL /NDL /NJH /NJS >nul 2>&1
REM Supresses errors 1, 2, and 4; not real errors at robocopy
set /A ERRLVL="%ERRORLEVEL% & 24"
exit /B %ERRLVL%
REM -----

REM ----- Function: fn_copyfiles src, dst, name
REM Copy all files with name "name" from directory "src" to directory "dst"
:fn_copyfiles
echo Copy files:
echo     From: "%~1"
echo     To:   "%~2"
echo     Name: "%~3"
robocopy "%~1" "%~2" "%~3" /COPY:DAT /MT /W:0 /NOOFFLOAD /NS /NC /NP /NFL /NDL /NJH /NJS >nul 2>&1
REM Supresses errors 1, 2, and 4; not real errors at robocopy
set /A ERRLVL="%ERRORLEVEL% & 24"
exit /B %ERRLVL%
REM -----

REM ----- Function: fn_delfile file
REM Deletes a file
:fn_delfile
echo Delete file: "%~1"
if exist "%~1" del /S /Q "%~1" >nul 2>&1
exit /B %ERRORLEVEL%
REM -----

REM ----- Function: fn_deldir dir
REM Deletes a directory
:fn_deldir
echo Delete directory: "%~1"
if exist "%~1" rd /S /Q "%~1" >nul 2>&1
exit /B %ERRORLEVEL%
REM -----

REM ----- Function: fn_mkdir dir
REM Creates a directory
:fn_mkdir
echo Create directory: "%~1"
if not exist "%~1" mkdir "%~1" >nul 2>&1
exit /B %ERRORLEVEL%
REM -----

:create
set OUT=%ARF%
set OUT_DIR=%DIR_DIST%\%NAM%
set OUT_JRE=%OUT_DIR%\jre
set OUT_LIB=%OUT_DIR%\lib
set OUT_RES=%OUT_DIR%\resources
set OUT_BIN=%OUT_RES%\binary

echo Creating distribution archive file...
echo     OS:          "%OS%"
echo     Output path: "%OUT%"
echo.

:delete_old
echo Deleting the old archive...
call :fn_delfile "%OUT%"

echo Deleting the old directory...
call :fn_deldir "%OUT_DIR%"

:parent_dir
echo Creating the parent directory...
call :fn_mkdir "%OUT_DIR%"

:jre
echo Adding custom JRE...
call :fn_copydir "%PATH_JRE%\%JRE%", "%OUT_JRE%"

:resources
echo Adding resources...
call :fn_copyfile "%PATH_RES%\jre_version" "%OUT_RES%\jre_version"

:resources_binary
echo Adding binary resources: FFMpeg...
call :fn_copydir "%PATH_BIN%\%BIN%\%APP_NAME_FFMPEG%\%APP_VERSION_FFMPEG%" "%OUT_BIN%"

echo Adding binary resources: FFProbe...
call :fn_copydir "%PATH_BIN%\%BIN%\%APP_NAME_FFPROBE%\%APP_VERSION_FFPROBE%" "%OUT_BIN%"

if "%OS%" == "windows" (
	echo Adding binary resources: PsSuspend...
	call :fn_copydir "%PATH_BIN%\%BIN%\%APP_NAME_PSSUSPEND%\%APP_VERSION_PSSUSPEND%" "%OUT_BIN%"
)

:libraries
echo Adding libraries...
call :fn_copyfiles "%PATH_LIB%" "%OUT_LIB%" "*.jar"

for %%f in (%LIB_EXCLUDE%) do (
	call :fn_delfile "%OUT_LIB%\%%f"
)

:jar
echo Adding the JAR file...
call :fn_copyfile "%PATH_JAR%\%JAR_NAME%" "%OUT_DIR%\%JAR_NAME%"

:exe
echo Adding the executable file...
call :fn_copyfile "%PATH_RUN%\%RUN%" "%OUT_DIR%\%RUN%"
ren "%OUT_DIR%\%RUN%" "%RNM%"

:archive
echo Creating the archive...
if "%OS%" == "windows" (
	7z a "%OUT%" "%OUT_DIR%\*" -r -mm=Deflate -mx=9 -mfb=256 -bso0
) else (
	7z a "%OUT%" "%OUT_DIR%\*" -r -ttar -bso0
)

:compress
if NOT "%OS%" == "windows" (
	echo Compressing the archive...
	7z a "%OUT%.gz" "%OUT%" -tgzip -mx=9 -mfb=256 -bso0
	echo Deleting the uncompressed archive...
	call :fn_delfile "%OUT%"
)

:cleanup
echo Deleting the temporary directory...
call :fn_deldir "%OUT_DIR%"

echo Done!

:end
