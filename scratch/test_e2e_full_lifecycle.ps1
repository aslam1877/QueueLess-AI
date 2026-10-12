# QueueLess AI - Full End-to-End System Integration Test
# Validates Spring Boot, Python FastAPI ML Service, and React Frontend Proxy

$ErrorActionPreference = "Stop"

function Assert-Condition($condition, $message) {
    if ($condition) {
        Write-Host " [PASS] $message" -ForegroundColor Green
    } else {
        Write-Host " [FAIL] $message" -ForegroundColor Red
        exit 1
    }
}

Write-Host "`n=== 1. SERVICE LIVENESS & PROXY CHECKS ===" -ForegroundColor Cyan

# 1.1 Python ML Microservice Health
$mlHealth = Invoke-RestMethod -Uri "http://127.0.0.1:8000/health" -Method Get
Assert-Condition ($mlHealth.status -eq "UP") "FastAPI ML service is UP on port 8000"
Assert-Condition ($mlHealth.model_loaded -eq $true) "ML model is loaded in memory"

# 1.2 Frontend Liveness
$feRes = Invoke-WebRequest -Uri "http://localhost:5173" -Method Get -UseBasicParsing
Assert-Condition ($feRes.StatusCode -eq 200) "React Vite frontend is serving on port 5173"

# 1.3 Frontend -> Python ML Proxy
$mlProxyHealth = Invoke-RestMethod -Uri "http://localhost:5173/ml/health" -Method Get
Assert-Condition ($mlProxyHealth.status -eq "UP") "Frontend /ml proxy to FastAPI is functional"

Write-Host "`n=== 2. AUTHENTICATION & ACCESS CONTROL ===" -ForegroundColor Cyan

$ts = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()

# 2.1 Register Business Owner
$ownerPayload = @{
    name = "Dr. Marcus Vance"
    email = "owner_$ts@queueless.com"
    password = "Password123!"
    role = "BUSINESS_OWNER"
} | ConvertTo-Json

$ownerReg = Invoke-RestMethod -Uri "http://localhost:5173/api/auth/register" -Method Post -Body $ownerPayload -ContentType "application/json"
Assert-Condition ($ownerReg.id -gt 0) "Business Owner registered successfully"

# 2.2 Owner Login
$ownerLoginPayload = @{
    email = "owner_$ts@queueless.com"
    password = "Password123!"
} | ConvertTo-Json
$ownerAuth = Invoke-RestMethod -Uri "http://localhost:5173/api/auth/login" -Method Post -Body $ownerLoginPayload -ContentType "application/json"
$ownerToken = $ownerAuth.accessToken
Assert-Condition ($ownerToken.Length -gt 20) "Owner received JWT token"

# 2.3 Register Customer
$custPayload = @{
    name = "Elena Rostova"
    email = "customer_$ts@queueless.com"
    password = "Password123!"
    role = "CUSTOMER"
} | ConvertTo-Json
$custReg = Invoke-RestMethod -Uri "http://localhost:5173/api/auth/register" -Method Post -Body $custPayload -ContentType "application/json"
Assert-Condition ($custReg.id -gt 0) "Customer registered successfully"

$custLoginPayload = @{
    email = "customer_$ts@queueless.com"
    password = "Password123!"
} | ConvertTo-Json
$custAuth = Invoke-RestMethod -Uri "http://localhost:5173/api/auth/login" -Method Post -Body $custLoginPayload -ContentType "application/json"
$custToken = $custAuth.accessToken
Assert-Condition ($custToken.Length -gt 20) "Customer received JWT token"

# 2.4 Negative Security Test: Customer attempts to create business (Forbidden 403)
$forbiddenFailed = $false
try {
    Invoke-RestMethod -Uri "http://localhost:5173/api/businesses" -Method Post -Headers @{ Authorization = "Bearer $custToken" } -Body (@{ name = "Hacker Biz" } | ConvertTo-Json) -ContentType "application/json"
} catch {
    $forbiddenFailed = ($_.Exception.Response.StatusCode.value__ -eq 403)
}
Assert-Condition ($forbiddenFailed) "Security: Customer cannot create business (HTTP 403 Forbidden enforced)"

Write-Host "`n=== 3. BUSINESS & SERVICE LIFECYCLE ===" -ForegroundColor Cyan

