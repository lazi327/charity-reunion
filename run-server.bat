@echo off
chcp 65001 >nul
echo ============================================
echo  公益寻亲平台 - 后端 (Spring Boot :8081)
echo ============================================
cd /d %~dp0server
mvn spring-boot:run
pause
