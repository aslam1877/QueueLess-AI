$ErrorActionPreference = "Stop"

$baseUrl = "http://127.0.0.1:8000"
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   QueueLess AI - Phase 7: Python ML Service Live Test" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$pass = 0
$fail = 0

function Assert-Test($condition, $msg) {
    if ($condition) {
        Write-Host "  [PASS] $msg" -ForegroundColor Green
        $script:pass++
    } else {
        Write-Host "  [FAIL] $msg" -ForegroundColor Red
        $script:fail++
    }
}

# 1. Health check
Write-Host "`n--- 1. Health Check Endpoint (/health) ---" -ForegroundColor Yellow
$health = Invoke-RestMethod -Uri "$baseUrl/health" -Method Get
Assert-Test ($health.status -eq "UP") "Health status is UP"
Assert-Test ($health.model_loaded -eq $true) "Model is loaded"
Assert-Test ($health.model_name -ne "") "Model name: $($health.model_name)"

# 2. Model info
Write-Host "`n--- 2. Model Info Endpoint (/model-info) ---" -ForegroundColor Yellow
$info = Invoke-RestMethod -Uri "$baseUrl/model-info" -Method Get
Assert-Test ($info.dataset_samples -eq 10000) "Trained on 10,000 synthetic queue records"
Assert-Test ($info.ml_metrics.mae -le 7.0) "Winning model MAE is under 7.0 minutes: $($info.ml_metrics.mae)m"
Assert-Test ($info.baseline_benchmark.mae -ge 15.0) "Deterministic baseline MAE was: $($info.baseline_benchmark.mae)m"

# 3. Standard Prediction (Peak Hour Rush)
Write-Host "`n--- 3. Standard Prediction (Peak Lunch Hour Rush) ---" -ForegroundColor Yellow
$reqBody1 = @{
    people_ahead = 6
    active_counters = 2
    service_duration_min = 25.0
    hour_of_day = 12
    day_of_week = 4
    service_category = "PROCEDURE"
} | ConvertTo-Json

$pred1 = Invoke-RestMethod -Uri "$baseUrl/predict" -Method Post -ContentType "application/json" -Body $reqBody1
Assert-Test ($pred1.baseline_wait_minutes -eq 75) "Deterministic baseline: ceil(6 * 25 / 2) = 75 mins"
Assert-Test ($pred1.predicted_wait_minutes -gt 75) "ML model accurately adjusted upward for lunch rush and procedure complexity: $($pred1.predicted_wait_minutes) mins (+$($pred1.difference_minutes)m)"
Assert-Test ($pred1.estimation_source -eq "ML_MODEL") "Estimation source is ML_MODEL"

# 4. Off-Peak Prediction (Fast Moving Morning)
Write-Host "`n--- 4. Off-Peak Prediction (Midweek Morning) ---" -ForegroundColor Yellow
$reqBody2 = @{
    people_ahead = 4
    active_counters = 2
    service_duration_min = 20.0
    hour_of_day = 10
    day_of_week = 1
    service_category = "EXPRESS_SERVICE"
} | ConvertTo-Json

$pred2 = Invoke-RestMethod -Uri "$baseUrl/predict" -Method Post -ContentType "application/json" -Body $reqBody2
Assert-Test ($pred2.baseline_wait_minutes -eq 40) "Deterministic baseline: ceil(4 * 20 / 2) = 40 mins"
Assert-Test ($pred2.predicted_wait_minutes -le 40) "ML model adjusted downward for off-peak express service: $($pred2.predicted_wait_minutes) mins"
Assert-Test ($pred2.estimation_source -eq "ML_MODEL") "Estimation source is ML_MODEL"

# 5. Next in Line (0 People Ahead)
Write-Host "`n--- 5. Immediate Service (0 People Ahead) ---" -ForegroundColor Yellow
$reqBody3 = @{
    people_ahead = 0
    active_counters = 2
    service_duration_min = 30.0
} | ConvertTo-Json

$pred3 = Invoke-RestMethod -Uri "$baseUrl/predict" -Method Post -ContentType "application/json" -Body $reqBody3
Assert-Test ($pred3.predicted_wait_minutes -eq 0) "Predicted wait is 0 mins when 0 people ahead"
Assert-Test ($pred3.baseline_wait_minutes -eq 0) "Baseline wait is 0 mins when 0 people ahead"

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "  Phase 7 Live Microservice Test Summary" -ForegroundColor Cyan
Write-Host "  PASSED: $pass" -ForegroundColor Green
Write-Host "  FAILED: $fail" -ForegroundColor $(if ($fail -eq 0) { "Green" } else { "Red" })
Write-Host "==========================================================" -ForegroundColor Cyan

if ($fail -gt 0) {
    exit 1
}
