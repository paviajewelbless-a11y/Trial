@echo off
cd /d "%~dp0"
echo Starting FeedMill Ops Web Portal at http://localhost:5000 ...

python -V >nul 2>nul
if %errorlevel%==0 (
  echo Using Python web server. Keep this window open while using Chrome.
  start "" http://localhost:5000
  python -m http.server 5000 --directory web
  pause
  goto :end
)

where npx >nul 2>nul
if %errorlevel%==0 (
  echo Using Node serve. Keep this window open while using Chrome.
  start "" http://localhost:5000
  npx --yes serve web -l 5000
  pause
  goto :end
)

echo Using built-in Windows PowerShell web server...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0web-server.ps1"
pause

:end
