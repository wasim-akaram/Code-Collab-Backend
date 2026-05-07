# ============================================================
#  start-all.ps1  --  CodeSync full-stack dev launcher
#  Run from:  d:\CODE-COLLAB\backend\
#
#  Startup order (no Eureka dependency; gateway uses static URIs):
#    1. API Gateway          (8080) -- fast, no DB needed
#    2. Auth Service         (8081) -- needs PostgreSQL + Redis
#    3. Project Service      (8082) -- needs PostgreSQL
#    4. File Service         (8083) -- needs PostgreSQL
#    5. Collab Service       (8084) -- needs PostgreSQL
#    6. Version Service      (8085) -- needs PostgreSQL
#    7. Execution Service    (8086) -- needs PostgreSQL
#    8. Comment Service      (8087) -- needs PostgreSQL
#    9. Notification Service (8088) -- needs PostgreSQL
#   10. Frontend             (4200) -- Angular dev server
#  [Optional] Eureka Server  (8761) -- not required for routing
# ============================================================

$ROOT = "d:\CODE-COLLAB"

function Start-Service {
    param(
        [string]$Name,
        [string]$Path,
        [string]$Command = "mvn spring-boot:run",
        [int]$WaitSeconds = 0
    )
    Write-Host "  >> Starting $Name..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$Path'; Write-Host '=== $Name ===' -ForegroundColor Green; $Command"
    if ($WaitSeconds -gt 0) {
        Write-Host "     Waiting ${WaitSeconds}s for $Name to initialize..." -ForegroundColor DarkYellow
        Start-Sleep -Seconds $WaitSeconds
    }
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  CodeSync -- Starting All Services"        -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""

# --- 1. API Gateway ------------------------------------------
# Starts fast; does not need Eureka (uses static route URIs).
Start-Service -Name "API Gateway        :8080" `
              -Path "$ROOT\backend\api-gateway" `
              -WaitSeconds 8

# --- 2-9. Microservices --------------------------------------
# All connect to PostgreSQL. Stagger start slightly so the
# DB connection pool is not hammered simultaneously.
$microservices = @(
    @{ Name = "Auth Service         :8081"; Dir = "auth-service"         },
    @{ Name = "Project Service      :8082"; Dir = "project-service"      },
    @{ Name = "File Service         :8083"; Dir = "file-service"         },
    @{ Name = "Collab Service       :8084"; Dir = "collab-service"       },
    @{ Name = "Version Service      :8085"; Dir = "version-service"      },
    @{ Name = "Execution Service    :8086"; Dir = "execution-service"    },
    @{ Name = "Comment Service      :8087"; Dir = "comment-service"      },
    @{ Name = "Notification Service :8088"; Dir = "notification-service" }
)

foreach ($svc in $microservices) {
    Start-Service -Name $svc.Name `
                  -Path "$ROOT\backend\$($svc.Dir)" `
                  -WaitSeconds 6
}

# --- 10. Angular Frontend ------------------------------------
Start-Service -Name "Frontend (Angular) :4200" `
              -Path "$ROOT\frontend" `
              -Command "npm run start" `
              -WaitSeconds 0

# --- Optional: Eureka ----------------------------------------
# Uncomment to also start the Eureka service registry.
# The gateway does NOT require it (static URIs), but it gives
# a discovery dashboard at http://localhost:8761.
#
# Start-Service -Name "Eureka Server      :8761" `
#               -Path "$ROOT\backend\eureka-server" `
#               -WaitSeconds 0

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  All services launched in separate windows!" -ForegroundColor Green
Write-Host ""
Write-Host "  Service Ports:"                            -ForegroundColor White
Write-Host "    API Gateway         -> http://localhost:8080" -ForegroundColor White
Write-Host "    Auth Service        -> http://localhost:8081" -ForegroundColor White
Write-Host "    Project Service     -> http://localhost:8082" -ForegroundColor White
Write-Host "    File Service        -> http://localhost:8083" -ForegroundColor White
Write-Host "    Collab Service      -> http://localhost:8084" -ForegroundColor White
Write-Host "    Version Service     -> http://localhost:8085" -ForegroundColor White
Write-Host "    Execution Service   -> http://localhost:8086" -ForegroundColor White
Write-Host "    Comment Service     -> http://localhost:8087" -ForegroundColor White
Write-Host "    Notification Svc    -> http://localhost:8088" -ForegroundColor White
Write-Host "    Frontend            -> http://localhost:4200" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Wait ~30s for all Spring Boot services to fully start." -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
