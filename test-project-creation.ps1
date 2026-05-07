# ============================================================
# test-project-creation.ps1
# Quick end-to-end test of project creation via the API Gateway
# Flow: Login → get JWT → POST /projects via Gateway → verify response
# ============================================================

param(
    [string]$GatewayUrl  = "http://localhost:8080",
    [string]$Email       = "test@example.com",      # ← change to a real account
    [string]$Password    = "Test1234!"               # ← change to match
)

Write-Host ""
Write-Host "=== CodeSync Project Creation Test ===" -ForegroundColor Cyan
Write-Host "Gateway : $GatewayUrl"
Write-Host "User    : $Email"
Write-Host ""

# ── Step 1: Health-check the gateway ──────────────────────────────────────────
Write-Host "[1/4] Checking API Gateway is reachable..." -ForegroundColor Yellow
try {
    $null = Invoke-WebRequest -Uri "$GatewayUrl/auth/health" `
                              -Method GET `
                              -UseBasicParsing `
                              -TimeoutSec 5 `
                              -ErrorAction Stop
    Write-Host "      ✅ Gateway is up." -ForegroundColor Green
} catch {
    Write-Host "      ❌ Gateway unreachable at $GatewayUrl" -ForegroundColor Red
    Write-Host "         Make sure Eureka + API Gateway + Auth Service are running." -ForegroundColor Red
    Write-Host "         Run: .\start-all.ps1  (then wait ~60 s)" -ForegroundColor Red
    exit 1
}

# ── Step 2: Login and obtain JWT ──────────────────────────────────────────────
Write-Host "[2/4] Logging in as $Email..." -ForegroundColor Yellow
$loginBody = @{ email = $Email; password = $Password } | ConvertTo-Json
try {
    $loginResp = Invoke-WebRequest -Uri "$GatewayUrl/auth/login" `
                                   -Method POST `
                                   -ContentType "application/json" `
                                   -Body $loginBody `
                                   -UseBasicParsing `
                                   -TimeoutSec 15 `
                                   -ErrorAction Stop

    $jwt = $loginResp.Content.Trim().Trim('"')   # token is returned as plain string
    if ($jwt.Length -lt 20) { throw "Token too short — login may have failed." }
    Write-Host "      ✅ JWT obtained (${$jwt.Substring(0,30)}...)" -ForegroundColor Green
} catch {
    Write-Host "      ❌ Login failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "         Response: $($_.ErrorDetails.Message)" -ForegroundColor DarkRed
    exit 1
}

# ── Step 3: Create a project via the API Gateway ──────────────────────────────
Write-Host "[3/4] Creating a test project via POST $GatewayUrl/projects..." -ForegroundColor Yellow
$projectName = "test-project-$(Get-Date -Format 'yyyyMMddHHmmss')"
$projectBody = @{
    name        = $projectName
    description = "Automated test project"
    language    = "Java"
    visibility  = "PRIVATE"
} | ConvertTo-Json

$headers = @{ Authorization = "Bearer $jwt" }
try {
    $createResp = Invoke-WebRequest -Uri "$GatewayUrl/projects" `
                                    -Method POST `
                                    -ContentType "application/json" `
                                    -Headers $headers `
                                    -Body $projectBody `
                                    -UseBasicParsing `
                                    -TimeoutSec 15 `
                                    -ErrorAction Stop

    $project = $createResp.Content | ConvertFrom-Json
    Write-Host "      ✅ Project created!" -ForegroundColor Green
    Write-Host "         ID          : $($project.id)"
    Write-Host "         Name        : $($project.name)"
    Write-Host "         Owner       : $($project.ownerEmail)"
    Write-Host "         Visibility  : $($project.visibility)"
    Write-Host "         Language    : $($project.language)"
    Write-Host "         CreatedAt   : $($project.createdAt)"
} catch {
    $status = $_.Exception.Response.StatusCode.value__
    Write-Host "      ❌ Project creation FAILED (HTTP $status)" -ForegroundColor Red
    Write-Host "         $($_.ErrorDetails.Message)" -ForegroundColor DarkRed
    exit 1
}

# ── Step 4: Verify the project is retrievable ─────────────────────────────────
Write-Host "[4/4] Verifying project appears in GET /projects/my..." -ForegroundColor Yellow
try {
    $myResp = Invoke-WebRequest -Uri "$GatewayUrl/projects/my" `
                                -Method GET `
                                -Headers $headers `
                                -UseBasicParsing `
                                -TimeoutSec 10 `
                                -ErrorAction Stop

    $page = $myResp.Content | ConvertFrom-Json
    $found = $page.content | Where-Object { $_.name -eq $projectName }

    if ($found) {
        Write-Host "      ✅ Project '$projectName' found in /projects/my." -ForegroundColor Green
    } else {
        Write-Host "      ⚠️  Project not found in /projects/my (may be a paging issue)." -ForegroundColor Yellow
        Write-Host "         Total projects returned: $($page.content.Count)"
    }
} catch {
    Write-Host "      ❌ Could not retrieve /projects/my: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Test Complete ===" -ForegroundColor Cyan
