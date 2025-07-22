package fintech2.easypay.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    
    @NotBlank(message = "휴대폰 번호는 필수입니다")
    private String phoneNumber;
    
    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
} 