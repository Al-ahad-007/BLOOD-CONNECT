@echo off
title Build BloodConnect WAR
echo ========================================================
echo   Building BloodConnect WAR Package
echo ========================================================
echo.

mvn clean package -DskipTests

echo.
echo ========================================================
echo   Build Complete: target/BloodConnect.war
echo   You can deploy this WAR directly to Apache Tomcat 10.1
echo ========================================================
pause

