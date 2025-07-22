package fintech2.easypay.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenRefreshRequest {
    
    @NotBlank(message = "Refresh 토큰은 필수입니다")
    private String refreshToken;
} 