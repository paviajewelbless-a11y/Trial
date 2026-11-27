@echo off
setlocal
cd /d "%~dp0"

echo ============================================================
echo  FeedMill Ops - Publish Web Portal to Firebase Hosting
echo  Target URL: https://gen-lang-client-0293694787.web.app
echo ============================================================
echo.

where firebase >nul 2>nul
if %errorlevel%==0 (
  echo Found Firebase CLI. Signing in and deploying web/ ...
  call firebase login
  call firebase deploy --only hosting --project gen-lang-client-0293694787
  if %errorlevel%==0 goto :success
  goto :failed
)

where npx >nul 2>nul
if %errorlevel%==0 (
  echo Found Node.js. Running firebase-tools via npx ...
  call npx --yes firebase-tools login
  call npx --yes firebase-tools deploy --only hosting --project gen-lang-client-0293694787
  if %errorlevel%==0 goto :success
  goto :failed
)

echo Firebase CLI and Node.js were not found. Checking standalone Firebase CLI via PowerShell...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "$dir = Join-Path $env:LOCALAPPDATA 'FeedMillOps';" ^
  "New-Item -ItemType Directory -Force -Path $dir | Out-Null;" ^
  "$exe = Join-Path $dir 'firebase.exe';" ^
  "if (-not (Test-Path $exe)) {" ^
  "  Write-Host 'Downloading official Firebase CLI for Windows...' -ForegroundColor Cyan;" ^
  "  Invoke-WebRequest -Uri 'https://firebase.tools/bin/win/latest' -OutFile $exe -UseBasicParsing;" ^
  "}" ^
  "& $exe login;" ^
  "if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }" ^
  "& $exe deploy --only hosting --project gen-lang-client-0293694787;" ^
  "exit $LASTEXITCODE"
if %errorlevel%==0 goto :success

:failed
echo.
echo [ERROR] Deployment did not complete.
echo Sign in with the Google account that owns project gen-lang-client-0293694787 and try again.
pause
exit /b 1

:success
echo.
echo Success! Opening https://gen-lang-client-0293694787.web.app ...
start "" https://gen-lang-client-0293694787.web.app
pause
