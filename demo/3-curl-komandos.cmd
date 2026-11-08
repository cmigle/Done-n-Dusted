@echo off
rem Atidaro docs\visual\DEMO_KOMANDOS.txt ir Git Bash, kuriame jau prisijungta:
rem $A - rokas, $J - jonas, $ADM - admin. Komandas galima kopijuoti tiesiai.
cd /d "%~dp0.."
start "" notepad "docs\visual\DEMO_KOMANDOS.txt"
set "GITDIR=%ProgramFiles%\Git"
if not exist "%GITDIR%\usr\bin\mintty.exe" (
  echo Nerastas Git Bash: %GITDIR%
  pause
  exit /b 1
)
set MSYSTEM=MINGW64
start "" "%GITDIR%\usr\bin\mintty.exe" -i "%GITDIR%\git-bash.exe" -t "LD2 curl" /usr/bin/bash --rcfile demo/zetonai.sh -i
