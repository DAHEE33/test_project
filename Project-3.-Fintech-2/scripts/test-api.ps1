# EasyPay API 테스트 스크립트
# PowerShell에서 실행: .\test-api.ps1

$baseUrl = "http://localhost:8081"
$token = ""

Write-Host "=== EasyPay API 테스트 ===" -ForegroundColor Green

# 1. 회원가입
Write-Host "`n1. 회원가입 테스트..." -ForegroundColor Yellow
$registerBody = @{
    phoneNumber = "010-1234-5678"
    password = "password123"
    name = "홍길동"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method POST -Body $registerBody -ContentType "application/json"
    Write-Host "회원가입 성공!" -ForegroundColor Green
    Write-Host "계좌번호: $($response.accountNumber)" -ForegroundColor Cyan
    $token = $response.accessToken
} catch {
    Write-Host "회원가입 실패 또는 이미 존재하는 계정" -ForegroundColor Red
    Write-Host $_.Exception.Message
}

# 2. 로그인
Write-Host "`n2. 로그인 테스트..." -ForegroundColor Yellow
$loginBody = @{
    phoneNumber = "010-1234-5678"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    Write-Host "로그인 성공!" -ForegroundColor Green
    $token = $response.accessToken
} catch {
    Write-Host "로그인 실패" -ForegroundColor Red
    Write-Host $_.Exception.Message
    exit
}

# 3. 잔액 조회
Write-Host "`n3. 잔액 조회 테스트..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/accounts/VA12345678/balance" -Method GET -Headers @{"Authorization" = "Bearer $token"}
    Write-Host "잔액 조회 성공!" -ForegroundColor Green
    Write-Host "계좌번호: $($response.accountNumber)" -ForegroundColor Cyan
    Write-Host "잔액: $($response.balance)원" -ForegroundColor Cyan
} catch {
    Write-Host "잔액 조회 실패" -ForegroundColor Red
    Write-Host $_.Exception.Message
}

# 4. 테스트 입금
Write-Host "`n4. 테스트 입금..." -ForegroundColor Yellow
$depositBody = @{
    accountNumber = "VA12345678"
    amount = 50000
    transactionType = "DEPOSIT"
    description = "테스트 입금"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/accounts/update-balance" -Method POST -Body $depositBody -ContentType "application/json" -Headers @{"Authorization" = "Bearer $token"}
    Write-Host "입금 성공!" -ForegroundColor Green
    Write-Host "변경 전: $($response.balanceBefore)원" -ForegroundColor Cyan
    Write-Host "변경 후: $($response.balanceAfter)원" -ForegroundColor Cyan
} catch {
    Write-Host "입금 실패" -ForegroundColor Red
    Write-Host $_.Exception.Message
}

# 5. 테스트 출금
Write-Host "`n5. 테스트 출금..." -ForegroundColor Yellow
$withdrawBody = @{
    accountNumber = "VA12345678"
    amount = -20000
    transactionType = "WITHDRAW"
    description = "테스트 출금"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/accounts/update-balance" -Method POST -Body $withdrawBody -ContentType "application/json" -Headers @{"Authorization" = "Bearer $token"}
    Write-Host "출금 성공!" -ForegroundColor Green
    Write-Host "변경 전: $($response.balanceBefore)원" -ForegroundColor Cyan
    Write-Host "변경 후: $($response.balanceAfter)원" -ForegroundColor Cyan
} catch {
    Write-Host "출금 실패" -ForegroundColor Red
    Write-Host $_.Exception.Message
}

# 6. 거래내역 조회
Write-Host "`n6. 거래내역 조회..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/accounts/VA12345678/transactions" -Method GET -Headers @{"Authorization" = "Bearer $token"}
    Write-Host "거래내역 조회 성공!" -ForegroundColor Green
    Write-Host "거래 건수: $($response.Count)건" -ForegroundColor Cyan
    foreach ($transaction in $response) {
        Write-Host "  - $($transaction.description): $($transaction.amount)원 ($($transaction.createdAt))" -ForegroundColor White
    }
} catch {
    Write-Host "거래내역 조회 실패" -ForegroundColor Red
    Write-Host $_.Exception.Message
}

Write-Host "`n=== 테스트 완료 ===" -ForegroundColor Green 