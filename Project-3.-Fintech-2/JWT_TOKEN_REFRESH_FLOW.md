# JWT 토큰 재발급 프로세스 (Refresh Flow)

## 🔄 JWT Access/Refresh Token 분리 구현

### **토큰 구조**
- **Access Token**: 1시간 유효 (API 호출용)
- **Refresh Token**: 30일 유효 (토큰 갱신용, DB 저장)

### **JWT 토큰 재발급 프로세스 다이어그램**

```
┌─────────────┐   1. 로그인 요청    ┌─────────────┐
│   Client    │ ──────────────────► │    Server   │
│             │                     │             │
│             │ ◄────────────────── │             │
│             │   Access + Refresh  │             │
└─────────────┘       Tokens        └─────────────┘
         │                                   │
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

### **상세 프로세스**

#### **1. 로그인 단계**
```
Client → Server: POST /auth/login
Server → Client: {
  "accessToken": "eyJ...",
  "refreshToken": "uuid-refresh-token"
}
```

#### **2. API 호출 단계**
```
Client → Server: GET /api/resource
Header: Authorization: Bearer {accessToken}
```

#### **3. 토큰 만료 감지**
```
Server → Client: 401 Unauthorized
{
  "error": "TOKEN_EXPIRED",
  "message": "Access Token이 만료되었습니다"
}
```

#### **4. 토큰 갱신 단계**
```
Client → Server: POST /auth/refresh
Body: {
  "refreshToken": "uuid-refresh-token"
}

Server → Client: {
  "accessToken": "eyJ...",
  "message": "토큰이 갱신되었습니다"
}
```

#### **5. API 재호출**
```
Client → Server: GET /api/resource
Header: Authorization: Bearer {newAccessToken}
```

### **보안 특징**

#### **Access Token**
- ✅ 1시간 만료 (짧은 수명)
- ✅ 메모리 저장 (클라이언트)
- ✅ API 호출 시마다 검증

#### **Refresh Token**
- ✅ 30일 만료 (긴 수명)
- ✅ DB 저장 (서버)
- ✅ UUID 기반 (예측 불가능)
- ✅ 폐기 가능 (로그아웃 시)

### **구현된 API 엔드포인트**

#### **토큰 갱신**
```http
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "uuid-refresh-token"
}
```

#### **로그아웃**
```http
POST /auth/logout
Authorization: Bearer {accessToken}
```

### **에러 처리**

#### **Refresh Token 관련 에러**
- `INVALID_REFRESH_TOKEN`: 유효하지 않은 토큰
- `EXPIRED_REFRESH_TOKEN`: 만료된 토큰
- `REVOKED_REFRESH_TOKEN`: 폐기된 토큰

### **스케줄링**

#### **만료된 토큰 정리**
```java
@Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시
public void cleanupExpiredTokens() {
    refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
}
```

### **데이터베이스 스키마**

#### **refresh_tokens 테이블**
```sql
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP NULL
);
```

### **장점**

1. **보안성**: Access Token의 짧은 수명으로 탈취 위험 최소화
2. **사용자 경험**: 자동 토큰 갱신으로 끊김 없는 서비스
3. **감사 추적**: Refresh Token DB 저장으로 사용자 활동 추적
4. **폐기 가능**: 로그아웃 시 즉시 토큰 무효화
5. **스케일링**: Stateless JWT로 서버 확장 용이 