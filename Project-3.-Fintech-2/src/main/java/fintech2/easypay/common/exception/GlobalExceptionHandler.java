package fintech2.easypay.common.exception;

import fintech2.easypay.audit.service.AlarmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final AlarmService alarmService;

    // 인증 관련 예외
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuthException(AuthException e) {
        log.error("Auth Exception: {}", e.getMessage());
        
        // 인증 실패 알람 발송
        alarmService.sendSystemAlert("AUTH", e.getMessage(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", e.getErrorCode());
        response.put("message", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // 비밀번호 불일치
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException e) {
        log.error("Bad Credentials: {}", e.getMessage());
        
        // 로그인 실패 알람 발송
        alarmService.sendSystemAlert("AUTH", "로그인 실패 - 비밀번호 불일치", e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "INVALID_CREDENTIALS");
        response.put("message", "휴대폰 번호 또는 비밀번호가 올바르지 않습니다");
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // 인증 실패
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException e) {
        log.error("Authentication Exception: {}", e.getMessage());
        
        // 인증 실패 알람 발송
        alarmService.sendSystemAlert("AUTH", "인증 실패: " + e.getMessage(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "AUTHENTICATION_FAILED");
        response.put("message", "인증에 실패했습니다");
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // 리소스 없음
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.error("No Handler Found: {} {}", e.getHttpMethod(), e.getRequestURL());
        
        // 404 에러 알람 발송
        alarmService.sendSystemAlert("SYSTEM", "404 에러: " + e.getHttpMethod() + " " + e.getRequestURL(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "NOT_FOUND");
        response.put("message", "요청한 리소스를 찾을 수 없습니다");
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 잔액 부족
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientBalanceException(InsufficientBalanceException e) {
        log.warn("Insufficient Balance: {}", e.getMessage());
        
        // 잔액 부족 경고 알람 발송
        alarmService.sendSystemAlert("ACCOUNT", "잔액 부족: " + e.getMessage(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "INSUFFICIENT_BALANCE");
        response.put("message", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 계좌 없음
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAccountNotFoundException(AccountNotFoundException e) {
        log.warn("Account Not Found: {}", e.getMessage());
        
        // 계좌 없음 경고 알람 발송
        alarmService.sendSystemAlert("ACCOUNT", "계좌 없음: " + e.getMessage(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "ACCOUNT_NOT_FOUND");
        response.put("message", e.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 일반적인 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);
        
        // 시스템 에러 알람 발송
        alarmService.sendSystemAlert("SYSTEM", "시스템 에러: " + e.getMessage(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "INTERNAL_SERVER_ERROR");
        response.put("message", "서버 내부 오류가 발생했습니다");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
} 