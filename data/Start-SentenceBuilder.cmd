@echo off
setlocal
cd /d "%~dp0.."
set "DB_PREFER_JDBC=false"
if exist "C:\Tools\apache-maven-3.10.0\bin\mvn.cmd" (
  call "C:\Tools\apache-maven-3.10.0\bin\mvn.cmd" clean javafx:run
) else (
  call mvn clean javafx:run
)
if errorlevel 1 (
  echo.
  echo The app could not start. Copy the error above for troubleshooting.
  pause
)
endlocal
