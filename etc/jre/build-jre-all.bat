@echo off
title Build JRE (All)

set JAR_NAME=media-downloader-jre.jar
set JRE_WIN=windows-x64
set JRE_LIN=linux-x64
set JRE_OSX=osx-x64

call build-jre %JAR_NAME% %JRE_WIN%
call build-jre %JAR_NAME% %JRE_LIN%
call build-jre %JAR_NAME% %JRE_OSX%
echo All done.

pause > nul
exit
