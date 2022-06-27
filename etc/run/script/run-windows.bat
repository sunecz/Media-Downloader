@echo off

set BIN=%cd%\jre\bin\javaw.exe
set JAR=%cd%\media-downloader.jar

start "" "%BIN%" -jar "%JAR%"