# 3.1 Owner creates Business
$bizPayload = @{
    name = "Metropolitan Cardiology Clinic"
    address = "500 Heart Way, Suite 200"
    phone = "555-019-8234"
    description = "Specialized cardiology diagnostic clinic"
} | ConvertTo-Json
$biz = Invoke-RestMethod -Uri "http://localhost:5173/api/businesses" -Method Post -Headers @{ Authorization = "Bearer $ownerToken" } -Body $bizPayload -ContentType "application/json"
Assert-Condition ($biz.id -gt 0) "Business created: '$($biz.name)' (ID: $($biz.id))"

# 3.2 Owner creates Service
$svcPayload = @{
    name = "Echocardiogram Diagnostic"
    description = "Ultrasound examination of heart chambers"
    estimatedDurationMinutes = 30
} | ConvertTo-Json
$svc = Invoke-RestMethod -Uri "http://localhost:5173/api/businesses/$($biz.id)/services" -Method Post -Headers @{ Authorization = "Bearer $ownerToken" } -Body $svcPayload -ContentType "application/json"
Assert-Condition ($svc.id -gt 0 -and $svc.estimatedDurationMinutes -eq 30) "Service created: '$($svc.name)' (30 mins)"

Write-Host "`n=== 4. QUEUE CREATION & TOKEN ISSUANCE ===" -ForegroundColor Cyan

# 4.1 Open Queue with 2 active counters
$queuePayload = @{ activeCounters = 2 } | ConvertTo-Json
$q = Invoke-RestMethod -Uri "http://localhost:5173/api/services/$($svc.id)/queues" -Method Post -Headers @{ Authorization = "Bearer $ownerToken" } -Body $queuePayload -ContentType "application/json"
Assert-Condition ($q.id -gt 0 -and $q.status -eq "OPEN") "Queue opened with $($q.activeCounters) active counters"

# 4.2 Customer 1 joins queue
$c1Entry = Invoke-RestMethod -Uri "http://localhost:5173/api/queues/$($q.id)/entries" -Method Post -Headers @{ Authorization = "Bearer $custToken" }
Assert-Condition ($c1Entry.tokenNumber -eq "T-001" -and $c1Entry.status -eq "WAITING") "Customer 1 joined: Token T-001 issued"

# 4.3 Customer 2 joins queue
$cust2Payload = @{
    name = "Liam Chen"
    email = "customer2_$ts@queueless.com"
    password = "Password123!"
    role = "CUSTOMER"
} | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:5173/api/auth/register" -Method Post -Body $cust2Payload -ContentType "application/json" | Out-Null
$c2Auth = Invoke-RestMethod -Uri "http://localhost:5173/api/auth/login" -Method Post -Body (@{ email = "customer2_$ts@queueless.com"; password = "Password123!" } | ConvertTo-Json) -ContentType "application/json"
$c2Token = $c2Auth.accessToken

$c2Entry = Invoke-RestMethod -Uri "http://localhost:5173/api/queues/$($q.id)/entries" -Method Post -Headers @{ Authorization = "Bearer $c2Token" }
Assert-Condition ($c2Entry.tokenNumber -eq "T-002" -and $c2Entry.status -eq "WAITING") "Customer 2 joined: Token T-002 issued sequentially"

# 4.4 Check Queue Position
$c1Pos = Invoke-RestMethod -Uri "http://localhost:5173/api/queues/$($q.id)/entries/my" -Method Get -Headers @{ Authorization = "Bearer $custToken" }
Assert-Condition ($c1Pos.peopleAhead -eq 0) "Customer 1 has 0 people ahead"

$c2Pos = Invoke-RestMethod -Uri "http://localhost:5173/api/queues/$($q.id)/entries/my" -Method Get -Headers @{ Authorization = "Bearer $c2Token" }
Assert-Condition ($c2Pos.peopleAhead -eq 1) "Customer 2 has 1 person ahead"

Write-Host "`n=== 5. WAIT TIME ESTIMATE & ML PREDICTION ===" -ForegroundColor Cyan

# 5.1 Estimation via Spring Boot endpoint (calls Python ML under the hood)
$c2Estimate = Invoke-RestMethod -Uri "http://localhost:5173/api/queue-entries/$($c2Entry.id)/estimate" -Method Get -Headers @{ Authorization = "Bearer $c2Token" }
Assert-Condition ($c2Estimate.estimatedWaitMinutes -ge 0) "Estimated wait minutes returned: $($c2Estimate.estimatedWaitMinutes) min"
Assert-Condition ($c2Estimate.estimationSource -in @("ML_MODEL", "BASELINE")) "Estimation source is valid: $($c2Estimate.estimationSource)"

