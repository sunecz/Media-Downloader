@echo off
title Build JRE

REM In the folder where this script is, it is required to have following directories:
REM javafx      - contains subdirectories with JavaFX .jar files for specific OS, for more information see jdk-os-info.txt
REM javafx-mods - contains subdirectories with JavaFX .jmod files for specific OS, for more information see jdk-os-info.txt
REM jdk         - contains subdirectories with JDKs for specific OS, for more information see jdk-os-info.txt
REM lib         - contains all .jar libraries that are used by the application
REM Then call this script:
REM     build-jre.bat [jar_name] [os_name]
REM Where:
REM     jar_name - the name of the JAR for which to create the JRE
REM     os_name  - the name of subdirectory in jdk directory, this script then uses its jmods to create the JRE 

:get_jar_name
if "%1" == "" (
	set /p jar_name=JAR name: 
) else (
	set jar_name=%1
)

:get_os_name
if "%2" == "" (
	set /p os_name=OS name: 
) else (
	set os_name=%2
)

set PATH_LIB=%cd%\lib
set PATH_JAVA=%cd%\jdk\%os_name%\jmods
set PATH_JAVAFX=%cd%\javafx\%os_name%
set PATH_JAVAFX_MODS=%cd%\javafx-jmods\%os_name%
set OUTPUT=jre\%os_name%
set MANUAL_MODULES=infomas.asl,org.jsoup,ssdf2,sune.memory,sune.api.process,sune.util.load

echo Getting dependencies from the JAR...
jdeps --module-path "%PATH_JAVAFX%;%PATH_LIB%" --add-modules="%MANUAL_MODULES%" --print-module-deps --ignore-missing-deps "%jar_name%" > deps.txt
set /p JDEPS=<deps.txt

set JDEPS=%JDEPS%,java.net.http,java.management,java.sql,java.naming,java.compiler,java.instrument,jdk.jdi,jdk.sctp,jdk.localedata,jdk.accessibility,jdk.scripting.nashorn

echo Done. Dependencies: "%JDEPS%"
del deps.txt

echo Creating JRE...
jlink --module-path "%PATH_JAVA%";"%PATH_JAVAFX_MODS%";"%PATH_LIB%" --no-header-files --no-man-pages --compress=2 --strip-debug --add-modules="%JDEPS%" --add-modules jdk.crypto.ec --output "%OUTPUT%"
echo Done. JRE location: %OUTPUT%
