package fintech2.easypay.account.service;

import fintech2.easypay.account.entity.TransactionHistory;
import fintech2.easypay.account.repository.TransactionHistoryRepository;
import fintech2.easypay.audit.service.AuditLogService;
import fintech2.easypay.common.TransactionStatus;
import fintech2.easypay.common.TransactionType;
import fintech2.easypay.common.exception.AccountNotFoundException;
import fintech2.easypay.common.exception.InsufficientBalanceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 송금 비즈니스 로직 서비스
 * 잔액 처리는 BalanceService에 위임하고, 송금의 비즈니스 플로우만 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final BalanceService balanceService;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final AuditLogService auditLogService;

    /**
     * 계좌 간 송금
     * @param fromAccountNumber 출금 계좌
     * @param toAccountNumber 입금 계좌
     * @param amount 송금 금액
     * @param description 송금 설명
     * @param userId 송금 요청자 ID
     * @return 송금 결과
     */
    @Transactional
    public Map<String, Object> transfer(String fromAccountNumber, String toAccountNumber, 
                                      BigDecimal amount, String description, String userId) {
        
        String transferId = generateTransferId();
        
        try {
            // 1. 송금 전 검증
            validateTransferRequest(fromAccountNumber, toAccountNumber, amount, userId);
            
            // 2. 송금 시작 로그
            auditLogService.logSuccess("TRANSFER_START", "TRANSFER", transferId, 
                "송금 시작 - 출금계좌: " + fromAccountNumber + ", 입금계좌: " + toAccountNumber + ", 금액: " + amount, null);

            // 3. 출금 처리 (BalanceService 호출)
            BalanceService.BalanceChangeResult withdrawResult = balanceService.decrease(
                fromAccountNumber, 
                amount, 
                TransactionType.TRANSFER, 
                "송금 출금 - " + description + " (송금ID: " + transferId + ")",
                transferId
            );

            // 4. 입금 처리 (BalanceService 호출)
            BalanceService.BalanceChangeResult depositResult = balanceService.increase(
                toAccountNumber, 
                amount, 
                TransactionType.TRANSFER, 
                "송금 입금 - " + description + " (송금ID: " + transferId + ")",
                transferId
            );

            // 5. 송금 완료 로그
            auditLogService.logSuccess("TRANSFER_COMPLETE", "TRANSFER", transferId, 
                "송금 완료 - 출금: " + withdrawResult.getBalanceAfter() + ", 입금: " + depositResult.getBalanceAfter(), null);

            // 6. 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("transferId", transferId);
            response.put("fromAccount", fromAccountNumber);
            response.put("toAccount", toAccountNumber);
            response.put("amount", amount);
            response.put("fromBalanceAfter", withdrawResult.getBalanceAfter());
            response.put("toBalanceAfter", depositResult.getBalanceAfter());
            response.put("status", "COMPLETED");
            response.put("message", "송금이 성공적으로 완료되었습니다");

            return response;

        } catch (AccountNotFoundException | InsufficientBalanceException e) {
            // 비즈니스 예외는 그대로 전파
            auditLogService.logError("TRANSFER_FAILED", "TRANSFER", transferId, 
                "송금 실패: " + e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("송금 중 예상치 못한 오류 발생 - 송금ID: {}, 오류: {}", transferId, e.getMessage(), e);
            auditLogService.logError("TRANSFER_ERROR", "TRANSFER", transferId, 
                "송금 중 오류: " + e.getMessage(), e);
            throw new RuntimeException("송금 처리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 송금 요청 검증
     */
    private void validateTransferRequest(String fromAccountNumber, String toAccountNumber, 
                                       BigDecimal amount, String userId) {
        
        // 1. 기본 검증
        if (fromAccountNumber == null || fromAccountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("출금 계좌번호가 필요합니다");
        }
        if (toAccountNumber == null || toAccountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("입금 계좌번호가 필요합니다");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("송금 금액은 0보다 커야 합니다");
        }
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new IllegalArgumentException("출금 계좌와 입금 계좌가 같을 수 없습니다");
        }

        // 2. 계좌 존재 여부 확인
        try {
            balanceService.getBalance(fromAccountNumber);
        } catch (AccountNotFoundException e) {
            throw new AccountNotFoundException("출금 계좌를 찾을 수 없습니다: " + fromAccountNumber);
        }

        try {
            balanceService.getBalance(toAccountNumber);
        } catch (AccountNotFoundException e) {
            throw new AccountNotFoundException("입금 계좌를 찾을 수 없습니다: " + toAccountNumber);
        }

        // 3. 잔액 확인 (출금 가능 여부)
        BigDecimal currentBalance = balanceService.getBalance(fromAccountNumber);
        if (currentBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                "송금 가능한 잔액이 부족합니다. 현재 잔액: " + currentBalance + "원, 송금 금액: " + amount + "원");
        }

        // 4. 송금 한도 검증 (예: 일일 송금 한도)
        // TODO: 실제 비즈니스 로직에 맞게 구현
        BigDecimal dailyLimit = new BigDecimal("1000000"); // 100만원
        if (amount.compareTo(dailyLimit) > 0) {
            throw new IllegalArgumentException("일일 송금 한도를 초과했습니다. 한도: " + dailyLimit + "원");
        }
    }

    /**
     * 송금 ID 생성
     */
    private String generateTransferId() {
        return "TRF_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * 송금 내역 조회
     */
    public Map<String, Object> getTransferHistory(String accountNumber, String userId) {
        try {
            // 송금 내역 조회 (TRANSFER 타입만)
            // TODO: 실제 구현에서는 더 복잡한 쿼리가 필요할 수 있음
            
            auditLogService.logSuccess("TRANSFER_HISTORY", "TRANSFER", accountNumber, 
                "송금 내역 조회 성공", null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("accountNumber", accountNumber);
            response.put("message", "송금 내역 조회가 완료되었습니다");
            // TODO: 실제 송금 내역 데이터 추가
            
            return response;
            
        } catch (Exception e) {
            log.error("송금 내역 조회 중 오류 발생: {}", e.getMessage(), e);
            auditLogService.logError("TRANSFER_HISTORY_ERROR", "TRANSFER", accountNumber, 
                "송금 내역 조회 실패: " + e.getMessage(), e);
            throw new RuntimeException("송금 내역 조회 중 오류가 발생했습니다", e);
        }
    }
} 