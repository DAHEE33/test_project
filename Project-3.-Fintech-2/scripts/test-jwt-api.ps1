# JWT 인증 API 테스트 스크립트
$baseUrl = "http://localhost:8081"

Write-Host "=== JWT 인증 API 테스트 ===" -ForegroundColor Green
Write-Host ""

# 1. 회원가입
Write-Host "1. 회원가입..." -ForegroundColor Yellow
$registerData = @{
    phoneNumber = "010-9999-0000"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method POST -Body $registerData -ContentType "application/json"
    Write-Host "SUCCESS: 회원가입 완료" -ForegroundColor Green
    Write-Host "계좌번호: $($response.accountNumber)" -ForegroundColor Cyan
    Write-Host "Access Token: $($response.accessToken.Substring(0, 50))..." -ForegroundColor Cyan
    
    # JWT 토큰 저장
    $jwtToken = $response.accessToken
    $accountNumber = $response.accountNumber
} catch {
    Write-Host "FAILED: 회원가입 실패 - $($_.Exception.Message)" -ForegroundColor Red
    exit
}

Write-Host ""
Write-Host "2. JWT 토큰으로 인증 API 호출..." -ForegroundColor Yellow

# 2. JWT 토큰으로 계좌 정보 조회 (인증 필요)
$headers = @{
    "Authorization" = "Bearer $jwtToken"
    "Content-Type" = "application/json"
}

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/accounts/info" -Method GET -Headers $headers
    Write-Host "SUCCESS: 계좌 정보 조회 성공" -ForegroundColor Green
    Write-Host "휴대폰 번호: $($response.phoneNumber)" -ForegroundColor Cyan
    Write-Host "계좌번호: $($response.accountNumber)" -ForegroundColor Cyan
} catch {
    Write-Host "FAILED: 계좌 정보 조회 실패 - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "3. JWT 토큰 없이 인증 API 호출 (실패 예상)..." -ForegroundColor Yellow

# 3. JWT 토큰 없이 API 호출 (실패 예상)
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/accounts/info" -Method GET -ContentType "application/json"
    Write-Host "FAILED: JWT 없이도 API 호출 성공 (보안 문제!)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "SUCCESS: JWT 없이 API 호출 시 401 Unauthorized 반환" -ForegroundColor Green
    } else {
        Write-Host "FAILED: 예상과 다른 에러 - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "4. 잘못된 JWT 토큰으로 API 호출 (실패 예상)..." -ForegroundColor Yellow

# 4. 잘못된 JWT 토큰으로 API 호출
$wrongHeaders = @{
    "Authorization" = "Bearer invalid.jwt.token"
    "Content-Type" = "application/json"
}

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/accounts/info" -Method GET -Headers $wrongHeaders
    Write-Host "FAILED: 잘못된 JWT로도 API 호출 성공 (보안 문제!)" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "SUCCESS: 잘못된 JWT로 API 호출 시 401 Unauthorized 반환" -ForegroundColor Green
    } else {
        Write-Host "FAILED: 예상과 다른 에러 - $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "5. 토큰 갱신 테스트..." -ForegroundColor Yellow

# 5. 토큰 갱신 테스트
$refreshData = @{
    refreshToken = $response.refreshToken
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/refresh" -Method POST -Body $refreshData -ContentType "application/json"
    Write-Host "SUCCESS: 토큰 갱신 성공" -ForegroundColor Green
    Write-Host "새 Access Token: $($response.accessToken.Substring(0, 50))..." -ForegroundColor Cyan
} catch {
    Write-Host "FAILED: 토큰 갱신 실패 - $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== JWT 인증 테스트 완료 ===" -ForegroundColor Green
Write-Host ""
Write-Host "📋 JWT 사용법 요약:" -ForegroundColor Yellow
Write-Host "1. 로그인/회원가입으로 JWT 토큰 획득" -ForegroundColor White
Write-Host "2. Authorization: Bearer {JWT} 헤더 추가" -ForegroundColor White
Write-Host "3. @AuthenticationPrincipal로 사용자 정보 자동 추출" -ForegroundColor White
Write-Host "4. 토큰 만료 시 refresh API로 갱신" -ForegroundColor White 