package fintech2.easypay.common.exception;

import fintech2.easypay.auth.dto.AuthResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 인증 관련 예외 처리
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<AuthResponse> handleAuthException(AuthException e) {
        log.error("AuthException: {}", e.getMessage());
        
        AuthResponse errorResponse = AuthResponse.builder()
                .message(e.getMessage())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * 계정 잠금 예외 처리
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<AuthResponse> handleIllegalStateException(IllegalStateException e) {
        if (e.getMessage().contains("잠겨있습니다")) {
            log.error("Account locked: {}", e.getMessage());
            
            AuthResponse errorResponse = AuthResponse.builder()
                    .message(e.getMessage())
                    .build();
            
            return ResponseEntity.status(HttpStatus.LOCKED).body(errorResponse);
        }
        
        // 다른 IllegalStateException은 기본 처리
        return handleGenericException(e);
    }

    /**
     * 비즈니스 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AuthResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("IllegalArgumentException: {}", e.getMessage());
        
        AuthResponse errorResponse = AuthResponse.builder()
                .message(e.getMessage())
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 유효성 검증 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.error("Validation errors: {}", errors);
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * 일반 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthResponse> handleGenericException(Exception e) {
        log.error("Unexpected error: ", e);
        
        AuthResponse errorResponse = AuthResponse.builder()
                .message("서버 오류가 발생했습니다")
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
} 