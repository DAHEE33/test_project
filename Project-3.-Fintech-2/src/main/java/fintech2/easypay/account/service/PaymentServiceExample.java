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
 * 결제 담당자를 위한 예제 서비스
 * 실제 결제 담당자가 BalanceService를 어떻게 사용하는지 보여주는 예제
 * 
 * ⚠️ 이 클래스는 예제용이며, 실제 결제 담당자가 구현할 때 참고용으로만 사용
 * 실제 구현 시에는 이 클래스를 삭제하고 결제 담당자가 직접 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceExample {

    private final BalanceService balanceService;

    /**
     * 결제 처리 예제
     * 결제 담당자가 BalanceService를 사용하여 결제를 처리하는 방법
     */
    @Transactional
    public Map<String, Object> processPayment(String accountNumber, BigDecimal amount, 
                                            String merchantId, String orderId, String userId) {
        
        String paymentId = generatePaymentId();
        
        try {
            // 1. 결제 전 검증
            validatePaymentRequest(accountNumber, amount, merchantId, orderId);
            
            // 2. 결제 시작 로그
            log.info("결제 시작 - 결제ID: {}, 계좌: {}, 금액: {}, 가맹점: {}, 주문번호: {}", 
                paymentId, accountNumber, amount, merchantId, orderId);

            // 3. 결제 처리 (BalanceService 호출)
            BalanceService.BalanceChangeResult paymentResult = balanceService.decrease(
                accountNumber, 
                amount, 
                TransactionType.PAYMENT, 
                "결제 - 가맹점: " + merchantId + ", 주문번호: " + orderId + " (결제ID: " + paymentId + ")",
                paymentId
            );

            // 4. 결제 완료 로그
            log.info("결제 완료 - 결제ID: {}, 잔액: {}", paymentId, paymentResult.getBalanceAfter());

            // 5. 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("paymentId", paymentId);
            response.put("accountNumber", accountNumber);
            response.put("amount", amount);
            response.put("merchantId", merchantId);
            response.put("orderId", orderId);
            response.put("balanceAfter", paymentResult.getBalanceAfter());
            response.put("status", "COMPLETED");
            response.put("message", "결제가 성공적으로 완료되었습니다");

            return response;

        } catch (Exception e) {
            log.error("결제 실패 - 결제ID: {}, 오류: {}", paymentId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("paymentId", paymentId);
            errorResponse.put("status", "FAILED");
            errorResponse.put("error", e.getMessage());
            
            throw new RuntimeException("결제 처리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 결제 취소 처리 예제
     * 결제 담당자가 BalanceService를 사용하여 결제 취소를 처리하는 방법
     */
    @Transactional
    public Map<String, Object> cancelPayment(String accountNumber, BigDecimal amount, 
                                           String originalPaymentId, String userId) {
        
        String cancelId = generateCancelId();
        
        try {
            // 1. 결제 취소 검증
            validateCancelRequest(accountNumber, amount, originalPaymentId);
            
            // 2. 결제 취소 시작 로그
            log.info("결제 취소 시작 - 취소ID: {}, 계좌: {}, 금액: {}, 원결제ID: {}", 
                cancelId, accountNumber, amount, originalPaymentId);

            // 3. 결제 취소 처리 (BalanceService 호출 - 입금)
            BalanceService.BalanceChangeResult cancelResult = balanceService.increase(
                accountNumber, 
                amount, 
                TransactionType.REFUND, 
                "결제 취소 - 원결제ID: " + originalPaymentId + " (취소ID: " + cancelId + ")",
                cancelId
            );

            // 4. 결제 취소 완료 로그
            log.info("결제 취소 완료 - 취소ID: {}, 잔액: {}", cancelId, cancelResult.getBalanceAfter());

            // 5. 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("cancelId", cancelId);
            response.put("accountNumber", accountNumber);
            response.put("amount", amount);
            response.put("originalPaymentId", originalPaymentId);
            response.put("balanceAfter", cancelResult.getBalanceAfter());
            response.put("status", "CANCELLED");
            response.put("message", "결제가 성공적으로 취소되었습니다");

            return response;

        } catch (Exception e) {
            log.error("결제 취소 실패 - 취소ID: {}, 오류: {}", cancelId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("cancelId", cancelId);
            errorResponse.put("status", "FAILED");
            errorResponse.put("error", e.getMessage());
            
            throw new RuntimeException("결제 취소 처리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 결제 요청 검증
     * 결제 담당자가 구현할 검증 로직
     */
    private void validatePaymentRequest(String accountNumber, BigDecimal amount, 
                                      String merchantId, String orderId) {
        // 1. 기본 검증
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("계좌번호가 필요합니다");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다");
        }
        if (merchantId == null || merchantId.trim().isEmpty()) {
            throw new IllegalArgumentException("가맹점 ID가 필요합니다");
        }
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("주문번호가 필요합니다");
        }

        // 2. 잔액 확인 (BalanceService 사용)
        if (!balanceService.hasSufficientBalance(accountNumber, amount)) {
            BigDecimal currentBalance = balanceService.getBalance(accountNumber);
            throw new IllegalArgumentException(
                "결제 가능한 잔액이 부족합니다. 현재 잔액: " + currentBalance + "원, 결제 금액: " + amount + "원");
        }

        // 3. 결제 한도 검증 (결제 담당자의 비즈니스 로직)
        BigDecimal singlePaymentLimit = new BigDecimal("500000"); // 50만원
        if (amount.compareTo(singlePaymentLimit) > 0) {
            throw new IllegalArgumentException("단일 결제 한도를 초과했습니다. 한도: " + singlePaymentLimit + "원");
        }
    }

    /**
     * 결제 취소 요청 검증
     */
    private void validateCancelRequest(String accountNumber, BigDecimal amount, String originalPaymentId) {
        // 1. 기본 검증
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("계좌번호가 필요합니다");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("취소 금액은 0보다 커야 합니다");
        }
        if (originalPaymentId == null || originalPaymentId.trim().isEmpty()) {
            throw new IllegalArgumentException("원결제 ID가 필요합니다");
        }

        // 2. 계좌 존재 여부 확인
        try {
            balanceService.getBalance(accountNumber);
        } catch (Exception e) {
            throw new IllegalArgumentException("유효하지 않은 계좌번호입니다: " + accountNumber);
        }
    }

    /**
     * 결제 ID 생성
     */
    private String generatePaymentId() {
        return "PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    /**
     * 취소 ID 생성
     */
    private String generateCancelId() {
        return "CANCEL_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
} 