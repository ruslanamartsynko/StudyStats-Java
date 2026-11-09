@echo off
chcp 65001 >nul
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
  echo Java JDK not found. Install JDK 17 or later, then reopen this file.
  pause
  exit /b 1
)
if not exist out mkdir out
javac --release 17 -encoding UTF-8 -d out src\main\java\ua\studystats\*.java
if errorlevel 1 (
  echo Compilation failed.
  pause
  exit /b 1
)
java -Dfile.encoding=UTF-8 -cp out ua.studystats.Main
pause
