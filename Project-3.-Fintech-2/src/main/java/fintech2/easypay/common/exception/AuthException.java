package fintech2.easypay.common.exception;

public class AuthException extends RuntimeException {
    
    private final String errorCode;
    
    public AuthException(String message) {
        super(message);
        this.errorCode = "AUTH_ERROR";
    }
    
    public AuthException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
} 