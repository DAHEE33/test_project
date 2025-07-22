# EasyPay API Test Script
# Run in PowerShell

$baseUrl = "http://localhost:8081"

Write-Host "=== EasyPay API Test ===" -ForegroundColor Green
Write-Host ""

# 1. Register Test
Write-Host "1. Register Test" -ForegroundColor Yellow
$registerData = @{
    phoneNumber = "010-8765-4321"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method POST -Body $registerData -ContentType "application/json"
    Write-Host "SUCCESS: Registration successful!" -ForegroundColor Green
    Write-Host "Response: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Cyan
    
    # Check if accountNumber is present
    if ($response.accountNumber) {
        Write-Host "✅ Account Number: $($response.accountNumber)" -ForegroundColor Green
    } else {
        Write-Host "❌ Account Number is missing!" -ForegroundColor Red
    }
} catch {
    Write-Host "FAILED: Registration failed - $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $errorResponse = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($errorResponse)
        $responseBody = $reader.ReadToEnd()
        Write-Host "Error Response: $responseBody" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Waiting 2 seconds before login test..." -ForegroundColor Yellow
Start-Sleep -Seconds 2

# 2. Login Test
Write-Host "2. Login Test" -ForegroundColor Yellow
$loginData = @{
    phoneNumber = "010-8765-4321"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $loginData -ContentType "application/json"
    Write-Host "SUCCESS: Login successful!" -ForegroundColor Green
    Write-Host "Response: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Cyan
} catch {
    Write-Host "FAILED: Login failed - $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $errorResponse = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($errorResponse)
        $responseBody = $reader.ReadToEnd()
        Write-Host "Error Response: $responseBody" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "3. Phone Number Check Test" -ForegroundColor Yellow

# 3. Phone Number Check Test
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/check-phone/010-9999-8888" -Method GET
    Write-Host "SUCCESS: Phone number check successful!" -ForegroundColor Green
    Write-Host "Response: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Cyan
} catch {
    Write-Host "FAILED: Phone number check failed - $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $errorResponse = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($errorResponse)
        $responseBody = $reader.ReadToEnd()
        Write-Host "Error Response: $responseBody" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== Test Complete ===" -ForegroundColor Green 