# EasyPay 개발 가이드

## 🔐 JWT 인증 사용법

### 1. 기본 개념
- **JWT (JSON Web Token)**: 사용자 인증을 위한 토큰
- **Access Token**: API 호출 시 사용 (1시간 유효)
- **Refresh Token**: Access Token 갱신용 (30일 유효)

### 2. JWT 발급 과정
```
1. 회원가입/로그인 API 호출
2. 응답에서 accessToken, refreshToken 추출
3. 이후 모든 API 호출 시 Authorization 헤더에 JWT 추가
```

### 3. Spring Boot 컨트롤러에서 사용법

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

### 4. API 테스트 방법

#### Postman 사용법
1. **회원가입/로그인**
   ```
   POST http://localhost:8081/auth/login
   Body: {"phoneNumber": "010-1234-5678", "password": "password123"}
   ```

2. **JWT 토큰 설정**
   - Postman의 Authorization 탭에서 Type을 "Bearer Token"으로 설정
   - Token 필드에 응답받은 `accessToken` 입력

3. **인증 API 호출**
   ```
   GET http://localhost:8081/accounts/info
   Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
   ```

#### PowerShell 스크립트 사용법
```bash
# JWT 인증 테스트
powershell -ExecutionPolicy Bypass -File test-jwt-api.ps1

# 기본 API 테스트
powershell -ExecutionPolicy Bypass -File test-api.ps1
```

### 5. 에러 처리

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

### 6. 개발 시 주의사항

#### ✅ 올바른 사용법
```java
// 1. @AuthenticationPrincipal 사용
@GetMapping("/api")
public ResponseEntity<?> api(@AuthenticationPrincipal UserPrincipal user) {
    Long userId = user.getId();
    // 비즈니스 로직
}

// 2. 권한 검증
if (!service.hasPermission(userId, resourceId)) {
    return ResponseEntity.status(403).body(errorResponse);
}
```

#### ❌ 잘못된 사용법
```java
// 1. JWT 토큰을 직접 파싱하지 말 것
// 2. 사용자 정보를 요청 파라미터로 받지 말 것
// 3. 권한 검증을 생략하지 말 것
```

### 7. 새로운 API 추가 시 체크리스트

- [ ] `@AuthenticationPrincipal UserPrincipal userPrincipal` 파라미터 추가
- [ ] 사용자 권한 검증 로직 구현
- [ ] API 문서에 JWT 필요 여부 명시
- [ ] 테스트 코드 작성 (JWT 토큰 포함)
- [ ] 에러 처리 구현

### 8. 디버깅 팁

#### JWT 토큰 확인
1. https://jwt.io/ 에서 토큰 디코딩
2. 토큰 만료 시간 확인
3. 토큰 내용 확인

#### 로그 확인
```bash
# 애플리케이션 로그에서 JWT 관련 메시지 확인
tail -f logs/application.log | grep JWT
```

### 9. 자주 묻는 질문

#### Q: JWT 토큰이 만료되면 어떻게 하나요?
A: Refresh Token을 사용하여 새로운 Access Token을 발급받으세요.

#### Q: 여러 API를 동시에 호출할 때 JWT는 어떻게 하나요?
A: 각 API 호출마다 동일한 JWT 토큰을 Authorization 헤더에 포함시키면 됩니다.

#### Q: JWT 토큰을 클라이언트에서 어떻게 저장하나요?
A: localStorage, sessionStorage, 또는 쿠키에 저장하되, 보안을 위해 HttpOnly 쿠키 사용을 권장합니다.

### 10. 연락처

JWT 인증 관련 문의사항이 있으시면 개발팀에 연락해주세요. 