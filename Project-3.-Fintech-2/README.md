# EasyPay Fintech System

## 🚀 프로젝트 개요
Spring Boot 기반의 핀테크 시스템으로 회원가입, 로그인, 가상계좌 관리, 잔액 조회 등의 기능을 제공합니다.

## 🔐 JWT 인증 사용법

### 1. 로그인/회원가입
```bash
# 회원가입
POST /auth/register
{
  "phoneNumber": "010-1234-5678",
  "password": "password123"
}

# 로그인
POST /auth/login
{
  "phoneNumber": "010-1234-5678",
  "password": "password123"
}
```

### 2. JWT 토큰 응답
```json
{
  "success": true,
  "message": "로그인 성공",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "accountNumber": "VA1234567890"
}
```

### 3. 인증 API 호출 방법
모든 인증이 필요한 API 호출 시 아래 헤더를 추가하세요:

```bash
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 4. Spring Boot 컨트롤러에서 사용법
```java
@GetMapping("/account/balance")
public ResponseEntity<BalanceResponse> getBalance(
    @AuthenticationPrincipal UserPrincipal userPrincipal) {
    // userPrincipal에서 사용자 정보 추출
    Long userId = userPrincipal.getId();
    String phoneNumber = userPrincipal.getPhoneNumber();
    
    // 비즈니스 로직 구현
    return ResponseEntity.ok(balanceService.getBalance(userId));
}
```

## 📖 API 명세

### 인증 API (JWT 불필요)
- `POST /auth/register` - 회원가입
- `POST /auth/login` - 로그인
- `GET /auth/check-phone/{phoneNumber}` - 휴대폰 번호 체크
- `POST /auth/refresh` - 토큰 갱신
- `POST /auth/logout` - 로그아웃

### 계좌 API (JWT 필요)
- `GET /accounts/{accountId}/balance` - 잔액 조회
- `POST /accounts/{accountId}/deposit` - 입금
- `POST /accounts/{accountId}/withdraw` - 출금
- `POST /accounts/{accountId}/transfer` - 이체

## 🛠️ 개발 환경 설정

### 필수 요구사항
- Java 17+
- Gradle 8.0+

### 실행 방법
```bash
# 프로젝트 클론
git clone [repository-url]

# 프로젝트 디렉토리 이동
cd Project-3.-Fintech-2

# 애플리케이션 실행
./gradlew bootRun
```

### 접속 정보
- **애플리케이션**: http://localhost:8081
- **H2 콘솔**: http://localhost:8081/h2-console
  - JDBC URL: `jdbc:h2:mem:testdb`
  - Username: `sa`
  - Password: (비어있음)

## 🧪 테스트 방법

### 1. Postman 사용
1. 회원가입/로그인 API 호출
2. 응답에서 `accessToken` 추출
3. Postman의 Authorization 탭에서 Type을 "Bearer Token"으로 설정
4. Token 필드에 JWT 입력
5. 이후 모든 API 요청에 자동으로 헤더 추가됨

### 2. PowerShell 스크립트 사용
```bash
# 기본 API 테스트
powershell -ExecutionPolicy Bypass -File test-api.ps1

# 로그인 실패 테스트
powershell -ExecutionPolicy Bypass -File test-login-failure.ps1
```

### 3. 브라우저 테스트
- http://localhost:8081/test.html - 간단한 API 테스트 페이지

## 🔧 공통 모듈 사용법

### JWT 토큰 검증
모든 인증이 필요한 API는 자동으로 JWT 토큰을 검증합니다.

### 사용자 정보 추출
```java
@RestController
public class AccountController {
    
    @GetMapping("/account/info")
    public ResponseEntity<AccountInfo> getAccountInfo(
        @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // userPrincipal에서 사용자 정보 추출
        Long userId = userPrincipal.getId();
        String phoneNumber = userPrincipal.getPhoneNumber();
        
        // 비즈니스 로직 구현
        AccountInfo accountInfo = accountService.getAccountInfo(userId);
        return ResponseEntity.ok(accountInfo);
    }
}
```

### 예외 처리
JWT 토큰이 유효하지 않거나 만료된 경우 자동으로 401 Unauthorized 응답을 반환합니다.

## 📝 개발 가이드라인

### 1. 새로운 API 추가 시
1. 컨트롤러에 `@AuthenticationPrincipal UserPrincipal userPrincipal` 파라미터 추가
2. 비즈니스 로직만 구현 (인증/인가는 자동 처리)
3. API 문서에 JWT 필요 여부 명시

### 2. 테스트 작성 시
1. 로그인 API로 JWT 토큰 획득
2. 테스트할 API 호출 시 Authorization 헤더에 JWT 추가

### 3. 에러 처리
- 401: JWT 토큰이 유효하지 않음
- 403: 권한 부족
- 400: 잘못된 요청
- 500: 서버 내부 오류

## 🚨 주의사항

1. **JWT 토큰 보안**: 토큰을 안전하게 보관하고, 클라이언트 사이드에서만 사용
2. **토큰 만료**: Access Token은 1시간, Refresh Token은 30일 후 만료
3. **HTTPS 사용**: 프로덕션 환경에서는 반드시 HTTPS 사용
4. **토큰 갱신**: Access Token 만료 시 Refresh Token으로 갱신

## 📞 문의사항

JWT 인증 관련 문의사항이 있으시면 개발팀에 연락해주세요. 