# Simple Login Failure Test
$baseUrl = "http://localhost:8081"

Write-Host "=== Simple Login Failure Test ===" -ForegroundColor Green

# 1. Register
$registerData = @{
    phoneNumber = "010-7777-8888"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method POST -Body $registerData -ContentType "application/json"
    Write-Host "Registration successful: $($response.accountNumber)" -ForegroundColor Green
} catch {
    Write-Host "Registration failed: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

Write-Host ""
Write-Host "Testing 6 login failures..." -ForegroundColor Yellow

# 2. Test 6 login failures
for ($i = 1; $i -le 6; $i++) {
    Write-Host "Attempt $i/6..." -ForegroundColor Yellow
    
    $loginData = @{
        phoneNumber = "010-7777-8888"
        password = "wrongpassword"
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $loginData -ContentType "application/json"
        Write-Host "  FAILED: Unexpected success on attempt $i" -ForegroundColor Red
    } catch {
        if ($_.Exception.Response.StatusCode -eq 401) {
            Write-Host "  SUCCESS: Expected failure on attempt $i" -ForegroundColor Green
        } else {
            Write-Host "  FAILED: Unexpected error on attempt $i - $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    
    Start-Sleep -Seconds 1
}

Write-Host ""
Write-Host "Testing correct password after 6 failures..." -ForegroundColor Yellow

# 3. Try correct password
$loginData = @{
    phoneNumber = "010-7777-8888"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "FAILED: Account should be locked but login succeeded!" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 423) {
        Write-Host "SUCCESS: Account is properly locked (423 Locked)" -ForegroundColor Green
    } else {
        Write-Host "FAILED: Unexpected error - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== Test Complete ===" -ForegroundColor Green 