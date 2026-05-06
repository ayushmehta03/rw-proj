@echo off
REM ─────────────────────────────────────────────────────────────────
REM  
REM  
REM ─────────────────────────────────────────────────────────────────
cd /d "%~dp0"

set SRC_DIR=src
set OUT_DIR=out

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"
echo Compiling...
dir /s /b "%SRC_DIR%\*.java" > sources.txt
javac -sourcepath "%SRC_DIR%" -d "%OUT_DIR%" @sources.txt
del sources.txt
echo Compiled OK — Launching...
java -cp "%OUT_DIR%" Main
pause
