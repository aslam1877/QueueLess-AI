$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080/api"
$testRunId = [System.Guid]::NewGuid().ToString().Substring(0, 8)
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  QueueLess AI - Phase 6: Baseline Estimation Test Suite" -ForegroundColor Cyan
Write-Host "  Run ID: $testRunId" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$passCount = 0
$failCount = 0

function Assert-Condition($condition, $message) {
    if ($condition) {
        Write-Host "  [PASS] $message" -ForegroundColor Green
        $script:passCount++
    } else {
        Write-Host "  [FAIL] $message" -ForegroundColor Red
        $script:failCount++
    }
}

function Register-And-Login($name, $email, $password, $role) {
    $regBody = @{
        name = $name
        email = $email
        password = $password
        role = $role
    } | ConvertTo-Json
    $user = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method Post -ContentType "application/json" -Body $regBody

    $loginBody = @{
        email = $email
        password = $password
    } | ConvertTo-Json
    $auth = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method Post -ContentType "application/json" -Body $loginBody

    return @{
        user = $user
        token = $auth.accessToken
    }
}

# ------------------------------------------------------------------
# Test 1: Register and login Business Owner
# ------------------------------------------------------------------
Write-Host "`n--- Step 1: Register and Login Business Owner ---" -ForegroundColor Yellow
$ownerEmail = "owner_$testRunId@queueless.com"
$ownerAuth = Register-And-Login -name "Dr. Auto Owner" -email $ownerEmail -password "Password123!" -role "BUSINESS_OWNER"
$ownerToken = $ownerAuth.token
Assert-Condition ($ownerToken -ne $null -and $ownerToken.Length -gt 20) "Owner registered and JWT accessToken received"

$authHeader = @{ "Authorization" = "Bearer $ownerToken" }

# ------------------------------------------------------------------
# Test 2: Create Business
# ------------------------------------------------------------------
Write-Host "`n--- Step 2: Create Business ---" -ForegroundColor Yellow
$businessBody = @{
    name = "Auto Express $testRunId"
    address = "456 Speed Way"
    phone = "555-987-6543"
    description = "Quick and reliable auto services"
} | ConvertTo-Json

$business = Invoke-RestMethod -Uri "$baseUrl/businesses" -Method Post -Headers $authHeader -ContentType "application/json" -Body $businessBody
$businessId = $business.id
Assert-Condition ($businessId -ne $null) "Business created with ID: $businessId"

# ------------------------------------------------------------------
# Test 3: Create Service with 30-minute estimated duration
# ------------------------------------------------------------------
Write-Host "`n--- Step 3: Create Service (30 min estimated duration) ---" -ForegroundColor Yellow
$serviceBody = @{
    name = "Comprehensive Tune-Up"
    description = "Full 50-point engine inspection and tune-up"
    estimatedDurationMinutes = 30
} | ConvertTo-Json

$service = Invoke-RestMethod -Uri "$baseUrl/businesses/$businessId/services" -Method Post -Headers $authHeader -ContentType "application/json" -Body $serviceBody
$serviceId = $service.id
Assert-Condition ($serviceId -ne $null -and $service.estimatedDurationMinutes -eq 30) "Service created with duration = 30 mins"

# ------------------------------------------------------------------
# Test 4: Open Queue with 2 active counters
# ------------------------------------------------------------------
Write-Host "`n--- Step 4: Open Queue with 2 active counters ---" -ForegroundColor Yellow
$queueBody = @{
    activeCounters = 2
} | ConvertTo-Json

$queue = Invoke-RestMethod -Uri "$baseUrl/services/$serviceId/queues" -Method Post -Headers $authHeader -ContentType "application/json" -Body $queueBody
$queueId = $queue.id
Assert-Condition ($queueId -ne $null -and $queue.activeCounters -eq 2) "Queue opened with ID: $queueId and 2 active counters"