# 5.2 Direct ML Microservice Prediction via /ml reverse proxy
$mlReq = @{
    people_ahead = 5
    active_counters = 2
    service_duration_min = 25.0
    hour_of_day = 13
    day_of_week = 4
    service_category = "PROCEDURE"
} | ConvertTo-Json
$mlDirect = Invoke-RestMethod -Uri "http://localhost:5173/ml/predict" -Method Post -Body $mlReq -ContentType "application/json"
Assert-Condition ($mlDirect.predicted_wait_minutes -gt 0) "ML direct inference succeeded: $($mlDirect.predicted_wait_minutes) min (baseline: $($mlDirect.baseline_wait_minutes) min)"
Assert-Condition ($mlDirect.model_name -like "*Gradient Boosting*") "ML model used: $($mlDirect.model_name)"

Write-Host "`n=== 6. STAFF CONSOLE & QUEUE ADVANCEMENT ===" -ForegroundColor Cyan

# 6.1 Call Next Customer
$calledEntry = Invoke-RestMethod -Uri "http://localhost:5173/api/queues/$($q.id)/entries/call-next" -Method Put -Headers @{ Authorization = "Bearer $ownerToken" }
Assert-Condition ($calledEntry.tokenNumber -eq "T-001" -and $calledEntry.status -eq "CALLED") "Staff called next: T-001 transitioned to CALLED"

# 6.2 Start Service
$startedEntry = Invoke-RestMethod -Uri "http://localhost:5173/api/queue-entries/$($calledEntry.id)/start-service" -Method Put -Headers @{ Authorization = "Bearer $ownerToken" }
Assert-Condition ($startedEntry.status -eq "IN_SERVICE") "T-001 transitioned to IN_SERVICE"

# 6.3 Complete Service
$completedEntry = Invoke-RestMethod -Uri "http://localhost:5173/api/queue-entries/$($startedEntry.id)/complete" -Method Put -Headers @{ Authorization = "Bearer $ownerToken" }
Assert-Condition ($completedEntry.status -eq "COMPLETED") "T-001 transitioned to COMPLETED"

# 6.4 Verify Customer 2 Position Updated
$c2PosAfter = Invoke-RestMethod -Uri "http://localhost:5173/api/queues/$($q.id)/entries/my" -Method Get -Headers @{ Authorization = "Bearer $c2Token" }
Assert-Condition ($c2PosAfter.peopleAhead -eq 0) "Customer 2 moved up to position 0 (first in line)"

# 6.5 Call Customer 2
$called2 = Invoke-RestMethod -Uri "http://localhost:5173/api/queues/$($q.id)/entries/call-next" -Method Put -Headers @{ Authorization = "Bearer $ownerToken" }
Assert-Condition ($called2.tokenNumber -eq "T-002" -and $called2.status -eq "CALLED") "Staff called next: T-002 transitioned to CALLED"

# 6.6 Skip Customer 2 (No-show handling)
$skipped2 = Invoke-RestMethod -Uri "http://localhost:5173/api/queue-entries/$($called2.id)/skip" -Method Put -Headers @{ Authorization = "Bearer $ownerToken" }
Assert-Condition ($skipped2.status -eq "SKIPPED") "T-002 skipped due to no-show"

Write-Host "`n=== 7. QUEUE CLOSURE & REJECTION TESTS ===" -ForegroundColor Cyan

# 7.1 Close Queue
$closedQueue = Invoke-RestMethod -Uri "http://localhost:5173/api/queues/$($q.id)/close" -Method Put -Headers @{ Authorization = "Bearer $ownerToken" }
Assert-Condition ($closedQueue.status -eq "CLOSED") "Queue transitioned to CLOSED"

# 7.2 Joining a closed queue is rejected
$joinClosedFailed = $false
try {
    Invoke-RestMethod -Uri "http://localhost:5173/api/queues/$($q.id)/entries" -Method Post -Headers @{ Authorization = "Bearer $c2Token" }
} catch {
    $joinClosedFailed = $true
}
Assert-Condition ($joinClosedFailed) "Integrity: Joining closed queue is rejected"

Write-Host "`n=========================================================" -ForegroundColor Green
Write-Host " ALL END-TO-END INTEGRATION & EDGE TESTS PASSED! " -ForegroundColor Green
Write-Host "=========================================================`n" -ForegroundColor Green
