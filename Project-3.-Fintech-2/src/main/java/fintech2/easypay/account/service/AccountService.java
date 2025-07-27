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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 계좌 관리 서비스 (리팩토링됨)
 * 잔액 처리는 BalanceService에 위임하고, 계좌 조회 및 기본 관리만 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountBalanceRepository accountBalanceRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final BalanceService balanceService; // 중앙화된 잔액 서비스
    private final AuditLogService auditLogService;
    private final AlarmService alarmService;

    public ResponseEntity<?> getBalance(String accountNumber, String token) {
        try {
            // BalanceService를 통해 잔액 조회 (중앙화된 처리)
            BigDecimal balance = balanceService.getBalance(accountNumber);
            
            Map<String, Object> resp = new HashMap<>();
            resp.put("accountNumber", accountNumber);
            resp.put("balance", balance);
            resp.put("currency", "KRW");

            auditLogService.logSuccess("BALANCE_INQUIRY", "ACCOUNT", accountNumber, "잔액 조회 성공", resp);
            return ResponseEntity.ok(resp);

        } catch (AccountNotFoundException e) {
            throw e; // 예외를 다시 던져서 GlobalExceptionHandler에서 처리
        } catch (Exception e) {
            log.error("잔액 조회 중 오류 발생: {}", e.getMessage(), e);
            auditLogService.logError("BALANCE_INQUIRY", "ACCOUNT", accountNumber, "잔액 조회 실패", e);
            throw new RuntimeException("잔액 조회 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 잔액 변경 (기존 API 호환성을 위해 유지, 내부적으로 BalanceService 사용)
     * @deprecated 새로운 비즈니스 로직에서는 BalanceService를 직접 사용하세요
     */
    @Transactional
    public ResponseEntity<?> updateBalance(String accountNumber, BigDecimal amount, String transactionTypeStr, String description) {
        try {
            // TransactionType enum으로 변환
            TransactionType transactionType;
            try {
                transactionType = TransactionType.valueOf(transactionTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                auditLogService.logWarning("BALANCE_UPDATE", "ACCOUNT", accountNumber, "잘못된 거래 유형: " + transactionTypeStr);
                throw new IllegalArgumentException("잘못된 거래 유형입니다: " + transactionTypeStr);
            }

            // BalanceService를 통해 잔액 변경 (중앙화된 처리)
            BalanceService.BalanceChangeResult result;
            String referenceId = "LEGACY_" + System.currentTimeMillis(); // 레거시 API용 참조 ID

            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                // 입금
                result = balanceService.increase(accountNumber, amount, transactionType, description, referenceId);
            } else {
                // 출금
                result = balanceService.decrease(accountNumber, amount.abs(), transactionType, description, referenceId);
            }

            // 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("accountNumber", result.getAccountNumber());
            response.put("balanceBefore", result.getBalanceBefore());
            response.put("balanceAfter", result.getBalanceAfter());
            response.put("transactionType", result.getTransactionType());
            response.put("amount", result.getChangeAmount());
            response.put("message", "잔액이 성공적으로 변경되었습니다");

            return ResponseEntity.ok(response);

        } catch (AccountNotFoundException | InsufficientBalanceException | IllegalArgumentException e) {
            throw e; // 예외를 다시 던져서 GlobalExceptionHandler에서 처리
        } catch (Exception e) {
            log.error("잔액 변경 중 오류 발생: {}", e.getMessage(), e);
            auditLogService.logError("BALANCE_UPDATE", "ACCOUNT", accountNumber, "잔액 변경 실패", e);
            throw new RuntimeException("잔액 변경 중 오류가 발생했습니다", e);
        }
    }

    public ResponseEntity<?> getTransactionHistory(String accountNumber) {
        try {
            List<TransactionHistory> transactions = transactionHistoryRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
            
            auditLogService.logSuccess("TRANSACTION_HISTORY", "ACCOUNT", accountNumber, "거래내역 조회 성공", null);
            return ResponseEntity.ok(transactions);

        } catch (Exception e) {
            log.error("거래내역 조회 중 오류 발생: {}", e.getMessage(), e);
            auditLogService.logError("TRANSACTION_HISTORY", "ACCOUNT", accountNumber, "거래내역 조회 실패", e);
            throw new RuntimeException("거래내역 조회 중 오류가 발생했습니다", e);
        }
    }
} 