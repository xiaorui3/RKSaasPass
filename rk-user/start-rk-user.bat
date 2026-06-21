@echo off
cd /d C:\Users\Administrator\IdeaProjects\RK-Web\rk-user
echo Starting rk-user service...
start /B java -jar target\rk-user.jar > logs\rk-user.log 2>&1
echo rk-user service started in background
