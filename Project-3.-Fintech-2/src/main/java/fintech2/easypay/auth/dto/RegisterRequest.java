package fintech2.easypay.auth.dto;
import lombok.Data;

@Data
public class RegisterRequest {
    private String phoneNumber;
    private String password;
    private String name;
} 