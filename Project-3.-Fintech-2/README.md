# EasyPay - 금융 서비스 API

안전하고 편리한 가상계좌 기반 금융 서비스 플랫폼

## 📋 목차
- [구현 기능](#-구현-기능)
- [기술 스택](#-기술-스택)
- [API 가이드](#-api-가이드)
- [송금/결제 담당자 가이드](#-송금결제-담당자-가이드)
- [알람 시스템](#-알람-시스템)
- [테스트 진행 방법](#-테스트-진행-방법)
- [프로젝트 구조](#-프로젝트-구조)

## 🚀 구현 기능

### 1. 회원가입/인증 시스템
- **회원가입**: 휴대폰 번호 중복 체크, 비밀번호 암호화(BCrypt), 가상계좌 자동 생성
- **로그인**: JWT 토큰 발급, 5회 실패 시 30분 계정 잠금
- **보안**: JWT 기반 인증, 로그인 이력 기록, 계정 잠금 관리

### 2. 가상계좌 및 잔액 관리
- **가상계좌 생성**: "VA" + 8자리 숫자 + 2자리 체크섬 형태
- **잔액 조회**: JWT 인증 기반 본인 계좌 조회
- **잔액 증감**: 비관적 락, 잔액 부족 검증, 거래내역 자동 기록
- **거래내역**: 모든 거래 추적 및 조회 가능

### 3. 알람/감사 로그 시스템
- **감사 로그**: 모든 중요 비즈니스 이벤트 기록
- **자동 알람**: 에러/경고/중요 이벤트 시 실시간 알림
- **확장성**: SMTP, Slack 연동 준비 완료

### 4. 프론트엔드 (4페이지)
- **로그인 페이지**: 기본 접속 페이지
- **회원가입 페이지**: 완료 시 팝업 및 자동 이동
- **메인 페이지**: 송금/결제/잔액조회 버튼, 알림 아이콘
- **잔액조회 페이지**: 현재 잔액, 테스트 입출금, 거래내역

## 🛠 기술 스택

- **Backend**: Spring Boot 3.x, Java 17+, JPA/Hibernate
- **Security**: Spring Security, JWT (JJWT 0.12.5)
- **Database**: H2 (개발), PostgreSQL/MySQL (운영 가능)
- **Frontend**: HTML5, CSS3, JavaScript (Vanilla)
- **Build**: Gradle (Kotlin DSL)

## 📡 API 가이드

### 인증 API

#### 회원가입
```http
POST /auth/register
Content-Type: application/json

{
    "phoneNumber": "010-1234-5678",
    "password": "password123",
    "name": "홍길동"
}
```

**응답 (성공 201)**
```json
{
    "message": "회원가입이 완료되었습니다",
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "accountNumber": "VA12345678"
}
```

#### 로그인
```http
POST /auth/login
Content-Type: application/json

{
    "phoneNumber": "010-1234-5678",
    "password": "password123"
}
```

**응답 (성공 200)**
```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### 계좌 API

#### 잔액 조회
```http
GET /accounts/{accountNumber}/balance
Authorization: Bearer {accessToken}
```

**응답 (성공 200)**
```json
{
    "accountNumber": "VA12345678",
    "balance": 50000.00,
    "currency": "KRW"
}
```

#### 거래내역 조회
```http
GET /accounts/{accountNumber}/transactions
Authorization: Bearer {accessToken}
```

**응답 (성공 200)**
```json
[
    {
        "id": 1,
        "accountNumber": "VA12345678",
        "transactionType": "DEPOSIT",
        "amount": 10000.00,
        "balanceBefore": 40000.00,
        "balanceAfter": 50000.00,
        "description": "테스트 입금",
        "status": "COMPLETED",
        "createdAt": "2024-01-15T10:30:00"
    }
]
```

## 💸 송금/결제 담당자 가이드

### 잔액 증감 API 호출

송금/결제 시스템에서 실제 잔액 변경을 위해 호출하는 API입니다.

```http
POST /accounts/update-balance
Content-Type: application/json
Authorization: Bearer {accessToken}

{
    "accountNumber": "VA12345678",
    "amount": 50000,
    "transactionType": "TRANSFER_OUT",
    "description": "홍길동님께 송금"
}
```

### 요청 파라미터
- `accountNumber`: 대상 계좌번호
- `amount`: 금액 (양수: 입금, 음수: 출금)
- `transactionType`: 거래 유형 (DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT, PAYMENT 등)
- `description`: 거래 설명

### 응답 처리

#### 성공 (200)
```json
{
    "accountNumber": "VA12345678",
    "balanceBefore": 100000.00,
    "balanceAfter": 50000.00,
    "transactionType": "TRANSFER_OUT",
    "amount": -50000.00,
    "message": "잔액이 성공적으로 변경되었습니다"
}
```

#### 실패 - 잔액 부족 (400)
```json
{
    "error": "INSUFFICIENT_BALANCE",
    "message": "잔액이 부족합니다"
}
```

#### 실패 - 계좌 없음 (404)
```json
{
    "error": "NOT_FOUND",
    "message": "계좌를 찾을 수 없습니다"
}
```

### 감사 로그 및 알람

모든 잔액 변경 시 자동으로 다음이 수행됩니다:

1. **감사 로그 기록**
   - 액션: `BALANCE_UPDATE`
   - 리소스: `ACCOUNT`
   - 변경 전/후 값 기록

2. **자동 알람 발송** (다음 조건)
   - 잔액 부족 시: `WARNING` 레벨 알람
   - 시스템 오류 시: `ERROR` 레벨 알람
   - 정상 처리 시: `INFO` 레벨 비즈니스 이벤트

### 거래내역 조회

송금/결제 완료 후 거래내역 확인:

```http
GET /accounts/VA12345678/transactions
Authorization: Bearer {accessToken}
```

### 에러 처리 권장사항

```javascript
try {
    const response = await fetch('/accounts/update-balance', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(requestData)
    });

    if (response.ok) {
        const result = await response.json();
        console.log('잔액 변경 성공:', result);
        // 성공 처리 로직
    } else {
        const error = await response.json();
        console.error('잔액 변경 실패:', error);
        
        switch (error.error) {
            case 'INSUFFICIENT_BALANCE':
                // 잔액 부족 처리
                break;
            case 'NOT_FOUND':
                // 계좌 없음 처리
                break;
            default:
                // 기타 오류 처리
        }
    }
} catch (error) {
    console.error('네트워크 오류:', error);
    // 네트워크 오류 처리
}
```

## 🔔 알람 시스템

### 현재 구현 (Exception 기반 자동 알람)
```java
// GlobalExceptionHandler에서 자동 알람 발송
@ExceptionHandler(InsufficientBalanceException.class)
public ResponseEntity<Map<String, Object>> handleInsufficientBalanceException(InsufficientBalanceException e) {
    // 잔액 부족 경고 알람 자동 발송
    alarmService.sendSystemAlert("ACCOUNT", "잔액 부족: " + e.getMessage(), e);
    // ...
}

@ExceptionHandler(AccountNotFoundException.class)
public ResponseEntity<Map<String, Object>> handleAccountNotFoundException(AccountNotFoundException e) {
    // 계좌 없음 경고 알람 자동 발송
    alarmService.sendSystemAlert("ACCOUNT", "계좌 없음: " + e.getMessage(), e);
    // ...
}
```

### 알람 발송 조건
- **잔액 부족**: `InsufficientBalanceException` 발생 시
- **계좌 없음**: `AccountNotFoundException` 발생 시
- **인증 실패**: `AuthException`, `BadCredentialsException` 발생 시
- **시스템 에러**: 모든 `Exception` 발생 시
- **404 에러**: `NoHandlerFoundException` 발생 시

### 비즈니스 이벤트 알람 (수동)
```java
// AuditLogService에서 중요 이벤트 알람
if (isImportantEvent(action)) {
    alarmService.sendBusinessEvent(action, userId, description);
}
```

### SMTP 연동 예시 (추후 구현)
```java
@Service
public class AlarmService {
    private final EmailService emailService;
    
    public void sendBusinessEvent(String eventType, String userId, String description) {
        // Slack 웹훅 전송
        slackClient.sendMessage(buildSlackMessage(eventType, userId, description));
        
        // 이메일 전송 (중요 이벤트)
        if (isImportantEvent(eventType)) {
            emailService.sendAlert("admin@company.com", "중요 이벤트 발생", description);
        }
    }
}
```

### Slack 연동 예시 (추후 구현)
```yaml
# application.yml
alarm:
  slack:
    webhook-url: https://hooks.slack.com/services/YOUR/WEBHOOK/URL
    channel: "#alerts"
  email:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-app-password
```

## 🧪 테스트 진행 방법

### 1. 서버 실행
```bash
./gradlew bootRun
```

### 2. 웹 브라우저 테스트
1. http://localhost:8081 접속
2. 회원가입 진행 (010-1234-5678, password123, 홍길동)
3. 로그인 진행
4. 메인 페이지에서 "잔액조회" 클릭
5. 테스트 입금/출금으로 기능 확인

### 3. PowerShell 스크립트 테스트 (권장)
```powershell
# PowerShell에서 실행
.\scripts\test-api.ps1
```

### 4. curl 테스트 (터미널/CMD)
```bash
# 1. 회원가입
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"phoneNumber\":\"010-1234-5678\",\"password\":\"password123\",\"name\":\"홍길동\"}"

# 2. 로그인 (응답에서 accessToken 추출)
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"phoneNumber\":\"010-1234-5678\",\"password\":\"password123\"}"

# 3. 잔액 조회 (TOKEN 부분을 실제 토큰으로 교체)
curl -X GET http://localhost:8081/accounts/VA12345678/balance \
  -H "Authorization: Bearer TOKEN"

# 4. 테스트 입금
curl -X POST http://localhost:8081/accounts/update-balance \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d "{\"accountNumber\":\"VA12345678\",\"amount\":50000,\"transactionType\":\"DEPOSIT\",\"description\":\"테스트 입금\"}"

# 5. 테스트 출금
curl -X POST http://localhost:8081/accounts/update-balance \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d "{\"accountNumber\":\"VA12345678\",\"amount\":-20000,\"transactionType\":\"WITHDRAW\",\"description\":\"테스트 출금\"}"

# 6. 거래내역 조회
curl -X GET http://localhost:8081/accounts/VA12345678/transactions \
  -H "Authorization: Bearer TOKEN"
```

### 5. 단위 테스트 실행
```bash
./gradlew test
```

## 📁 프로젝트 구조

```
src/main/java/fintech2/easypay/
├── auth/                    # 인증/회원가입
│   ├── controller/         # AuthController
│   ├── service/           # AuthService, JwtService, LoginHistoryService
│   ├── entity/            # User, LoginHistory
│   ├── repository/        # UserRepository, LoginHistoryRepository
│   └── dto/               # LoginRequest, RegisterRequest
├── account/                # 계좌/잔액 관리
│   ├── controller/        # AccountController
│   ├── service/          # AccountService
│   ├── entity/           # AccountBalance, TransactionHistory
│   └── repository/       # AccountBalanceRepository, TransactionHistoryRepository
├── audit/                 # 감사로그/알람
│   ├── service/          # AuditLogService, AlarmService
│   ├── entity/           # AuditLog
│   └── repository/       # AuditLogRepository
├── common/               # 공통
│   ├── exception/        # GlobalExceptionHandler, AuthException
│   └── AuditResult.java  # Enum
└── config/               # 설정
    └── SecurityConfig.java

src/main/resources/static/  # 프론트엔드
├── index.html             # 로그인 페이지 (기본)
├── register.html          # 회원가입 페이지
├── main.html             # 메인 페이지 (송금/결제/잔액 버튼)
├── balance.html          # 잔액조회 페이지
├── js/
│   ├── auth.js           # 인증 관련 JS
│   ├── main.js           # 메인 페이지 JS
│   └── balance.js        # 잔액조회 JS
└── css/
    └── common.css        # 공통 스타일
```

## 🔐 보안 고려사항

- JWT 토큰 기반 인증
- BCrypt 비밀번호 암호화
- 계정 잠금 (5회 실패 시 30분)
- 로그인 이력 추적
- 모든 중요 액션 감사로그 기록
- CORS 설정
- XSS/CSRF 방어

## 📞 문의

- 개발자: [개발자명]
- 이메일: [개발자 이메일]
- 프로젝트: EasyPay 금융 서비스 API

---

**주의**: 이 프로젝트는 개발/테스트 목적입니다. 운영 환경에서는 추가 보안 강화가 필요합니다. 