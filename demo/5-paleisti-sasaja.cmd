@echo off
rem Paleidzia React sasaja (http://localhost:5173) ir, kai ji pasiruosusi, atidaro narsykle.
rem API turi veikti (1-paleisti-api.cmd): sasaja /api uzklausas nukreipia i localhost:8080.
title LD3 Done-n-Dusted sasaja
cd /d "%~dp0..\frontend"
if not exist node_modules (
  echo Pirmas paleidimas - diegiamos priklausomybes, tai uztruks minute...
  call npm install
)
echo ============================================================
echo  Done-n-Dusted LD3 sasaja: http://localhost:5173
echo  Naudotojai: admin, rokas, ieva, jonas (slaptazodis Demo123!)
echo  Sustabdyti: Ctrl+C sitame lange arba 0-sustabdyti.
echo ============================================================
start "" /min powershell -NoProfile -WindowStyle Hidden -Command "for($i=0;$i -lt 120;$i++){try{$null=Invoke-WebRequest 'http://localhost:5173' -UseBasicParsing -TimeoutSec 2; Start-Process 'http://localhost:5173'; break}catch{Start-Sleep 1}}"
call npm run dev
pause
