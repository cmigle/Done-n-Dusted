@echo off
rem Sustabdo LD2 API (prievadas 8080) ir jos laikina PostgreSQL (55432), jei liko veikti.
rem Argumentas "quiet" - be pause (naudoja 1-paleisti-api.cmd).
setlocal
set STOPPED=0
for %%P in (8080 55432) do (
  for /f "tokens=5" %%I in ('netstat -ano ^| findstr /C:":%%P " ^| findstr LISTENING') do (
    taskkill /PID %%I /T /F >nul 2>&1 && (
      echo Sustabdyta: PID %%I, prievadas %%P
      set STOPPED=1
    )
  )
)
if "%STOPPED%"=="0" echo Prievadai 8080 ir 55432 laisvi - nieko stabdyti nereikejo.
if /i not "%~1"=="quiet" pause
