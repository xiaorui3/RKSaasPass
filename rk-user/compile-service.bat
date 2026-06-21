@echo off
setlocal enabledelayedexpansion

REM Set JAVA_HOME to JetBrains Runtime (using short path to avoid spaces)
set "JAVA_HOME=C:\Users\Administrator\AppData\Local\Programs\INTELL~1\jbr"

REM Set Maven path (using short path to avoid spaces)
set "MAVEN_HOME=C:\Users\Administrator\AppData\Local\Programs\INTELL~1\plugins\maven\lib\maven3"
set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

REM Change to project root first to install dependencies
cd /d C:\Users\Administrator\IdeaProjects\RK-Web

echo ========================================
echo Building RK-Web project
echo ========================================
echo JAVA_HOME: %JAVA_HOME%
echo MAVEN_CMD: %MAVEN_CMD%
echo Working Directory: %CD%
echo ========================================
echo.

echo Step 1: Installing all modules to local repository...
%MAVEN_CMD% clean install -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo ERROR: Failed to install modules
    goto :error
)

echo.
echo Step 2: Building rk-user service...
cd rk-user
%MAVEN_CMD% clean package -DskipTests

REM Capture exit code
set EXIT_CODE=%ERRORLEVEL%

echo.
echo ========================================
echo Compilation completed with exit code: %EXIT_CODE%
echo ========================================

REM Output result to file
echo Compilation Exit Code: %EXIT_CODE% > compile-result.txt
echo Timestamp: %date% %time% >> compile-result.txt

if %EXIT_CODE% equ 0 (
    echo SUCCESS: rk-user service compiled successfully >> compile-result.txt
) else (
    echo FAILED: rk-user service compilation failed with code %EXIT_CODE% >> compile-result.txt
    goto :error
)

exit /b %EXIT_CODE%

:error
echo.
echo ========================================
echo ERROR OCCURRED
echo ========================================
exit /b 1
