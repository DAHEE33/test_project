# 🏦 EasyPay - 금융 서비스 API

안전하고 편리한 가상계좌 기반 금융 서비스 플랫폼

## 📋 목차
- [구현 기능](#-구현-기능)
- [기술 스택](#-기술-스택)
- [프로젝트 구조](#-프로젝트-구조)
- [API 가이드](#-api-가이드)
- [JWT 인증 시스템](#-jwt-인증-시스템)
- [잔액 처리 아키텍처](#-잔액-처리-아키텍처)
- [알람 시스템](#-알람-시스템)
- [테스트 가이드](#-테스트-가이드)
- [개발 가이드](#-개발-가이드)

## 🚀 구현 기능

### 1. 회원가입/인증 시스템
- **회원가입**: 휴대폰 번호 중복 체크, 비밀번호 암호화(BCrypt), 가상계좌 자동 생성
- **로그인**: JWT 토큰 발급, 5회 실패 시 30분 계정 잠금
- **보안**: JWT 기반 인증, 로그인 이력 기록, 계정 잠금 관리
- **토큰 관리**: Access Token (1시간), Refresh Token (30일), 자동 갱신

### 2. 가상계좌 및 잔액 관리
- **가상계좌 생성**: "VA" + 8자리 숫자 + 2자리 체크섬 형태
- **잔액 조회**: JWT 인증 기반 본인 계좌 조회
- **잔액 증감**: Pessimistic Lock, 잔액 부족 검증, 거래내역 자동 기록
- **거래내역**: 모든 거래 추적 및 조회 가능

### 3. 알람/감사 로그 시스템
- **감사 로그**: 모든 중요 비즈니스 이벤트 기록
- **자동 알람**: 에러/경고/중요 이벤트 시 실시간 알림
- **사용자/관리자 구분**: 거래내역(사용자) vs 시스템에러(관리자)
- **확장성**: SMTP, Slack 연동 준비 완료

### 4. 프론트엔드 (4페이지)
- **로그인 페이지**: 기본 접속 페이지
- **회원가입 페이지**: 완료 시 팝업 및 자동 이동
- **메인 페이지**: 송금/결제/잔액조회 버튼, 동적 알림 개수
- **잔액조회 페이지**: 현재 잔액, 테스트 입출금, 거래내역

## 🛠 기술 스택

### Backend
- **Framework**: Spring Boot 3.5.3
- **Language**: Java 21
- **Database**: H2 (인메모리, 개발용)
- **ORM**: JPA/Hibernate 6.6.18
- **Security**: Spring Security + JWT (JJWT 0.12.5)
- **Build Tool**: Gradle 8.x

### 주요 라이브러리
- **JWT**: io.jsonwebtoken:jjwt-api:0.12.5
- **Password Encoding**: BCrypt
- **Database Migration**: Flyway
- **Testing**: JUnit 5 + Mockito

### Frontend
- **HTML5, CSS3, JavaScript (Vanilla)**
- **반응형 디자인**
- **JWT 토큰 기반 인증**

## 📁 프로젝트 구조

```
src/main/java/fintech2/easypay/
├── auth/                    # 인증 관련
│   ├── entity/             # User, RefreshToken, LoginHistory
│   ├── service/            # AuthService, TokenService, JwtService, LoginHistoryService
│   ├── repository/         # UserRepository, RefreshTokenRepository, LoginHistoryRepository
│   ├── dto/                # RegisterRequest, LoginRequest, AuthResponse
│   ├── controller/         # AuthController
│   └── filter/             # JwtAuthenticationFilter
├── account/                # 계좌 관련
│   ├── entity/             # VirtualAccount, AccountBalance, TransactionHistory
│   ├── service/            # AccountService, BalanceService, TransferService
│   ├── repository/         # VirtualAccountRepository, AccountBalanceRepository, TransactionHistoryRepository
│   ├── controller/         # AccountController, TransferController
│   └── dto/                # AccountInfoResponse, BalanceResponse, TransactionResponse
├── audit/                  # 감사 관련
│   ├── entity/             # AuditLog
│   ├── service/            # AuditLogService, AlarmService
│   ├── repository/         # AuditLogRepository
│   └── controller/         # AlarmController
├── common/                 # 공통
│   ├── exception/          # GlobalExceptionHandler, AuthException, AccountNotFoundException
│   └── enums/              # UserStatus, AccountStatus, TransactionStatus, TransactionType
└── config/                 # 설정
    ├── SecurityConfig      # Spring Security 설정
    └── SchedulingConfig    # 스케줄링 설정

src/main/resources/static/  # 프론트엔드
├── index.html             # 로그인 페이지 (기본)
├── register.html          # 회원가입 페이지
├── main.html             # 메인 페이지 (송금/결제/잔액 버튼)
├── balance.html          # 잔액조회 페이지
├── alarm.html            # 알람 페이지
├── js/
│   ├── auth.js           # 인증 관련 JS
│   ├── main.js           # 메인 페이지 JS
│   ├── balance.js        # 잔액조회 JS
│   ├── alarm.js          # 알람 JS
│   └── api.js            # API 공통 JS
└── css/
    ├── common.css        # 공통 스타일
    └── login.css         # 로그인 스타일
```

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
    "refreshToken": "uuid-refresh-token",
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
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "uuid-refresh-token"
}
```

#### 토큰 갱신
```http
POST /auth/refresh
Content-Type: application/json

{
    "refreshToken": "uuid-refresh-token"
}
```

#### 로그아웃
```http
POST /auth/logout
Authorization: Bearer {accessToken}
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

### 알람 API

#### 알림 개수 조회
```http
GET /api/alarms/count
Authorization: Bearer {accessToken}
```

**응답 (성공 200)**
```json
{
    "count": 3,
    "success": true
}
```

## 🔐 JWT 인증 시스템

### JWT 토큰 구조
- **Access Token**: 1시간 유효 (API 호출용)
- **Refresh Token**: 30일 유효 (토큰 갱신용, DB 저장)

### JWT 토큰 재발급 프로세스

```
┌─────────────┐   1. 로그인 요청    ┌─────────────┐
│   Client    │ ──────────────────► │    Server   │
│             │                     │             │
│             │ ◄────────────────── │             │
│             │   Access + Refresh  │             │
└─────────────┘       Tokens        └─────────────┘
         │                                   │
         │ 2. API 호출 (Access Token)        │
         │ ──────────────────────────────────┼─►
         │                                   │
         │ 3. Access Token 만료 (401)        │
         │ ◄─────────────────────────────────┼─
         │                                   │
         │ 4. Refresh Token으로 갱신 요청    │
         │ ──────────────────────────────────┼─►
         │                                   │
         │ 5. 새로운 Access Token 발급       │
         │ ◄─────────────────────────────────┼─
         │                                   │
         │ 6. API 재호출 (새 Access Token)   │
         │ ──────────────────────────────────┼─►
```

### 보안 특징

#### Access Token
- ✅ 1시간 만료 (짧은 수명)
- ✅ 메모리 저장 (클라이언트)
- ✅ API 호출 시마다 검증

#### Refresh Token
- ✅ 30일 만료 (긴 수명)
- ✅ DB 저장 (서버)
- ✅ UUID 기반 (예측 불가능)
- ✅ 폐기 가능 (로그아웃 시)

### 컨트롤러에서 사용법

```java
@RestController
public class MyController {
    
    @GetMapping("/my-api")
    public ResponseEntity<MyResponse> myApi(
        @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // 사용자 정보 추출
        Long userId = userPrincipal.getId();
        String phoneNumber = userPrincipal.getPhoneNumber();
        String accountNumber = userPrincipal.getAccountNumber();
        
        // 비즈니스 로직 구현
        MyResponse response = myService.doSomething(userId);
        return ResponseEntity.ok(response);
    }
}
```

### 에러 처리

#### HTTP 상태 코드
- **401 Unauthorized**: JWT 토큰이 없거나 유효하지 않음
- **403 Forbidden**: 권한 부족
- **400 Bad Request**: 잘못된 요청
- **500 Internal Server Error**: 서버 내부 오류

#### JWT 관련 에러
```json
{
  "success": false,
  "message": "JWT 토큰이 유효하지 않습니다",
  "error": "INVALID_TOKEN"
}
```

## 🏦 잔액 처리 아키텍처

### 중앙화된 잔액 서비스 (BalanceService)

```java
@Service
public class BalanceService {
    
    @Transactional
    public BalanceChangeResult increase(String accountNumber, BigDecimal amount, 
                                      TransactionType transactionType, String description, String referenceId) {
        // 입금 처리
    }
    
    @Transactional
    public BalanceChangeResult decrease(String accountNumber, BigDecimal amount, 
                                      TransactionType transactionType, String description, String referenceId) {
        // 출금 처리
    }
    
    private BalanceChangeResult changeBalance(...) {
        // 1. Pessimistic Lock으로 동시성 제어
        Optional<AccountBalance> accountOpt = accountBalanceRepository.findByIdWithLock(accountNumber);
        
        // 2. 잔액 검증
        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientBalanceException(...);
        }
        
        // 3. 잔액 업데이트 (Lock으로 보호됨)
        account.setBalance(balanceAfter);
        accountBalanceRepository.save(account);
        
        // 4. 거래내역 기록
        transactionHistoryRepository.save(transaction);
        
        // 5. 알람 발송 (트랜잭션 외부)
        alarmService.sendBalanceChangeAlert(...);
    }
}
```

### 송금/결제 담당자 사용법

```java
@Service
public class TransferService {
    
    @Transactional
    public Map<String, Object> transfer(String fromAccountNumber, String toAccountNumber, 
                                      BigDecimal amount, String description, String userId) {
        
        // 1. 송금 검증
        validateTransferRequest(...);
        
        // 2. 출금 처리 (BalanceService 호출)
        BalanceService.BalanceChangeResult withdrawResult = balanceService.decrease(
            fromAccountNumber, amount, TransactionType.TRANSFER, description, transferId);
        
        // 3. 입금 처리 (BalanceService 호출)
        BalanceService.BalanceChangeResult depositResult = balanceService.increase(
            toAccountNumber, amount, TransactionType.TRANSFER, description, transferId);
        
        // 4. 송금 완료 처리
        return createTransferResponse(...);
    }
}
```

### 동시성 제어 강화

```java
@Repository
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, String> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ab FROM AccountBalance ab WHERE ab.accountNumber = :accountNumber")
    Optional<AccountBalance> findByIdWithLock(@Param("accountNumber") String accountNumber);
}
```

### 개선 효과

1. **동시성 제어 강화**: Pessimistic Lock으로 Race Condition 방지
2. **책임 분리**: 잔액 변경의 중앙 제어
3. **장애 복구 강화**: 중앙화된 로깅과 참조 ID
4. **확장성 향상**: 새로운 비즈니스 로직 추가 용이

## 🔔 알람 시스템

### 사용자 vs 관리자 알림 구분

#### 사용자 알림 (거래내역, 잔액 변동 등)
```java
// 잔액 변동 알림
alarmService.sendBalanceChangeAlert(accountNumber, userId, "입금", "50000", "150000");

// 잔액 부족 알림
alarmService.sendInsufficientBalanceAlert(accountNumber, userId, "30000", "100000");

// 계정 잠금 알림
alarmService.sendAccountLockAlert(phoneNumber, userId, "5회 연속 로그인 실패");
```

#### 관리자 알림 (시스템 에러, 보안 이슈 등)
```java
// 시스템 에러 알림
alarmService.sendSystemAlert("DATABASE", "데이터베이스 연결 오류", exception);

// 보안 이슈 알림 (자동)
// - 계정 잠금 시 관리자에게 보안 이슈 알림
// - 로그인 실패 시 관리자에게 보안 이슈 알림
```

### 알림 유형별 처리

#### 사용자 알림 유형
- **BALANCE_CHANGE**: 잔액 변동 알림
- **INSUFFICIENT_BALANCE**: 잔액 부족 알림
- **ACCOUNT_LOCK**: 계정 잠금 알림
- **LOGIN_FAILURE**: 로그인 실패 알림

#### 관리자 알림 유형
- **SYSTEM_ERROR**: 시스템 에러 알림
- **SECURITY_ISSUE**: 보안 이슈 알림
- **DATABASE_ERROR**: 데이터베이스 에러 알림
- **NETWORK_ERROR**: 네트워크 에러 알림

### 알림 발송 조건
- **잔액 부족**: `InsufficientBalanceException` 발생 시
- **계좌 없음**: `AccountNotFoundException` 발생 시
- **인증 실패**: `AuthException`, `BadCredentialsException` 발생 시
- **시스템 에러**: 모든 `Exception` 발생 시
- **404 에러**: `NoHandlerFoundException` 발생 시

## 🧪 테스트 가이드

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

# JWT 인증 테스트
.\scripts\test-jwt-api.ps1

# 토큰 만료 테스트
.\scripts\test-token-expiration-simple.ps1
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

# 4. 알림 개수 조회
curl -X GET http://localhost:8081/api/alarms/count \
  -H "Authorization: Bearer TOKEN"

# 5. 테스트 입금
curl -X POST http://localhost:8081/accounts/update-balance \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d "{\"accountNumber\":\"VA12345678\",\"amount\":50000,\"transactionType\":\"DEPOSIT\",\"description\":\"테스트 입금\"}"

# 6. 테스트 출금
curl -X POST http://localhost:8081/accounts/update-balance \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d "{\"accountNumber\":\"VA12345678\",\"amount\":-20000,\"transactionType\":\"WITHDRAW\",\"description\":\"테스트 출금\"}"

# 7. 거래내역 조회
curl -X GET http://localhost:8081/accounts/VA12345678/transactions \
  -H "Authorization: Bearer TOKEN"
```

### 5. 단위 테스트 실행
```bash
# 전체 테스트
./gradlew test

# 특정 테스트
./gradlew test --tests BalanceServiceTest
./gradlew test --tests AuthServiceTest
./gradlew test --tests TokenServiceTest
```

### 6. JWT 토큰 만료 테스트
```bash
# 토큰 만료 설정 (1분)
# src/main/resources/application.properties 수정
jwt.expiration.access=60000      # 1분
jwt.expiration.refresh=300000    # 5분

# 테스트 실행
powershell -ExecutionPolicy Bypass -File test-token-expiration-simple.ps1

# 설정 복원
powershell -ExecutionPolicy Bypass -File restore-token-settings.ps1
```

## 💻 개발 가이드

### JWT 인증 사용법

#### 기본 사용법
```java
@RestController
public class MyController {
    
    @GetMapping("/my-api")
    public ResponseEntity<MyResponse> myApi(
        @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // 사용자 정보 추출
        Long userId = userPrincipal.getId();
        String phoneNumber = userPrincipal.getPhoneNumber();
        String accountNumber = userPrincipal.getAccountNumber();
        
        // 비즈니스 로직 구현
        MyResponse response = myService.doSomething(userId);
        return ResponseEntity.ok(response);
    }
}
```

#### 권한 검증 예시
```java
@PostMapping("/accounts/{accountId}/withdraw")
public ResponseEntity<WithdrawResponse> withdraw(
    @PathVariable Long accountId,
    @RequestBody WithdrawRequest request,
    @AuthenticationPrincipal UserPrincipal userPrincipal) {
    
    Long userId = userPrincipal.getId();
    
    // 계좌 소유자 검증
    if (!accountService.isAccountOwner(accountId, userId)) {
        return ResponseEntity.status(403).body(
            WithdrawResponse.builder()
                .success(false)
                .message("해당 계좌에 대한 접근 권한이 없습니다.")
                .build()
        );
    }
    
    // 비즈니스 로직 실행
    WithdrawResponse result = accountService.withdraw(accountId, request.getAmount());
    return ResponseEntity.ok(result);
}
```

### 새로운 API 추가 시 체크리스트

- [ ] `@AuthenticationPrincipal UserPrincipal userPrincipal` 파라미터 추가
- [ ] 사용자 권한 검증 로직 구현
- [ ] API 문서에 JWT 필요 여부 명시
- [ ] 테스트 코드 작성 (JWT 토큰 포함)
- [ ] 에러 처리 구현

### BalanceService 사용법 (송금/결제 담당자)

```java
@Service
public class TransferService {
    
    private final BalanceService balanceService;
    
    @Transactional
    public void transfer(String fromAccount, String toAccount, BigDecimal amount) {
        // 1. 잔액 확인
        if (!balanceService.hasSufficientBalance(fromAccount, amount)) {
            throw new InsufficientBalanceException("잔액 부족");
        }
        
        // 2. 출금 처리
        balanceService.decrease(fromAccount, amount, TransactionType.TRANSFER, "송금 출금", transferId);
        
        // 3. 입금 처리
        balanceService.increase(toAccount, amount, TransactionType.TRANSFER, "송금 입금", transferId);
    }
}
```

### 알림 시스템 사용법

#### 사용자 알림 발송
```java
// 잔액 변동 알림
alarmService.sendBalanceChangeAlert(accountNumber, userId, "입금", "50000", "150000");

// 잔액 부족 알림
alarmService.sendInsufficientBalanceAlert(accountNumber, userId, "30000", "100000");
```

#### 관리자 알림 발송
```java
// 시스템 에러 알림
alarmService.sendSystemAlert("DATABASE", "데이터베이스 연결 오류", exception);

// 보안 이슈 알림
alarmService.sendAdminNotification("SECURITY_ISSUE", "계정 잠금: " + phoneNumber, null);
```

## 🔐 보안 고려사항

- JWT 토큰 기반 인증
- BCrypt 비밀번호 암호화
- 계정 잠금 (5회 실패 시 30분)
- 로그인 이력 추적
- 모든 중요 액션 감사로그 기록
- CORS 설정
- XSS/CSRF 방어
- Pessimistic Lock으로 동시성 제어

## 📊 현재 실행 상태

### 애플리케이션 상태
- ✅ **실행 중**: 포트 8081에서 정상 실행
- ✅ **데이터베이스**: H2 콘솔 접근 가능 (`/h2-console`)
- ✅ **API 엔드포인트**: 모든 API 구현 완료
- ✅ **프론트엔드**: 4페이지 모두 구현 완료

### 사용 가능한 API
1. `POST /auth/register` - 회원가입 ✅
2. `POST /auth/login` - 로그인 ✅
3. `POST /auth/refresh` - 토큰 갱신 ✅
4. `POST /auth/logout` - 로그아웃 ✅
5. `GET /accounts/{accountNumber}/balance` - 잔액 조회 ✅
6. `GET /accounts/{accountNumber}/transactions` - 거래내역 조회 ✅
7. `GET /api/alarms/count` - 알림 개수 조회 ✅

## 📞 문의

- 개발자: [개발자명]
- 이메일: [개발자 이메일]
- 프로젝트: EasyPay 금융 서비스 API

---

**주의**: 이 프로젝트는 개발/테스트 목적입니다. 운영 환경에서는 추가 보안 강화가 필요합니다. 