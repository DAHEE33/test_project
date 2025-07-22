package fintech2.easypay.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String message;
    private String accessToken;
    private String refreshToken;
    private String accountNumber;
    private String error;
} 