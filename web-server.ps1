param(
  [int]$Port = 0
)

$ErrorActionPreference = "Stop"
$WebRoot = Join-Path $PSScriptRoot "web"
if (-not (Test-Path $WebRoot)) {
  Write-Host "Could not find 'web' folder next to start-web.bat." -ForegroundColor Red
  Read-Host "Press Enter to exit"
  exit 1
}

$CandidatePorts = if ($Port -gt 0) { @($Port) } else { @(8123, 5173, 5500, 8456, 8765, 9095, 5050) }
$Listener = $null
$Prefix = ""

foreach ($p in $CandidatePorts) {
  $TryListener = New-Object System.Net.HttpListener
  $TryPrefix = "http://localhost:$p/"
  $TryListener.Prefixes.Add($TryPrefix)
  try {
    $TryListener.Start()
    $Listener = $TryListener
    $Prefix = $TryPrefix
    break
  } catch {
    try { $TryListener.Close() } catch {}
  }
}

if (-not $Listener) {
  Write-Host "Could not bind any local port ($($CandidatePorts -join ', '))." -ForegroundColor Red
  Read-Host "Press Enter to exit"
  exit 1
}

Write-Host "FeedMill Ops Web Portal is running at $Prefix" -ForegroundColor Green
Write-Host "Keep this window open while using Chrome. Press Ctrl+C to stop." -ForegroundColor Yellow
Start-Process $Prefix

$MimeTypes = @{
  ".html" = "text/html; charset=utf-8"
  ".js"   = "application/javascript; charset=utf-8"
  ".mjs"  = "application/javascript; charset=utf-8"
  ".css"  = "text/css; charset=utf-8"
  ".json" = "application/json; charset=utf-8"
  ".png"  = "image/png"
  ".jpg"  = "image/jpeg"
  ".svg"  = "image/svg+xml"
  ".ico"  = "image/x-icon"
}

try {
  while ($Listener.IsListening) {
    $Context = $Listener.GetContext()
    $ReqPath = $Context.Request.Url.AbsolutePath.TrimStart("/")
    if ([string]::IsNullOrWhiteSpace($ReqPath)) {
      $ReqPath = "index.html"
    }
    $SafeRel = $ReqPath -replace "\.\.", ""
    $FullPath = Join-Path $WebRoot $SafeRel
    if (-not (Test-Path $FullPath -PathType Leaf)) {
      $FullPath = Join-Path $WebRoot "index.html"
    }

    $Ext = [System.IO.Path]::GetExtension($FullPath).ToLowerInvariant()
    $ContentType = if ($MimeTypes.ContainsKey($Ext)) { $MimeTypes[$Ext] } else { "application/octet-stream" }
    $Bytes = [System.IO.File]::ReadAllBytes($FullPath)

    $Context.Response.StatusCode = 200
    $Context.Response.ContentType = $ContentType
    $Context.Response.ContentLength64 = $Bytes.Length
    $Context.Response.OutputStream.Write($Bytes, 0, $Bytes.Length)
    $Context.Response.OutputStream.Close()
  }
} finally {
  $Listener.Stop()
  $Listener.Close()
}
