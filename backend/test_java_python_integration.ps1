$ErrorActionPreference = "Stop"

$springUrl = "http://localhost:8080/api"
$mlUrl = "http://127.0.0.1:8000"
$testRunId = [System.Guid]::NewGuid().ToString().Substring(0, 8)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " QueueLess AI - Phase 8: Java <-> Python Integration Test" -ForegroundColor Cyan
Write-Host " Run ID: $testRunId" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$passCount = 0
$failCount = 0

function Assert-Check($condition, $message) {
    if ($condition) {
        Write-Host "  [PASS] $message" -ForegroundColor Green
        $script:passCount++
    } else {
        Write-Host "  [FAIL] $message" -ForegroundColor Red
        $script:failCount++
    }
}

function Register-Login($name, $email, $role) {
    $reg = @{ name = $name; email = $email; password = "Password123!"; role = $role } | ConvertTo-Json
    $user = Invoke-RestMethod -Uri "$springUrl/auth/register" -Method Post -ContentType "application/json" -Body $reg
    $login = @{ email = $email; password = "Password123!" } | ConvertTo-Json
    $auth = Invoke-RestMethod -Uri "$springUrl/auth/login" -Method Post -ContentType "application/json" -Body $login
    return @{ user = $user; token = $auth.accessToken }
}

# ------------------------------------------------------------------
# Step 1: Health checks
# ------------------------------------------------------------------
Write-Host "`n--- Step 1: Microservice Health Checks ---" -ForegroundColor Yellow
$mlHealth = Invoke-RestMethod -Uri "$mlUrl/health" -Method Get
Assert-Check ($mlHealth.status -eq "UP" -and $mlHealth.model_loaded -eq $true) "Python ML microservice is UP and model is loaded"

# ------------------------------------------------------------------
# Step 2: Register Owner and setup Business, Service, Queue
# ------------------------------------------------------------------
Write-Host "`n--- Step 2: Setup Business, Service & Queue ---" -ForegroundColor Yellow
$owner = Register-Login "Dr. Sarah Lin" "sarah_$testRunId@apexmed.com" "BUSINESS_OWNER"
$ownerH = @{ "Authorization" = "Bearer $($owner.token)" }

$biz = Invoke-RestMethod -Uri "$springUrl/businesses" -Method Post -Headers $ownerH -ContentType "application/json" -Body (@{
    name = "Apex Diagnostic Imaging"
    address = "100 Medical Center Blvd"
    phone = "555-432-1098"
    description = "Advanced MRI and diagnostic clinic"
} | ConvertTo-Json)

$svc = Invoke-RestMethod -Uri "$springUrl/businesses/$($biz.id)/services" -Method Post -Headers $ownerH -ContentType "application/json" -Body (@{
    name = "MRI Scan & Diagnostic Consultation"
    description = "Full scan and review"
    estimatedDurationMinutes = 25
} | ConvertTo-Json)

$queue = Invoke-RestMethod -Uri "$springUrl/services/$($svc.id)/queues" -Method Post -Headers $ownerH -ContentType "application/json" -Body (@{
    activeCounters = 2
} | ConvertTo-Json)
Assert-Check ($queue.id -ne $null -and $queue.activeCounters -eq 2) "Queue opened: 2 active counters, 25 min base duration"

# ------------------------------------------------------------------
# Step 3: Register 5 Customers and Join Queue
# ------------------------------------------------------------------
Write-Host "`n--- Step 3: Customers Join Queue (Observing ML Predictions) ---" -ForegroundColor Yellow
$customers = @()
for ($i = 1; $i -le 5; $i++) {
    $c = Register-Login "Customer $i" "patient${i}_$testRunId@example.com" "CUSTOMER"
    $cH = @{ "Authorization" = "Bearer $($c.token)" }
    $entry = Invoke-RestMethod -Uri "$springUrl/queues/$($queue.id)/entries" -Method Post -Headers $cH
    $customers += @{ client = $c; entry = $entry; header = $cH }
    Write-Host "   Token $($entry.tokenNumber): wait=$($entry.estimatedWaitMinutes)m, source=$($entry.estimationSource)" -ForegroundColor DarkGray
}

