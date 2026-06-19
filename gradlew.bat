@echo off
set DIR=%~dp0
if not exist "%DIR%gradle\wrapper\gradle-wrapper.jar" (
  echo gradle-wrapper.jar not found. Run 'gradle wrapper' locally or add the jar to %DIR%gradle\wrapper\
  exit /b 1
)
java -jar "%DIR%gradle\wrapper\gradle-wrapper.jar" %*
