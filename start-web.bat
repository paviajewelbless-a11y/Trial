@echo off
echo Starting FeedMill Ops Web Portal at http://localhost:5000 ...
start "" http://localhost:5000
where python >nul 2>nul
if %errorlevel%==0 (
  python -m http.server 5000 -d web
) else (
  npx --yes serve web -l 5000
)