Assert-Check ($customers.Count -eq 5) "5 customers joined queue"
# Customer 1 is next in line -> 0 wait
Assert-Check ($customers[0].entry.estimatedWaitMinutes -eq 0) "Customer 1 (T-001) initial wait is 0 mins"
# Customers 2 to 5 should have received predictions from ML_MODEL
Assert-Check ($customers[4].entry.estimationSource -eq "ML_MODEL") "Customer 5 ticket generated with estimationSource = ML_MODEL"

# ------------------------------------------------------------------
# Step 4: Verify Dedicated GET /api/queue-entries/{id}/estimate
# ------------------------------------------------------------------
Write-Host "`n--- Step 4: Test GET /api/queue-entries/{id}/estimate with ML ---" -ForegroundColor Yellow
$c5EntryId = $customers[4].entry.id
$c5Estimate = Invoke-RestMethod -Uri "$springUrl/queue-entries/$c5EntryId/estimate" -Method Get -Headers $customers[4].header

Assert-Check ($c5Estimate.estimationSource -eq "ML_MODEL") "Estimation source is ML_MODEL"
Assert-Check ($c5Estimate.peopleAhead -eq 4) "People ahead: 4"
Assert-Check ($c5Estimate.position -eq 5) "Position in queue: 5"
Assert-Check ($c5Estimate.activeCounters -eq 2) "Active counters: 2"
Assert-Check ($c5Estimate.estimatedWaitMinutes -gt 0) "Estimated wait: $($c5Estimate.estimatedWaitMinutes) mins"
Assert-Check ($c5Estimate.explanation -match "ML predicted") "Explanation references ML model adjustment"
Write-Host "   Explanation: $($c5Estimate.explanation)" -ForegroundColor DarkGray

# ------------------------------------------------------------------
# Step 5: Verify Customer 5 Dashboard GET /queues/{id}/entries/my
# ------------------------------------------------------------------
Write-Host "`n--- Step 5: Verify Customer Dashboard (/entries/my) ---" -ForegroundColor Yellow
$c5Position = Invoke-RestMethod -Uri "$springUrl/queues/$($queue.id)/entries/my" -Method Get -Headers $customers[4].header
Assert-Check ($c5Position.tokenNumber -eq "T-005") "Customer 5 has token T-005"
Assert-Check ($c5Position.estimationSource -eq "ML_MODEL") "Dashboard estimationSource is ML_MODEL"
Assert-Check ($c5Position.peopleAhead -eq 4) "Dashboard reports 4 people ahead"

# ------------------------------------------------------------------
# Step 6: Full Queue Movement with Staff Operations & Live Recalculation
# ------------------------------------------------------------------
Write-Host "`n--- Step 6: Queue Progress (Staff calls T-001) ---" -ForegroundColor Yellow
$called1 = Invoke-RestMethod -Uri "$springUrl/queues/$($queue.id)/entries/call-next" -Method Put -Headers $ownerH
Assert-Check ($called1.tokenNumber -eq "T-001") "Staff called T-001"

$c5AfterCall = Invoke-RestMethod -Uri "$springUrl/queue-entries/$c5EntryId/estimate" -Method Get -Headers $customers[4].header
Assert-Check ($c5AfterCall.peopleAhead -eq 3) "Customer 5 people ahead dynamically reduced to 3"
Assert-Check ($c5AfterCall.position -eq 4) "Customer 5 position is now 4"
Assert-Check ($c5AfterCall.estimationSource -eq "ML_MODEL") "Estimation source remains ML_MODEL"

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  Phase 8 Test Summary" -ForegroundColor Cyan
Write-Host "  PASSED: $passCount" -ForegroundColor Green
Write-Host "  FAILED: $failCount" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Red" })
Write-Host "==========================================================" -ForegroundColor Cyan

if ($failCount -gt 0) {
    exit 1
}
