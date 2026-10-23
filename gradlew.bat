@echo off
cd /d "%~dp0"
java -jar gradle\wrapper\gradle-wrapper.jar %*
exit /b %ERRORLEVEL%