# ------------------------------------------------------------------
# Test 5: Register 4 Customers
# ------------------------------------------------------------------
Write-Host "`n--- Step 5: Register and Login 4 Customers ---" -ForegroundColor Yellow
$customerTokens = @()
for ($i = 1; $i -le 4; $i++) {
    $cEmail = "cust${i}_$testRunId@example.com"
    $cAuth = Register-And-Login -name "Customer $i" -email $cEmail -password "Password123!" -role "CUSTOMER"
    $customerTokens += $cAuth.token
}
Assert-Condition ($customerTokens.Count -eq 4) "4 customer accounts registered and logged in with JWT"

# ------------------------------------------------------------------
# Test 6: Customers 1 to 4 join queue sequentially and verify initial baseline
# ------------------------------------------------------------------
Write-Host "`n--- Step 6: Customers Join Queue & Verify Initial Estimates ---" -ForegroundColor Yellow

# Customer 1 joins: 0 people ahead -> ceil(0 * 30 / 2) = 0 mins
$c1Header = @{ "Authorization" = "Bearer $($customerTokens[0])" }
$entry1 = Invoke-RestMethod -Uri "$baseUrl/queues/$queueId/entries" -Method Post -Headers $c1Header
Assert-Condition ($entry1.tokenNumber -eq "T-001" -and $entry1.estimatedWaitMinutes -eq 0) "Customer 1 joined: Token T-001, initial wait = 0 mins"

# Customer 2 joins: 1 person ahead -> ceil(1 * 30 / 2) = 15 mins
$c2Header = @{ "Authorization" = "Bearer $($customerTokens[1])" }
$entry2 = Invoke-RestMethod -Uri "$baseUrl/queues/$queueId/entries" -Method Post -Headers $c2Header
Assert-Condition ($entry2.tokenNumber -eq "T-002" -and $entry2.estimatedWaitMinutes -eq 15) "Customer 2 joined: Token T-002, initial wait = 15 mins"

# Customer 3 joins: 2 people ahead -> ceil(2 * 30 / 2) = 30 mins
$c3Header = @{ "Authorization" = "Bearer $($customerTokens[2])" }
$entry3 = Invoke-RestMethod -Uri "$baseUrl/queues/$queueId/entries" -Method Post -Headers $c3Header
Assert-Condition ($entry3.tokenNumber -eq "T-003" -and $entry3.estimatedWaitMinutes -eq 30) "Customer 3 joined: Token T-003, initial wait = 30 mins"

# Customer 4 joins: 3 people ahead -> ceil(3 * 30 / 2) = 45 mins
$c4Header = @{ "Authorization" = "Bearer $($customerTokens[3])" }
$entry4 = Invoke-RestMethod -Uri "$baseUrl/queues/$queueId/entries" -Method Post -Headers $c4Header
Assert-Condition ($entry4.tokenNumber -eq "T-004" -and $entry4.estimatedWaitMinutes -eq 45) "Customer 4 joined: Token T-004, initial wait = 45 mins"

# ------------------------------------------------------------------
# Test 7: Verify Dedicated Estimation Endpoint GET /api/queue-entries/{id}/estimate
# ------------------------------------------------------------------
Write-Host "`n--- Step 7: Test GET /api/queue-entries/{id}/estimate Endpoint ---" -ForegroundColor Yellow
$estResponse = Invoke-RestMethod -Uri "$baseUrl/queue-entries/$($entry4.id)/estimate" -Method Get -Headers $c4Header
Assert-Condition ($estResponse.queueEntryId -eq $entry4.id) "Estimate endpoint returned matching entry ID: $($entry4.id)"
Assert-Condition ($estResponse.tokenNumber -eq "T-004") "Estimate endpoint returned token T-004"
Assert-Condition ($estResponse.position -eq 4) "Estimate endpoint returned position = 4"
Assert-Condition ($estResponse.peopleAhead -eq 3) "Estimate endpoint returned peopleAhead = 3"
Assert-Condition ($estResponse.activeCounters -eq 2) "Estimate endpoint returned activeCounters = 2"
Assert-Condition ($estResponse.serviceDurationMinutes -eq 30) "Estimate endpoint returned duration = 30 mins"
Assert-Condition ($estResponse.estimatedWaitMinutes -eq 45) "Estimate endpoint returned estimatedWaitMinutes = 45"
Assert-Condition ($estResponse.estimationSource -eq "BASELINE") "Estimate endpoint returned estimationSource = BASELINE"
Assert-Condition ($estResponse.estimatedServiceTime -ne $null) "Estimate endpoint returned valid estimatedServiceTime"
Assert-Condition ($estResponse.explanation -ne $null) "Estimate endpoint returned explanation: $($estResponse.explanation)"

