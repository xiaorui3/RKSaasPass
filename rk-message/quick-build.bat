@echo off
cd /d "C:\Users\Administrator\IdeaProjects\RK-Web\rk-message"
set "MAVEN_CMD=C:\Users\Administrator\AppData\Local\Programs\INTELL~1\plugins\maven\lib\maven3\bin\mvn.cmd"
set "JAVA_HOME=C:\Users\Administrator\AppData\Local\Programs\INTELL~1\jbr"

echo Building rk-message with Maven...
echo MAVEN_CMD: %MAVEN_CMD%
echo JAVA_HOME: %JAVA_HOME%

%MAVEN_CMD% clean package -DskipTests 2>&1

echo Exit code: %ERRORLEVEL%
