package fintech2.easypay.account.service;

import fintech2.easypay.account.entity.AccountBalance;
import fintech2.easypay.account.entity.TransactionHistory;
import fintech2.easypay.account.repository.AccountBalanceRepository;
import fintech2.easypay.account.repository.TransactionHistoryRepository;
import fintech2.easypay.audit.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountBalanceRepository accountBalanceRepository;

    @Mock
    private TransactionHistoryRepository transactionHistoryRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AccountService accountService;

    private AccountBalance testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new AccountBalance();
        testAccount.setAccountNumber("VA12345678");
        testAccount.setBalance(new BigDecimal("50000"));
    }

    @Test
    void 잔액조회_성공() {
        // Given
        when(accountBalanceRepository.findById("VA12345678")).thenReturn(Optional.of(testAccount));

        // When
        ResponseEntity<?> response = accountService.getBalance("VA12345678", "Bearer token");

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
    }

    @Test
    void 잔액조회_계좌없음() {
        // Given
        when(accountBalanceRepository.findById("VA12345678")).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> response = accountService.getBalance("VA12345678", "Bearer token");

        // Then
        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void 잔액증감_입금_성공() {
        // Given
        when(accountBalanceRepository.findById("VA12345678")).thenReturn(Optional.of(testAccount));
        when(accountBalanceRepository.save(any(AccountBalance.class))).thenReturn(testAccount);

        // When
        ResponseEntity<?> response = accountService.updateBalance(
            "VA12345678", 
            new BigDecimal("10000"), 
            "DEPOSIT", 
            "테스트 입금"
        );

        // Then
        assertEquals(200, response.getStatusCodeValue());
        verify(accountBalanceRepository).save(any(AccountBalance.class));
        verify(transactionHistoryRepository).save(any(TransactionHistory.class));
        verify(auditLogService).logSuccess(any(), eq("BALANCE_UPDATE"), eq("ACCOUNT"), eq("VA12345678"), any(), any());
    }

    @Test
    void 잔액증감_출금_잔액부족() {
        // Given
        when(accountBalanceRepository.findById("VA12345678")).thenReturn(Optional.of(testAccount));

        // When
        ResponseEntity<?> response = accountService.updateBalance(
            "VA12345678", 
            new BigDecimal("-60000"), // 잔액보다 큰 출금
            "WITHDRAW", 
            "테스트 출금"
        );

        // Then
        assertEquals(400, response.getStatusCodeValue());
        verify(auditLogService).logWarning(any(), eq("BALANCE_INSUFFICIENT"), eq("ACCOUNT"), eq("VA12345678"), any());
    }

    @Test
    void 잔액증감_출금_성공() {
        // Given
        when(accountBalanceRepository.findById("VA12345678")).thenReturn(Optional.of(testAccount));
        when(accountBalanceRepository.save(any(AccountBalance.class))).thenReturn(testAccount);

        // When
        ResponseEntity<?> response = accountService.updateBalance(
            "VA12345678", 
            new BigDecimal("-30000"), 
            "WITHDRAW", 
            "테스트 출금"
        );

        // Then
        assertEquals(200, response.getStatusCodeValue());
        verify(accountBalanceRepository).save(any(AccountBalance.class));
        verify(transactionHistoryRepository).save(any(TransactionHistory.class));
    }
} 