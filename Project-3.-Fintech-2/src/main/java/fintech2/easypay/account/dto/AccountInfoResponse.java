package fintech2.easypay.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountInfoResponse {
    private boolean success;
    private String message;
    private String accountNumber;
    private String phoneNumber;
    private String status;
    private LocalDateTime createdAt;
} 