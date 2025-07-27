package fintech2.easypay.account.service;

import fintech2.easypay.account.entity.AccountBalance;
import fintech2.easypay.account.repository.AccountBalanceRepository;
import fintech2.easypay.audit.service.AuditLogService;
import fintech2.easypay.audit.service.AlarmService;
import fintech2.easypay.common.TransactionType;
import fintech2.easypay.common.exception.AccountNotFoundException;
import fintech2.easypay.common.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private AccountBalanceRepository accountBalanceRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private AlarmService alarmService;

    @InjectMocks
    private BalanceService balanceService;

    private AccountBalance testAccount;

    @BeforeEach
    void setUp() {
        testAccount = AccountBalance.builder()
            .accountNumber("VA12345678")
            .balance(new BigDecimal("100000"))
            .version(1)
            .build();
    }

    @Test
    void 잔액증가_성공() {
        // Given
        when(accountBalanceRepository.findByIdWithLock("VA12345678"))
            .thenReturn(Optional.of(testAccount));
        when(accountBalanceRepository.save(any(AccountBalance.class)))
            .thenAnswer(invocation -> {
                AccountBalance savedAccount = invocation.getArgument(0);
                return savedAccount;
            });

        // When
        BalanceService.BalanceChangeResult result = balanceService.increase(
            "VA12345678", 
            new BigDecimal("50000"), 
            TransactionType.DEPOSIT, 
            "테스트 입금", 
            "REF001"
        );

        // Then
        assertNotNull(result);
        assertEquals("VA12345678", result.getAccountNumber());
        assertEquals(new BigDecimal("100000"), result.getBalanceBefore());
        assertEquals(new BigDecimal("150000"), result.getBalanceAfter());
        assertEquals(new BigDecimal("50000"), result.getChangeAmount());
        assertEquals(TransactionType.DEPOSIT, result.getTransactionType());
        assertEquals("REF001", result.getReferenceId());

        verify(accountBalanceRepository).findByIdWithLock("VA12345678");
        verify(accountBalanceRepository).save(any(AccountBalance.class));
        verify(auditLogService).logSuccess(eq("BALANCE_CHANGE"), eq("ACCOUNT"), eq("VA12345678"), any(), any());
    }

    @Test
    void 잔액감소_성공() {
        // Given
        when(accountBalanceRepository.findByIdWithLock("VA12345678"))
            .thenReturn(Optional.of(testAccount));
        when(accountBalanceRepository.save(any(AccountBalance.class)))
            .thenAnswer(invocation -> {
                AccountBalance savedAccount = invocation.getArgument(0);
                return savedAccount;
            });

        // When
        BalanceService.BalanceChangeResult result = balanceService.decrease(
            "VA12345678", 
            new BigDecimal("30000"), 
            TransactionType.WITHDRAWAL, 
            "테스트 출금", 
            "REF002"
        );

        // Then
        assertNotNull(result);
        assertEquals("VA12345678", result.getAccountNumber());
        assertEquals(new BigDecimal("100000"), result.getBalanceBefore());
        assertEquals(new BigDecimal("70000"), result.getBalanceAfter());
        assertEquals(new BigDecimal("-30000"), result.getChangeAmount());
        assertEquals(TransactionType.WITHDRAWAL, result.getTransactionType());
        assertEquals("REF002", result.getReferenceId());
    }

    @Test
    void 잔액감소_잔액부족() {
        // Given
        when(accountBalanceRepository.findByIdWithLock("VA12345678"))
            .thenReturn(Optional.of(testAccount));

        // When & Then
        InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class, () -> {
            balanceService.decrease(
                "VA12345678", 
                new BigDecimal("150000"), // 잔액보다 큰 출금
                TransactionType.WITHDRAWAL, 
                "테스트 출금", 
                "REF003"
            );
        });

        assertTrue(exception.getMessage().contains("잔액이 부족합니다"));
        verify(alarmService).sendInsufficientBalanceAlert(eq("VA12345678"), any(), any(), any());
        verify(auditLogService).logWarning(eq("BALANCE_INSUFFICIENT"), eq("ACCOUNT"), eq("VA12345678"), any());
    }

    @Test
    void 계좌없음_예외발생() {
        // Given
        when(accountBalanceRepository.findByIdWithLock("INVALID_ACCOUNT"))
            .thenReturn(Optional.empty());

        // When & Then
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class, () -> {
            balanceService.increase(
                "INVALID_ACCOUNT", 
                new BigDecimal("10000"), 
                TransactionType.DEPOSIT, 
                "테스트 입금", 
                "REF004"
            );
        });

        assertTrue(exception.getMessage().contains("계좌를 찾을 수 없습니다"));
        verify(auditLogService).logWarning(eq("BALANCE_CHANGE"), eq("ACCOUNT"), eq("INVALID_ACCOUNT"), any());
    }

    @Test
    void 잘못된입금금액_예외발생() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            balanceService.increase(
                "VA12345678", 
                new BigDecimal("-1000"), // 음수 금액
                TransactionType.DEPOSIT, 
                "테스트 입금", 
                "REF005"
            );
        });

        assertTrue(exception.getMessage().contains("입금 금액은 0보다 커야 합니다"));
    }

    @Test
    void 잘못된출금금액_예외발생() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            balanceService.decrease(
                "VA12345678", 
                new BigDecimal("-1000"), // 음수 금액
                TransactionType.WITHDRAWAL, 
                "테스트 출금", 
                "REF006"
            );
        });

        assertTrue(exception.getMessage().contains("출금 금액은 0보다 커야 합니다"));
    }

    @Test
    void 잔액조회_성공() {
        // Given
        when(accountBalanceRepository.findById("VA12345678"))
            .thenReturn(Optional.of(testAccount));

        // When
        BigDecimal balance = balanceService.getBalance("VA12345678");

        // Then
        assertEquals(new BigDecimal("100000"), balance);
    }

    @Test
    void 잔액조회_계좌없음() {
        // Given
        when(accountBalanceRepository.findById("INVALID_ACCOUNT"))
            .thenReturn(Optional.empty());

        // When & Then
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class, () -> {
            balanceService.getBalance("INVALID_ACCOUNT");
        });

        assertTrue(exception.getMessage().contains("계좌를 찾을 수 없습니다"));
    }

    /**
     * 동시성 테스트 (실제 환경에서는 더 복잡한 테스트 필요)
     */
    @Test
    void 동시출금_테스트() throws InterruptedException, ExecutionException, TimeoutException {
        // Given
        AccountBalance account = AccountBalance.builder()
            .accountNumber("VA12345678")
            .balance(new BigDecimal("100000"))
            .version(1)
            .build();

        when(accountBalanceRepository.findByIdWithLock("VA12345678"))
            .thenReturn(Optional.of(account));
        when(accountBalanceRepository.save(any(AccountBalance.class)))
            .thenAnswer(invocation -> {
                AccountBalance savedAccount = invocation.getArgument(0);
                return savedAccount;
            });

        // When - 10개의 동시 출금 요청
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CompletableFuture<BalanceService.BalanceChangeResult>[] futures = new CompletableFuture[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            futures[index] = CompletableFuture.supplyAsync(() -> {
                try {
                                    return balanceService.decrease(
                    "VA12345678", 
                    new BigDecimal("5000"), 
                    TransactionType.WITHDRAWAL, 
                    "동시출금테스트_" + index, 
                    "REF_" + index
                );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }

        // Then
        CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
        
        // 모든 요청이 성공했는지 확인
        for (CompletableFuture<BalanceService.BalanceChangeResult> future : futures) {
            assertNotNull(future.get());
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }
} 