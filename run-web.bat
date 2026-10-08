@echo off
chcp 65001 >nul
echo ============================================
echo  公益寻亲平台 - 前端 (Vite :5173)
echo ============================================
cd /d %~dp0web
if not exist node_modules (
  echo 首次运行,正在安装依赖...
  call npm install
)
call npm run dev
pause
