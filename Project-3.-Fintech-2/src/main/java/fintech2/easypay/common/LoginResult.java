package fintech2.easypay.common;

public enum LoginResult {
    SUCCESS,        // 로그인 성공
    INVALID_PASSWORD, // 잘못된 비밀번호
    ACCOUNT_LOCKED,   // 계정 잠금
    ACCOUNT_NOT_FOUND, // 계정 없음
    ACCOUNT_INACTIVE  // 비활성 계정
} 