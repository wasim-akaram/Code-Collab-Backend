# ============================================================
#  stop-all.ps1  --  CodeSync full-stack shutdown
#  Run from anywhere -- kills all CodeSync Spring Boot JVMs
#  and any Maven/Angular terminals launched by start-all.ps1
# ============================================================

Write-Host ""
Write-Host "============================================" -ForegroundColor Red
Write-Host "  CodeSync -- Stopping All Services" -ForegroundColor Red
Write-Host "============================================" -ForegroundColor Red
Write-Host ""

# --- 1. Kill Spring Boot / Maven JVM processes ---------------
Write-Host "Searching for CodeSync Java processes..." -ForegroundColor Yellow

$javaProcesses = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Where-Object {
    $_.CommandLine -match "spring-boot"         -or
    $_.CommandLine -match "auth-service"         -or
    $_.CommandLine -match "api-gateway"          -or
    $_.CommandLine -match "eureka-server"        -or
    $_.CommandLine -match "project-service"      -or
    $_.CommandLine -match "file-service"         -or
    $_.CommandLine -match "collab-service"       -or
    $_.CommandLine -match "version-service"      -or
    $_.CommandLine -match "execution-service"    -or
    $_.CommandLine -match "comment-service"      -or
    $_.CommandLine -match "notification-service" -or
    $_.CommandLine -match "com\.codesync"
}

if ($javaProcesses) {
    foreach ($process in $javaProcesses) {
        $cmdPreview = $process.CommandLine.Substring(0, [Math]::Min(80, $process.CommandLine.Length))
        Write-Host "  Stopping Java PID $($process.ProcessId) -- $cmdPreview..." -ForegroundColor Cyan
        Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
    }
    Write-Host "  All CodeSync Java processes stopped." -ForegroundColor Green
} else {
    Write-Host "  No CodeSync Java processes found." -ForegroundColor DarkCyan
}

# --- 2. Kill PowerShell terminals from start-all.ps1 ---------
Write-Host ""
Write-Host "Searching for Maven/Angular terminal windows..." -ForegroundColor Yellow

$devTerminals = Get-CimInstance Win32_Process -Filter "Name = 'powershell.exe' OR Name = 'pwsh.exe'" | Where-Object {
    $_.CommandLine -match "spring-boot:run" -or
    $_.CommandLine -match "npm run start"   -or
    $_.CommandLine -match "ng serve"
}

if ($devTerminals) {
    foreach ($term in $devTerminals) {
        Write-Host "  Closing terminal PID $($term.ProcessId)..." -ForegroundColor Cyan
        Stop-Process -Id $term.ProcessId -Force -ErrorAction SilentlyContinue
    }
    Write-Host "  Dev terminals closed." -ForegroundColor Green
} else {
    Write-Host "  No dev terminals found." -ForegroundColor DarkCyan
}

# --- 3. Kill Node.js process (Angular dev server) ------------
Write-Host ""
Write-Host "Searching for Node.js (Angular) process..." -ForegroundColor Yellow

$nodeProcesses = Get-CimInstance Win32_Process -Filter "Name = 'node.exe'" | Where-Object {
    $_.CommandLine -match "ng serve" -or
    $_.CommandLine -match "angular"  -or
    $_.CommandLine -match "CODE-COLLAB\\frontend"
}

if ($nodeProcesses) {
    foreach ($proc in $nodeProcesses) {
        Write-Host "  Stopping Node PID $($proc.ProcessId)..." -ForegroundColor Cyan
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
    }
    Write-Host "  Angular dev server stopped." -ForegroundColor Green
} else {
    Write-Host "  No Angular dev server process found." -ForegroundColor DarkCyan
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Red
Write-Host "  All CodeSync services have been stopped." -ForegroundColor Red
Write-Host "============================================" -ForegroundColor Red
Write-Host ""
