param (
    [Parameter(Mandatory=$true)]
    [string]$Token,
    [string]$HostUrl = "http://localhost:9000"
)

$backendDir = "d:\CODE-COLLAB\backend"
$dirs = Get-ChildItem -Path $backendDir -Directory

foreach ($dir in $dirs) {
    $pomPath = Join-Path -Path $dir.FullName -ChildPath "pom.xml"
    if (Test-Path $pomPath) {
        Write-Host "Running SonarQube analysis for: $($dir.Name)" -ForegroundColor Cyan
        Set-Location -Path $dir.FullName
        # Make sure tests have run to generate jacoco.exec
        mvn clean verify sonar:sonar "-Dsonar.projectKey=$($dir.Name)" "-Dsonar.host.url=$HostUrl" "-Dsonar.login=$Token"
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Error running analysis for $($dir.Name)" -ForegroundColor Red
        }
    }
}

Set-Location -Path $backendDir
Write-Host "Finished SonarQube backend analysis." -ForegroundColor Green
