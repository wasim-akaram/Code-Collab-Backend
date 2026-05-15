$token = Invoke-RestMethod -Uri 'http://localhost:8080/auth/login' -Method Post -InFile 'd:\CODE-COLLAB\backend\auth-service\test-login.json' -ContentType 'application/json'
Write-Host "Token obtained, length: $($token.Length)"

$headers = @{ "Authorization" = "Bearer $token" }

$my = Invoke-RestMethod -Uri 'http://localhost:8080/projects/my' -Headers $headers
Write-Host "GET /projects/my: $($my.totalElements) project(s)"

$pub = Invoke-RestMethod -Uri 'http://localhost:8080/projects/public' -Headers $headers
Write-Host "GET /projects/public: $($pub.totalElements) project(s)"

$trend = Invoke-RestMethod -Uri 'http://localhost:8080/projects/trending' -Headers $headers
Write-Host "GET /projects/trending: $($trend.totalElements) project(s)"

$search = Invoke-RestMethod -Uri 'http://localhost:8080/projects/search?searchTerm=test' -Headers $headers
Write-Host "GET /projects/search: $($search.totalElements) result(s)"

$arch = Invoke-RestMethod -Uri 'http://localhost:8080/projects/archived' -Headers $headers
Write-Host "GET /projects/archived: $($arch.totalElements) project(s)"

Write-Host "All endpoints OK!"