# ------------------------------------------------------------------
# Test 8: Verify Customer 4's GET /entries/my shows initial and dynamic estimates
# ------------------------------------------------------------------
Write-Host "`n--- Step 8: Verify Customer 4 Dashboard Position & Wait Time ---" -ForegroundColor Yellow
$pos4 = Invoke-RestMethod -Uri "$baseUrl/queues/$queueId/entries/my" -Method Get -Headers $c4Header
Assert-Condition ($pos4.initialEstimatedWaitMinutes -eq 45) "Initial wait recorded at join time: 45 mins"
Assert-Condition ($pos4.currentEstimatedWaitMinutes -eq 45) "Current dynamic wait time: 45 mins"
Assert-Condition ($pos4.peopleAhead -eq 3) "Customer 4 has 3 people ahead"

# ------------------------------------------------------------------
# Test 9: Queue Movement & Dynamic Recalculation (Staff calls next customer)
# ------------------------------------------------------------------
Write-Host "`n--- Step 9: Staff calls Customer 1 -> Check Dynamic Recalculation for Customer 4 ---" -ForegroundColor Yellow
$called1 = Invoke-RestMethod -Uri "$baseUrl/queues/$queueId/entries/call-next" -Method Put -Headers $authHeader
Assert-Condition ($called1.tokenNumber -eq "T-001" -and $called1.status -eq "CALLED") "Staff called T-001"

# Customer 4 checks position: now only 2 customers waiting ahead (Customer 2 and 3)
# ceil(2 * 30 / 2) = 30 mins
$pos4After1 = Invoke-RestMethod -Uri "$baseUrl/queues/$queueId/entries/my" -Method Get -Headers $c4Header
Assert-Condition ($pos4After1.peopleAhead -eq 2) "People ahead reduced from 3 to 2"
Assert-Condition ($pos4After1.initialEstimatedWaitMinutes -eq 45) "Initial wait preserved as 45 mins"
Assert-Condition ($pos4After1.currentEstimatedWaitMinutes -eq 30) "Current dynamic wait dynamically updated to 30 mins!"

# Check dedicated endpoint as well
$estAfter1 = Invoke-RestMethod -Uri "$baseUrl/queue-entries/$($entry4.id)/estimate" -Method Get -Headers $c4Header
Assert-Condition ($estAfter1.estimatedWaitMinutes -eq 30) "Dedicated /estimate dynamically updated to 30 mins"

# ------------------------------------------------------------------
# Test 10: Staff calls Customer 2 -> Dynamic wait drops to 15 mins
# ------------------------------------------------------------------
Write-Host "`n--- Step 10: Staff calls Customer 2 -> Dynamic Wait Drops to 15 mins ---" -ForegroundColor Yellow
$called2 = Invoke-RestMethod -Uri "$baseUrl/queues/$queueId/entries/call-next" -Method Put -Headers $authHeader
Assert-Condition ($called2.tokenNumber -eq "T-002" -and $called2.status -eq "CALLED") "Staff called T-002"

$pos4After2 = Invoke-RestMethod -Uri "$baseUrl/queues/$queueId/entries/my" -Method Get -Headers $c4Header
Assert-Condition ($pos4After2.peopleAhead -eq 1) "People ahead reduced to 1"
Assert-Condition ($pos4After2.currentEstimatedWaitMinutes -eq 15) "Current dynamic wait dynamically updated to 15 mins!"

