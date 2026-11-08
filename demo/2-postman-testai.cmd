@echo off
rem Paleidzia visa Postman kolekcija per newman: 78 uzklausos, 176 patikros.
title LD2 Postman testai
chcp 65001 >nul
cd /d "%~dp0.."
powershell -NoProfile -Command "try{$null=Invoke-WebRequest 'http://localhost:8080/v3/api-docs' -UseBasicParsing -TimeoutSec 3; exit 0}catch{exit 1}"
if errorlevel 1 (
  echo API neveikia. Pirma paleisk 1-paleisti-api ir palauk, kol atsidarys Swagger.
  pause
  exit /b 1
)
where newman >nul 2>&1
if errorlevel 1 (set "NEWMAN=npx --yes newman") else (set "NEWMAN=newman")
call %NEWMAN% run postman\DoneNDusted.postman_collection.json
pause
