@echo off
rem Paleidzia LD2 API su svaria demonstracine DB ir atidaro Swagger, kai API pasiruosusi.
title LD2 Done-n-Dusted API
cd /d "%~dp0.."
call "%~dp00-sustabdyti.cmd" quiet
echo ============================================================
echo  Done-n-Dusted LD2 API
echo  DB kiekviena karta svari. Naudotojai: admin, rokas, ieva, jonas
echo  (visu slaptazodis Demo123!). Swagger atsidarys pats, kai API
echo  pasileis (eilute "Started DoneNDustedApplication").
echo  Sustabdyti: Ctrl+C sitame lange arba 0-sustabdyti.
echo ============================================================
start "" /min powershell -NoProfile -WindowStyle Hidden -Command "for($i=0;$i -lt 300;$i++){try{$null=Invoke-WebRequest 'http://localhost:8080/v3/api-docs' -UseBasicParsing -TimeoutSec 2; Start-Process 'http://localhost:8080/swagger-ui.html'; break}catch{Start-Sleep 1}}"
call .\mvnw.cmd spring-boot:run
pause
