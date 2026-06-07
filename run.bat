@echo off
if exist "%~dp0jdk\bin\java.exe" (
  set "JAVA_HOME=%~dp0jdk"
  set "PATH=%JAVA_HOME%\bin;%PATH%"
)
call mvnw.cmd spring-boot:run
