@echo off
title BloodConnect Web Application
echo ========================================================
echo   Starting BloodConnect Web Application
echo ========================================================
echo.
echo Application URL: http://localhost:8080/BloodConnect/
echo.
echo Press Ctrl+C to stop the server at any time.
echo.

mvn exec:java
pause

