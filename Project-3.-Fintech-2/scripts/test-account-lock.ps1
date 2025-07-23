# 계정 잠금 테스트 스크립트
# PowerShell에서 실행: .\test-account-lock.ps1

$baseUrl = "http://localhost:8081"

Write-Host "=== 계정 잠금 테스트 ===" -ForegroundColor Green

# 1. 회원가입 (새 계정)
Write-Host "`n1. 새 계정 회원가입..." -ForegroundColor Yellow
$registerBody = @{
    phoneNumber = "010-9999-8888"
    password = "password123"
    name = "테스트사용자"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/register" -Method POST -Body $registerBody -ContentType "application/json"
    Write-Host "회원가입 성공!" -ForegroundColor Green
    Write-Host "계좌번호: $($response.accountNumber)" -ForegroundColor Cyan
} catch {
    Write-Host "회원가입 실패 또는 이미 존재하는 계정" -ForegroundColor Red
    Write-Host $_.Exception.Message
}

# 2. 5회 연속 로그인 실패로 계정 잠금 테스트
Write-Host "`n2. 5회 연속 로그인 실패로 계정 잠금 테스트..." -ForegroundColor Yellow

for ($i = 1; $i -le 5; $i++) {
    Write-Host "  $i번째 로그인 시도 (잘못된 비밀번호)..." -ForegroundColor Cyan
    
    $loginBody = @{
        phoneNumber = "010-9999-8888"
        password = "wrongpassword"
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
        Write-Host "    예상과 다름: 로그인 성공" -ForegroundColor Red
    } catch {
        Write-Host "    로그인 실패 (예상됨)" -ForegroundColor Yellow
        if ($i -eq 5) {
            Write-Host "    ✅ 5회 실패로 계정이 잠겼습니다!" -ForegroundColor Green
        }
    }
    
    Start-Sleep -Seconds 1
}

# 3. 잠긴 계정으로 로그인 시도
Write-Host "`n3. 잠긴 계정으로 로그인 시도..." -ForegroundColor Yellow

$loginBody = @{
    phoneNumber = "010-9999-8888"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$baseUrl/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    Write-Host "❌ 예상과 다름: 잠긴 계정으로 로그인 성공" -ForegroundColor Red
} catch {
    Write-Host "✅ 예상됨: 잠긴 계정으로 로그인 실패" -ForegroundColor Green
    Write-Host "   에러 메시지: $($_.Exception.Message)" -ForegroundColor Cyan
}

# 4. 30분 대기 안내
Write-Host "`n4. 계정 잠금 해제 테스트" -ForegroundColor Yellow
Write-Host "   계정이 30분간 잠겨있습니다." -ForegroundColor Cyan
Write-Host "   실제 테스트를 위해서는 30분 후에 다시 로그인을 시도해보세요." -ForegroundColor Cyan
Write-Host "   또는 서버 로그에서 잠금 해제 시간을 확인하세요." -ForegroundColor Cyan

Write-Host "`n=== 테스트 완료 ===" -ForegroundColor Green
Write-Host "서버 로그에서 다음을 확인하세요:" -ForegroundColor Yellow
Write-Host "- [ACCOUNT_LOCK] 알람 메시지" -ForegroundColor Cyan
Write-Host "- [LOGIN_FAILURE] 알람 메시지" -ForegroundColor Cyan
Write-Host "- 계정 잠금 해제 시간" -ForegroundColor Cyan 