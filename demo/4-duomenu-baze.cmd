@echo off
rem Parodo demonstracijos DB (prievadas 55432): BCrypt maisos, narystes, refresh zetonu maisos.
rem Po to lieka atviras psql - galima rasyti savo SQL.
title LD2 duomenu baze (psql)
cd /d "%~dp0.."
set "PSQL=psql"
where psql >nul 2>&1
if errorlevel 1 for /d %%D in ("%ProgramFiles%\PostgreSQL\*") do if exist "%%D\bin\psql.exe" set "PSQL=%%D\bin\psql.exe"
chcp 65001 >nul
set PGCLIENTENCODING=UTF8
"%PSQL%" -h localhost -p 55432 -U postgres -d postgres -P pager=off -f demo\db-demo.sql
if errorlevel 1 (
  echo.
  echo Nepavyko prisijungti prie DB. DB pasileidzia kartu su API - pirma 1-paleisti-api.
  pause
  exit /b 1
)
echo.
echo ---- Galima rasyti savo SQL, pvz.: SELECT * FROM log_entry;   Iseiti: \q
"%PSQL%" -h localhost -p 55432 -U postgres -d postgres
