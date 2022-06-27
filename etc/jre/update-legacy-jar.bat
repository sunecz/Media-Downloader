@echo off
title Update Legacy JAR

:start
set /p JAR_PATH=JAR Path: 
set /p MODULE_NAME=Module name: 

:update
echo Generating module-info.java...
jdeps --generate-module-info . %JAR_PATH%
echo Patching the module...
javac --patch-module %MODULE_NAME%=%JAR_PATH% %MODULE_NAME%/module-info.java
echo Updating the JAR file...
jar uf %JAR_PATH% -C %MODULE_NAME% module-info.class

echo.
echo JAR has been updated!

:exit
pause > nul
exit
