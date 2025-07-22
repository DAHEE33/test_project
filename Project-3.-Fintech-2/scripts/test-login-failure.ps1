# Login Failure Test Script
# Test account lock after 5 consecutive failures

$baseUrl = "http://localhost:8081"

Write-Host "=== Login Failure Test (Account Lock) ===" -ForegroundColor Green
Write-Host ""

# 1. Register a new user
Write-Host "1. Registering new user..." -ForegroundColor Yellow
$registerData = @{
    phoneNumber = "010-3333-4444"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method POST -Body $registerData -ContentType "application/json"
    Write-Host "SUCCESS: Registration successful!" -ForegroundColor Green
    Write-Host "Account Number: $($response.accountNumber)" -ForegroundColor Cyan
} catch {
    Write-Host "FAILED: Registration failed - $($_.Exception.Message)" -ForegroundColor Red
    exit
}

Write-Host ""
Write-Host "2. Testing login failures (5 times)..." -ForegroundColor Yellow

# 2. Try to login with wrong password 5 times
for ($i = 1; $i -le 5; $i++) {
    Write-Host "   Attempt $i/5..." -ForegroundColor Yellow
    
    $loginData = @{
        phoneNumber = "010-3333-4444"
        password = "wrongpassword"
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $loginData -ContentType "application/json"
        Write-Host "   FAILED: Unexpected success on attempt $i" -ForegroundColor Red
    } catch {
        if ($_.Exception.Response.StatusCode -eq 401) {
            Write-Host "   SUCCESS: Expected failure on attempt $i" -ForegroundColor Green
        } else {
            Write-Host "   FAILED: Unexpected error on attempt $i - $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    Start-Sleep -Seconds 1
}

Write-Host ""
Write-Host "3. Testing account lock..." -ForegroundColor Yellow

# 3. Try to login with correct password (should be locked)
$loginData = @{
    phoneNumber = "010-3333-4444"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "FAILED: Account should be locked but login succeeded!" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 423) {
        Write-Host "SUCCESS: Account is properly locked (423 Locked)" -ForegroundColor Green
        $errorResponse = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($errorResponse)
        $responseBody = $reader.ReadToEnd()
        Write-Host "Lock message: $responseBody" -ForegroundColor Cyan
    } else {
        Write-Host "FAILED: Unexpected error - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "4. Testing phone number check..." -ForegroundColor Yellow

# 4. Test phone number check
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/check-phone/010-3333-4444" -Method GET
    Write-Host "SUCCESS: Phone number check successful" -ForegroundColor Green
    Write-Host "Response: $($response.message)" -ForegroundColor Cyan
} catch {
    Write-Host "FAILED: Phone number check failed - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Test Complete ===" -ForegroundColor Green 