# ------------------------------------------------------------------
# Test 11: Staff calls Customer 3 -> Customer 4 is next (peopleAhead = 0)
# ------------------------------------------------------------------
Write-Host "`n--- Step 11: Staff calls Customer 3 -> Customer 4 is Next in Line ---" -ForegroundColor Yellow
$called3 = Invoke-RestMethod -Uri "$baseUrl/queues/$queueId/entries/call-next" -Method Put -Headers $authHeader
Assert-Condition ($called3.tokenNumber -eq "T-003" -and $called3.status -eq "CALLED") "Staff called T-003"

$pos4After3 = Invoke-RestMethod -Uri "$baseUrl/queues/$queueId/entries/my" -Method Get -Headers $c4Header
Assert-Condition ($pos4After3.peopleAhead -eq 0) "People ahead reduced to 0"
Assert-Condition ($pos4After3.currentEstimatedWaitMinutes -eq 0) "Current dynamic wait is 0 mins (Customer 4 is next!)"

# ------------------------------------------------------------------
# Test 12: Staff calls Customer 4 -> Status becomes CALLED (wait time = 0)
# ------------------------------------------------------------------
Write-Host "`n--- Step 12: Staff calls Customer 4 ---" -ForegroundColor Yellow
$called4 = Invoke-RestMethod -Uri "$baseUrl/queues/$queueId/entries/call-next" -Method Put -Headers $authHeader
Assert-Condition ($called4.tokenNumber -eq "T-004" -and $called4.status -eq "CALLED") "Staff called T-004"

$estCalled4 = Invoke-RestMethod -Uri "$baseUrl/queue-entries/$($entry4.id)/estimate" -Method Get -Headers $c4Header
Assert-Condition ($estCalled4.estimatedWaitMinutes -eq 0) "CALLED entry has 0 wait time"
Assert-Condition ($estCalled4.status -eq "CALLED") "Status is CALLED"

# ------------------------------------------------------------------
# Test 13: Service lifecycle completion -> Terminal status check
# ------------------------------------------------------------------
Write-Host "`n--- Step 13: Complete Service for Customer 4 ---" -ForegroundColor Yellow
$inService = Invoke-RestMethod -Uri "$baseUrl/queue-entries/$($entry4.id)/start-service" -Method Put -Headers $authHeader
Assert-Condition ($inService.status -eq "IN_SERVICE") "Entry transitioned to IN_SERVICE"

$completed = Invoke-RestMethod -Uri "$baseUrl/queue-entries/$($entry4.id)/complete" -Method Put -Headers $authHeader
Assert-Condition ($completed.status -eq "COMPLETED") "Entry transitioned to COMPLETED"

$estCompleted4 = Invoke-RestMethod -Uri "$baseUrl/queue-entries/$($entry4.id)/estimate" -Method Get -Headers $c4Header
Assert-Condition ($estCompleted4.estimatedWaitMinutes -eq 0) "COMPLETED entry has 0 wait time"
Assert-Condition ($estCompleted4.status -eq "COMPLETED") "Status is COMPLETED"

# ------------------------------------------------------------------
# Test 14: Security Enforcement - Unauthenticated request to /estimate must return 401
# ------------------------------------------------------------------
Write-Host "`n--- Step 14: Security Check (Unauthenticated request to /estimate) ---" -ForegroundColor Yellow
try {
    $unauth = Invoke-RestMethod -Uri "$baseUrl/queue-entries/$($entry4.id)/estimate" -Method Get
    Assert-Condition $false "Unauthenticated request should have failed"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Assert-Condition ($statusCode -eq 401) "Unauthenticated request returned 401 Unauthorized"
}

# ------------------------------------------------------------------
# Summary
# ------------------------------------------------------------------
Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  Phase 6 Test Results Summary" -ForegroundColor Cyan
Write-Host "  PASSED: $passCount" -ForegroundColor Green
Write-Host "  FAILED: $failCount" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Red" })
Write-Host "==========================================================" -ForegroundColor Cyan

if ($failCount -gt 0) {
    exit 1
}
