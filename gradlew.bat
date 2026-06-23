@echo off
set "JAVA_HOME=%ProgramFiles%\Java\jdk1.8.0_202"
set "PATH=%JAVA_HOME%\bin;%PATH%"
java -version 2>&1
java -jar "%~dp0gradle\wrapper\gradle-wrapper.jar" %*
