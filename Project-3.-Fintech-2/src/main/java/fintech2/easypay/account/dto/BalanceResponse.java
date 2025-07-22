package fintech2.easypay.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponse {
    private boolean success;
    private String message;
    private String accountNumber;
    private BigDecimal balance;
    private String currency;
    private LocalDateTime lastUpdated;
    private String status;
} 