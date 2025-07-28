package fintech2.easypay.account.controller;

import fintech2.easypay.account.service.AccountService;
import fintech2.easypay.auth.dto.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<?> getBalance(@PathVariable String accountNumber, @RequestHeader("Authorization") String token) {
        return accountService.getBalance(accountNumber, token);
    }

    @PostMapping("/update-balance")
    public ResponseEntity<?> updateBalance(@RequestBody Map<String, Object> request, 
                                         @AuthenticationPrincipal UserPrincipal userPrincipal) {
        String accountNumber = (String) request.get("accountNumber");
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        String transactionType = (String) request.get("transactionType");
        String description = (String) request.get("description");
        
        String userId = userPrincipal != null ? userPrincipal.getId().toString() : "USER";
        
        return accountService.updateBalance(accountNumber, amount, transactionType, description, userId);
    }

    @GetMapping("/{accountNumber}/transactions")
    public ResponseEntity<?> getTransactionHistory(@PathVariable String accountNumber, @RequestHeader("Authorization") String token) {
        return accountService.getTransactionHistory(accountNumber);
    }
} 