# 계정 잠금 기능 테스트 스크립트
$baseUrl = "http://localhost:8081"

Write-Host "=== 계정 잠금 기능 테스트 ===" -ForegroundColor Green

# 1. 테스트용 사용자 등록
Write-Host "`n1. 테스트용 사용자 등록..." -ForegroundColor Yellow
$registerBody = @{
    phoneNumber = "01012345678"
    password = "TestPass123"
} | ConvertTo-Json

try {
    $registerResponse = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method POST -Body $registerBody -ContentType "application/json"
    Write-Host "✅ 사용자 등록 성공: $($registerResponse.message)" -ForegroundColor Green
} catch {
    Write-Host "❌ 사용자 등록 실패: $($_.Exception.Message)" -ForegroundColor Red
    exit
}

# 2. 5회 연속 로그인 실패 시뮬레이션
Write-Host "`n2. 5회 연속 로그인 실패 시뮬레이션..." -ForegroundColor Yellow

for ($i = 1; $i -le 5; $i++) {
    Write-Host "`n--- $i번째 로그인 실패 시도 ---" -ForegroundColor Cyan
    
    $loginBody = @{
        phoneNumber = "01012345678"
        password = "WrongPassword$i"
    } | ConvertTo-Json
    
    try {
        $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
        Write-Host "❌ 예상과 다름: 로그인 성공" -ForegroundColor Red
    } catch {
        $errorMessage = $_.Exception.Message
        Write-Host "✅ 로그인 실패 (예상됨): $errorMessage" -ForegroundColor Green
        
        if ($i -eq 5) {
            Write-Host "`n🎯 5회 연속 실패 완료! 계정이 잠겨야 합니다." -ForegroundColor Magenta
        }
    }
    
    Start-Sleep -Seconds 1
}

# 3. 잠긴 계정으로 로그인 시도
Write-Host "`n3. 잠긴 계정으로 로그인 시도..." -ForegroundColor Yellow

$correctLoginBody = @{
    phoneNumber = "01012345678"
    password = "TestPass123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $correctLoginBody -ContentType "application/json"
    Write-Host "❌ 계정 잠금이 작동하지 않음: 로그인 성공" -ForegroundColor Red
} catch {
    $errorMessage = $_.Exception.Message
    Write-Host "✅ 계정 잠금 작동: $errorMessage" -ForegroundColor Green
}

# 4. H2 콘솔에서 사용자 상태 확인 안내
Write-Host "`n4. H2 콘솔에서 사용자 상태 확인:" -ForegroundColor Yellow
Write-Host "   URL: http://localhost:8081/h2-console" -ForegroundColor Cyan
Write-Host "   JDBC URL: jdbc:h2:mem:testdb" -ForegroundColor Cyan
Write-Host "   Username: sa" -ForegroundColor Cyan
Write-Host "   Password: (비워두기)" -ForegroundColor Cyan
Write-Host "`n   SQL 쿼리:" -ForegroundColor Cyan
Write-Host "   SELECT id, phone_number, login_fail_count, is_locked, lock_reason, lock_expires_at FROM users WHERE phone_number = '01012345678';" -ForegroundColor White

Write-Host "`n=== 테스트 완료 ===" -ForegroundColor Green 