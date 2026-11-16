@echo off
rem Sustabdo LD2 API: ankstesnius 1-paleisti-api langus su visais ju procesais (Maven, API, DB),
rem tada viska, kas dar klauso prievadu 8080 ir 55432 (pvz. API, paleista ./mvnw is terminalo).
rem Argumentas "quiet" - be pause (naudoja 1-paleisti-api.cmd; jo paties langas neuzdaromas).
setlocal
set STOPPED=0
for /f "delims=" %%L in ('powershell -NoProfile -Command "$all = Get-CimInstance Win32_Process; $mine = @(); $x = $PID; while ($x -and $mine -notcontains $x) { $mine += $x; $x = ($all | Where-Object { $_.ProcessId -eq $x }).ParentProcessId }; $all | Where-Object { $_.Name -eq 'cmd.exe' -and $_.CommandLine -like '*1-paleisti-api.cmd*' -and $mine -notcontains $_.ProcessId } | ForEach-Object { taskkill /PID $_.ProcessId /T /F | Out-Null; 'Uzdarytas ankstesnis API langas, PID ' + $_.ProcessId }"') do (
  echo %%L
  set STOPPED=1
)
if "%STOPPED%"=="1" ping -n 3 127.0.0.1 >nul
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
