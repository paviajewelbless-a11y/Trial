param(
  [int]$Port = 5000
)

$ErrorActionPreference = "Stop"
$WebRoot = Join-Path $PSScriptRoot "web"
if (-not (Test-Path $WebRoot)) {
  Write-Host "Could not find 'web' folder next to start-web.bat." -ForegroundColor Red
  Read-Host "Press Enter to exit"
  exit 1
}

$Listener = New-Object System.Net.HttpListener
$Prefix = "http://localhost:$Port/"
$Listener.Prefixes.Add($Prefix)
$Listener.Start()

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
