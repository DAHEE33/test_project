package fintech2.easypay.account.service;

import fintech2.easypay.account.entity.AccountBalance;
import fintech2.easypay.account.entity.TransactionHistory;
import fintech2.easypay.account.repository.AccountBalanceRepository;
import fintech2.easypay.account.repository.TransactionHistoryRepository;
import fintech2.easypay.audit.service.AuditLogService;
import fintech2.easypay.audit.service.AlarmService;
import fintech2.easypay.common.TransactionStatus;
import fintech2.easypay.common.TransactionType;
import fintech2.easypay.common.exception.AccountNotFoundException;
import fintech2.easypay.common.exception.InsufficientBalanceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 중앙화된 잔액 처리 서비스
 * 모든 잔액 변경은 이 서비스를 통해서만 이루어져야 함
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceService {

    private final AccountBalanceRepository accountBalanceRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final AuditLogService auditLogService;
    private final AlarmService alarmService;

    /**
     * 잔액 증가 (입금) - 사용자 ID 포함
     */
    @Transactional(
        isolation = Isolation.SERIALIZABLE,
        propagation = Propagation.REQUIRED,
        timeout = 30,
        rollbackFor = {Exception.class}
    )
    public BalanceChangeResult increase(String accountNumber, BigDecimal amount, 
                                      TransactionType transactionType, String description, String referenceId, String userId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("입금 금액은 0보다 커야 합니다: " + amount);
        }
        
        return changeBalance(accountNumber, amount, transactionType, description, referenceId, userId);
    }

    /**
     * 잔액 감소 (출금) - 사용자 ID 포함
     */
    @Transactional(
        isolation = Isolation.SERIALIZABLE,
        propagation = Propagation.REQUIRED,
        timeout = 30,
        rollbackFor = {Exception.class}
    )
    public BalanceChangeResult decrease(String accountNumber, BigDecimal amount, 
                                      TransactionType transactionType, String description, String referenceId, String userId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("출금 금액은 0보다 커야 합니다: " + amount);
        }
        
        return changeBalance(accountNumber, amount.negate(), transactionType, description, referenceId, userId);
    }

    /**
     * 잔액 증가 (입금) - 기존 호환성용
     */
    @Transactional(
        isolation = Isolation.SERIALIZABLE,
        propagation = Propagation.REQUIRED,
        timeout = 30,
        rollbackFor = {Exception.class}
    )
    public BalanceChangeResult increase(String accountNumber, BigDecimal amount, 
                                      TransactionType transactionType, String description, String referenceId) {
        return increase(accountNumber, amount, transactionType, description, referenceId, "USER");
    }

    /**
     * 잔액 감소 (출금) - 기존 호환성용
     */
    @Transactional(
        isolation = Isolation.SERIALIZABLE,
        propagation = Propagation.REQUIRED,
        timeout = 30,
        rollbackFor = {Exception.class}
    )
    public BalanceChangeResult decrease(String accountNumber, BigDecimal amount, 
                                      TransactionType transactionType, String description, String referenceId) {
        return decrease(accountNumber, amount, transactionType, description, referenceId, "USER");
    }

    /**
     * 잔액 변경의 핵심 로직
     * 동시성 제어, 검증, 이력 기록을 모두 처리
     */
    private BalanceChangeResult changeBalance(String accountNumber, BigDecimal amount, 
                                            TransactionType transactionType, String description, String referenceId, String userId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 계좌 존재 여부 확인 및 동시성 제어
            // Pessimistic Lock을 사용하여 동시 접근 방지
            // 데드락 방지를 위해 계좌번호 순서로 정렬하여 락 획득
            Optional<AccountBalance> accountOpt = accountBalanceRepository.findByIdWithLock(accountNumber);
            if (accountOpt.isEmpty()) {
                auditLogService.logWarning("BALANCE_CHANGE", "ACCOUNT", accountNumber, "계좌를 찾을 수 없습니다");
                throw new AccountNotFoundException("계좌를 찾을 수 없습니다: " + accountNumber);
            }

            AccountBalance account = accountOpt.get();
            BigDecimal balanceBefore = account.getBalance();
            BigDecimal balanceAfter = balanceBefore.add(amount);

            // 2. 잔액 부족 검증 (출금인 경우)
            if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
                auditLogService.logWarning("BALANCE_INSUFFICIENT", "ACCOUNT", accountNumber, 
                    "잔액 부족 - 현재: " + balanceBefore + ", 요청: " + amount.abs());
                
                // 잔액 부족 알람 발송 (비동기로 처리)
                alarmService.sendInsufficientBalanceAlert(
                    accountNumber, userId, balanceBefore.toString(), amount.abs().toString());
                
                throw new InsufficientBalanceException(
                    "잔액이 부족합니다. 현재 잔액: " + balanceBefore + "원, 요청 금액: " + amount.abs() + "원");
            }

            // 3. 잔액 업데이트 (Pessimistic Lock으로 보호됨)
            account.setBalance(balanceAfter);
            AccountBalance savedAccount = accountBalanceRepository.save(account);

            // 4. 거래내역 기록 (트랜잭션 내에서 원자적으로 처리)
            TransactionHistory transaction = TransactionHistory.builder()
                .accountNumber(accountNumber)
                .transactionType(transactionType)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(description)
                .referenceId(referenceId)
                .status(TransactionStatus.COMPLETED)
                .build();
            
            transactionHistoryRepository.save(transaction);

            // 5. 성공 로그 기록
            BalanceChangeResult result = new BalanceChangeResult(
                accountNumber, balanceBefore, balanceAfter, amount, transactionType, referenceId);
            
            auditLogService.logSuccess("BALANCE_CHANGE", "ACCOUNT", accountNumber, 
                "잔액 변경 성공", null);

            // 6. 성능 로깅
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("잔액 변경 완료 - 계좌: {}, 금액: {}, 실행시간: {}ms", 
                accountNumber, amount, executionTime);

            // 7. 알람 발송 (트랜잭션 외부에서 비동기 처리)
            String changeType = amount.compareTo(BigDecimal.ZERO) > 0 ? "입금" : "출금";
            alarmService.sendBalanceChangeAlert(
                accountNumber, userId, changeType, amount.abs().toString(), balanceAfter.toString());

            // 8. 이상거래 감지
            alarmService.detectSuspiciousTransaction(
                accountNumber, userId, amount.abs(), transactionType.name());

            return result;

        } catch (AccountNotFoundException | InsufficientBalanceException e) {
            // 비즈니스 예외는 그대로 전파
            long executionTime = System.currentTimeMillis() - startTime;
            log.warn("잔액 변경 실패 (비즈니스 예외) - 계좌: {}, 금액: {}, 실행시간: {}ms, 오류: {}", 
                accountNumber, amount, executionTime, e.getMessage());
            throw e;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("잔액 변경 중 예상치 못한 오류 발생 - 계좌: {}, 금액: {}, 실행시간: {}ms, 오류: {}", 
                accountNumber, amount, executionTime, e.getMessage(), e);
            
            auditLogService.logError("BALANCE_CHANGE", "ACCOUNT", accountNumber, 
                "잔액 변경 실패: " + e.getMessage(), e);
            
            throw new RuntimeException("잔액 변경 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 잔액 조회 (읽기 전용)
     * 송금/결제 담당자가 잔액 확인용으로 사용
     */
    @Transactional(
        isolation = Isolation.READ_COMMITTED,  // 읽기 일관성 보장
        propagation = Propagation.REQUIRED,    // 기존 트랜잭션 참여 또는 새로 생성
        timeout = 10,                          // 10초 타임아웃
        readOnly = true                        // 읽기 전용 트랜잭션
    )
    public BigDecimal getBalance(String accountNumber) {
        Optional<AccountBalance> accountOpt = accountBalanceRepository.findById(accountNumber);
        if (accountOpt.isEmpty()) {
            throw new AccountNotFoundException("계좌를 찾을 수 없습니다: " + accountNumber);
        }
        return accountOpt.get().getBalance();
    }

    /**
     * 잔액 조회 (락 적용)
     * 송금/결제 담당자가 잔액 확인 후 즉시 변경할 때 사용
     */
    @Transactional(
        isolation = Isolation.SERIALIZABLE,    // 최고 격리 수준
        propagation = Propagation.REQUIRED,    // 기존 트랜잭션 참여 또는 새로 생성
        timeout = 30,                          // 30초 타임아웃
        rollbackFor = {Exception.class}        // 모든 예외 시 롤백
    )
    public BigDecimal getBalanceWithLock(String accountNumber) {
        Optional<AccountBalance> accountOpt = accountBalanceRepository.findByIdWithLock(accountNumber);
        if (accountOpt.isEmpty()) {
            throw new AccountNotFoundException("계좌를 찾을 수 없습니다: " + accountNumber);
        }
        return accountOpt.get().getBalance();
    }

    /**
     * 잔액 충분 여부 확인
     * 송금/결제 담당자가 출금 가능 여부를 미리 확인할 때 사용
     */
    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED,
        timeout = 10,
        readOnly = true
    )
    public boolean hasSufficientBalance(String accountNumber, BigDecimal requiredAmount) {
        try {
            BigDecimal currentBalance = getBalance(accountNumber);
            return currentBalance.compareTo(requiredAmount) >= 0;
        } catch (AccountNotFoundException e) {
            return false;
        }
    }

    /**
     * 잔액 변경 결과를 담는 불변 객체
     */
    public static class BalanceChangeResult {
        private final String accountNumber;
        private final BigDecimal balanceBefore;
        private final BigDecimal balanceAfter;
        private final BigDecimal changeAmount;
        private final TransactionType transactionType;
        private final String referenceId;

        public BalanceChangeResult(String accountNumber, BigDecimal balanceBefore, 
                                 BigDecimal balanceAfter, BigDecimal changeAmount, 
                                 TransactionType transactionType, String referenceId) {
            this.accountNumber = accountNumber;
            this.balanceBefore = balanceBefore;
            this.balanceAfter = balanceAfter;
            this.changeAmount = changeAmount;
            this.transactionType = transactionType;
            this.referenceId = referenceId;
        }

        // Getters
        public String getAccountNumber() { return accountNumber; }
        public BigDecimal getBalanceBefore() { return balanceBefore; }
        public BigDecimal getBalanceAfter() { return balanceAfter; }
        public BigDecimal getChangeAmount() { return changeAmount; }
        public TransactionType getTransactionType() { return transactionType; }
        public String getReferenceId() { return referenceId; }
    }
} 