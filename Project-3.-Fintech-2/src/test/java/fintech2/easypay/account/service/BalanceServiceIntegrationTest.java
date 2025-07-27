package fintech2.easypay.account.service;

import fintech2.easypay.account.entity.AccountBalance;
import fintech2.easypay.account.repository.AccountBalanceRepository;
import fintech2.easypay.common.TransactionType;
import fintech2.easypay.common.exception.AccountNotFoundException;
import fintech2.easypay.common.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 송금/결제 담당자를 위한 BalanceService 통합 테스트
 * 실제 DB를 사용하여 동시성 제어와 트랜잭션 처리를 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BalanceServiceIntegrationTest {

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private AccountBalanceRepository accountBalanceRepository;

    private String testAccountNumber1;
    private String testAccountNumber2;

    @BeforeEach
    void setUp() {
        // 테스트 계좌 생성
        testAccountNumber1 = "TEST_ACC_001";
        testAccountNumber2 = "TEST_ACC_002";

        AccountBalance account1 = AccountBalance.builder()
            .accountNumber(testAccountNumber1)
            .balance(new BigDecimal("100000"))
            .build();

        AccountBalance account2 = AccountBalance.builder()
            .accountNumber(testAccountNumber2)
            .balance(new BigDecimal("50000"))
            .build();

        accountBalanceRepository.save(account1);
        accountBalanceRepository.save(account2);
    }

    @Test
    void 송금담당자_잔액조회_테스트() {
        // Given & When
        BigDecimal balance1 = balanceService.getBalance(testAccountNumber1);
        BigDecimal balance2 = balanceService.getBalance(testAccountNumber2);

        // Then
        assertEquals(new BigDecimal("100000"), balance1);
        assertEquals(new BigDecimal("50000"), balance2);
    }

    @Test
    void 송금담당자_잔액충분여부확인_테스트() {
        // Given & When
        boolean hasSufficient1 = balanceService.hasSufficientBalance(testAccountNumber1, new BigDecimal("50000"));
        boolean hasSufficient2 = balanceService.hasSufficientBalance(testAccountNumber1, new BigDecimal("150000"));
        boolean hasSufficient3 = balanceService.hasSufficientBalance(testAccountNumber2, new BigDecimal("30000"));

        // Then
        assertTrue(hasSufficient1);  // 10만원 계좌에서 5만원 출금 가능
        assertFalse(hasSufficient2); // 10만원 계좌에서 15만원 출금 불가
        assertTrue(hasSufficient3);  // 5만원 계좌에서 3만원 출금 가능
    }

    @Test
    void 송금담당자_출금처리_성공_테스트() {
        // Given
        BigDecimal withdrawAmount = new BigDecimal("30000");

        // When
        BalanceService.BalanceChangeResult result = balanceService.decrease(
            testAccountNumber1, 
            withdrawAmount, 
            TransactionType.TRANSFER, 
            "송금 출금 테스트", 
            "TRF_TEST_001"
        );

        // Then
        assertEquals(testAccountNumber1, result.getAccountNumber());
        assertEquals(new BigDecimal("100000"), result.getBalanceBefore());
        assertEquals(new BigDecimal("70000"), result.getBalanceAfter());
        assertEquals(new BigDecimal("-30000"), result.getChangeAmount());
        assertEquals(TransactionType.TRANSFER, result.getTransactionType());
        assertEquals("TRF_TEST_001", result.getReferenceId());

        // DB에서 실제 잔액 확인
        BigDecimal actualBalance = accountBalanceRepository.findById(testAccountNumber1).get().getBalance();
        assertEquals(new BigDecimal("70000"), actualBalance);
    }

    @Test
    void 송금담당자_입금처리_성공_테스트() {
        // Given
        BigDecimal depositAmount = new BigDecimal("20000");

        // When
        BalanceService.BalanceChangeResult result = balanceService.increase(
            testAccountNumber2, 
            depositAmount, 
            TransactionType.TRANSFER, 
            "송금 입금 테스트", 
            "TRF_TEST_002"
        );

        // Then
        assertEquals(testAccountNumber2, result.getAccountNumber());
        assertEquals(new BigDecimal("50000"), result.getBalanceBefore());
        assertEquals(new BigDecimal("70000"), result.getBalanceAfter());
        assertEquals(new BigDecimal("20000"), result.getChangeAmount());
        assertEquals(TransactionType.TRANSFER, result.getTransactionType());
        assertEquals("TRF_TEST_002", result.getReferenceId());

        // DB에서 실제 잔액 확인
        BigDecimal actualBalance = accountBalanceRepository.findById(testAccountNumber2).get().getBalance();
        assertEquals(new BigDecimal("70000"), actualBalance);
    }

    @Test
    void 송금담당자_잔액부족_출금실패_테스트() {
        // Given
        BigDecimal withdrawAmount = new BigDecimal("150000"); // 잔액보다 큰 출금

        // When & Then
        InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class, () -> {
            balanceService.decrease(
                testAccountNumber1, 
                withdrawAmount, 
                TransactionType.TRANSFER, 
                "잔액 부족 테스트", 
                "TRF_TEST_003"
            );
        });

        assertTrue(exception.getMessage().contains("잔액이 부족합니다"));
        assertTrue(exception.getMessage().contains("100000"));
        assertTrue(exception.getMessage().contains("150000"));

        // 잔액이 변경되지 않았는지 확인
        BigDecimal actualBalance = accountBalanceRepository.findById(testAccountNumber1).get().getBalance();
        assertEquals(new BigDecimal("100000"), actualBalance);
    }

    @Test
    void 송금담당자_계좌없음_예외발생_테스트() {
        // When & Then
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class, () -> {
            balanceService.getBalance("INVALID_ACCOUNT");
        });

        assertTrue(exception.getMessage().contains("계좌를 찾을 수 없습니다"));
    }

    @Test
    void 송금담당자_동시출금_동시성제어_테스트() throws Exception {
        // Given
        int threadCount = 10;
        BigDecimal withdrawAmount = new BigDecimal("5000"); // 각각 5천원씩 출금
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // When - 10개의 동시 출금 요청 (총 5만원 출금)
        CompletableFuture<BalanceService.BalanceChangeResult>[] futures = new CompletableFuture[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures[index] = CompletableFuture.supplyAsync(() -> {
                try {
                    return balanceService.decrease(
                        testAccountNumber1, 
                        withdrawAmount, 
                        TransactionType.TRANSFER, 
                        "동시출금테스트_" + index, 
                        "TRF_CONCURRENT_" + index
                    );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }

        // Then
        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        
        // 모든 요청이 성공했는지 확인
        for (CompletableFuture<BalanceService.BalanceChangeResult> future : futures) {
            assertNotNull(future.get());
        }

        // 최종 잔액이 정확히 계산되었는지 확인
        // 초기 잔액: 100,000원, 총 출금: 50,000원, 최종 잔액: 50,000원
        BigDecimal finalBalance = accountBalanceRepository.findById(testAccountNumber1).get().getBalance();
        assertEquals(new BigDecimal("50000"), finalBalance);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void 송금담당자_송금시나리오_테스트() {
        // Given - 송금 시나리오: 계좌1에서 계좌2로 3만원 송금
        BigDecimal transferAmount = new BigDecimal("30000");

        // When - 송금 처리 (송금 담당자가 구현할 로직)
        // 1. 출금 계좌에서 잔액 확인
        BigDecimal balanceBefore = balanceService.getBalance(testAccountNumber1);
        assertTrue(balanceService.hasSufficientBalance(testAccountNumber1, transferAmount));

        // 2. 출금 처리
        BalanceService.BalanceChangeResult withdrawResult = balanceService.decrease(
            testAccountNumber1, 
            transferAmount, 
            TransactionType.TRANSFER, 
            "송금 출금", 
            "TRF_SCENARIO_001"
        );

        // 3. 입금 처리
        BalanceService.BalanceChangeResult depositResult = balanceService.increase(
            testAccountNumber2, 
            transferAmount, 
            TransactionType.TRANSFER, 
            "송금 입금", 
            "TRF_SCENARIO_001"
        );

        // Then
        // 출금 계좌 잔액 확인
        assertEquals(new BigDecimal("70000"), withdrawResult.getBalanceAfter());
        
        // 입금 계좌 잔액 확인
        assertEquals(new BigDecimal("80000"), depositResult.getBalanceAfter());
        
        // DB에서 실제 잔액 확인
        BigDecimal actualBalance1 = accountBalanceRepository.findById(testAccountNumber1).get().getBalance();
        BigDecimal actualBalance2 = accountBalanceRepository.findById(testAccountNumber2).get().getBalance();
        
        assertEquals(new BigDecimal("70000"), actualBalance1);
        assertEquals(new BigDecimal("80000"), actualBalance2);
    }

    @Test
    void 결제담당자_결제시나리오_테스트() {
        // Given - 결제 시나리오: 계좌1에서 2만원 결제
        BigDecimal paymentAmount = new BigDecimal("20000");

        // When - 결제 처리 (결제 담당자가 구현할 로직)
        // 1. 결제 가능 여부 확인
        assertTrue(balanceService.hasSufficientBalance(testAccountNumber1, paymentAmount));

        // 2. 결제 처리 (출금)
        BalanceService.BalanceChangeResult paymentResult = balanceService.decrease(
            testAccountNumber1, 
            paymentAmount, 
            TransactionType.PAYMENT, 
            "온라인 쇼핑몰 결제", 
            "PAY_ORDER_001"
        );

        // Then
        assertEquals(new BigDecimal("80000"), paymentResult.getBalanceAfter());
        assertEquals(TransactionType.PAYMENT, paymentResult.getTransactionType());
        assertEquals("PAY_ORDER_001", paymentResult.getReferenceId());

        // DB에서 실제 잔액 확인
        BigDecimal actualBalance = accountBalanceRepository.findById(testAccountNumber1).get().getBalance();
        assertEquals(new BigDecimal("80000"), actualBalance);
    }
} 