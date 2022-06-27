@echo off
title Create distribution archive files (All)

call dist windows
call dist linux
call dist osx
echo All done.
