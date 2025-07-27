package fintech2.easypay.account.service;

import fintech2.easypay.common.TransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 송금 담당자를 위한 예제 서비스
 * 실제 송금 담당자가 BalanceService를 어떻게 사용하는지 보여주는 예제
 * 
 * ⚠️ 이 클래스는 예제용이며, 실제 송금 담당자가 구현할 때 참고용으로만 사용
 * 실제 구현 시에는 이 클래스를 삭제하고 송금 담당자가 직접 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferServiceExample {

    private final BalanceService balanceService;

    /**
     * 송금 처리 예제
     * 송금 담당자가 BalanceService를 사용하여 송금을 처리하는 방법
     */
    @Transactional
    public Map<String, Object> transfer(String fromAccountNumber, String toAccountNumber, 
                                      BigDecimal amount, String description, String userId) {
        
        String transferId = generateTransferId();
        
        try {
            // 1. 송금 전 검증
            validateTransferRequest(fromAccountNumber, toAccountNumber, amount);
            
            // 2. 송금 시작 로그
            log.info("송금 시작 - 송금ID: {}, 출금계좌: {}, 입금계좌: {}, 금액: {}", 
                transferId, fromAccountNumber, toAccountNumber, amount);

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
            log.info("송금 완료 - 송금ID: {}, 출금후잔액: {}, 입금후잔액: {}", 
                transferId, withdrawResult.getBalanceAfter(), depositResult.getBalanceAfter());

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

        } catch (Exception e) {
            log.error("송금 실패 - 송금ID: {}, 오류: {}", transferId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("transferId", transferId);
            errorResponse.put("status", "FAILED");
            errorResponse.put("error", e.getMessage());
            
            throw new RuntimeException("송금 처리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 송금 요청 검증
     * 송금 담당자가 구현할 검증 로직
     */
    private void validateTransferRequest(String fromAccountNumber, String toAccountNumber, BigDecimal amount) {
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

        // 2. 잔액 확인 (BalanceService 사용)
        if (!balanceService.hasSufficientBalance(fromAccountNumber, amount)) {
            BigDecimal currentBalance = balanceService.getBalance(fromAccountNumber);
            throw new IllegalArgumentException(
                "송금 가능한 잔액이 부족합니다. 현재 잔액: " + currentBalance + "원, 송금 금액: " + amount + "원");
        }

        // 3. 송금 한도 검증 (송금 담당자의 비즈니스 로직)
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
} 