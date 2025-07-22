package fintech2.easypay.account.service;

import fintech2.easypay.account.entity.AccountBalance;
import fintech2.easypay.account.entity.TransactionHistory;
import fintech2.easypay.account.entity.VirtualAccount;
import fintech2.easypay.account.dto.BalanceResponse;
import fintech2.easypay.account.dto.AccountInfoResponse;
import fintech2.easypay.account.dto.TransactionResponse;
import fintech2.easypay.common.TransactionStatus;
import fintech2.easypay.common.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

    /**
     * 계좌 소유자 검증
     */
    public boolean isAccountOwner(Long accountId, Long userId) {
        // TODO: 실제 계좌 소유자 검증 로직 구현
        // 현재는 임시로 true 반환
        return true;
    }

    /**
     * 잔액 조회 (BalanceResponse 반환)
     */
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long accountId) {
        // TODO: 실제 잔액 조회 로직 구현
        BigDecimal balance = BigDecimal.ZERO;
        
        return BalanceResponse.builder()
                .success(true)
                .message("잔액 조회 성공")
                .accountNumber("ACC" + accountId)
                .balance(balance)
                .currency("KRW")
                .lastUpdated(LocalDateTime.now())
                .status("ACTIVE")
                .build();
    }

    /**
     * 계좌 정보 조회
     */
    @Transactional(readOnly = true)
    public AccountInfoResponse getAccountInfo(Long userId) {
        // TODO: 실제 계좌 정보 조회 로직 구현
        
        return AccountInfoResponse.builder()
                .success(true)
                .message("계좌 정보 조회 성공")
                .accountNumber("ACC" + userId)
                .phoneNumber("010-****-" + (userId % 10000))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 입금 처리
     */
    public TransactionResponse deposit(Long accountId, BigDecimal amount, String description) {
        // TODO: 실제 입금 로직 구현
        updateBalance(accountId, amount, TransactionType.DEPOSIT, description);
        
        return TransactionResponse.builder()
                .success(true)
                .message("입금 처리 완료")
                .transactionId(UUID.randomUUID().toString())
                .status("COMPLETED")
                .amount(amount)
                .balanceAfter(BigDecimal.ZERO) // TODO: 실제 잔액 계산
                .build();
    }

    /**
     * 잔액 증감 로직 (Central BalanceService)
     */
    public void updateBalance(Long accountId, BigDecimal amount, TransactionType transactionType, String description) {
        // 1. DB 락 획득 (SELECT ... FOR UPDATE)
        // TODO: 비관적 락 구현

        // 2. 현재 잔액 조회 및 검증
        // TODO: AccountBalance 조회

        // 3. 거래 한도 확인
        validateTransactionLimit(amount);

        // 4. 잔액 증감 계산
        // TODO: 잔액 업데이트 로직

        // 5. 거래 히스토리 저장
        saveTransactionHistory(accountId, amount, transactionType, description);
    }

    /**
     * 거래 한도 검증
     */
    private void validateTransactionLimit(BigDecimal amount) {
        BigDecimal maxSingleTransaction = new BigDecimal("10000000"); // 1,000만원
        BigDecimal maxDailyTransaction = new BigDecimal("50000000"); // 5,000만원

        if (amount.compareTo(maxSingleTransaction) > 0) {
            throw new IllegalArgumentException("1회 거래 한도를 초과했습니다");
        }

        // TODO: 일일 거래 한도 체크 로직
    }

    /**
     * 거래 히스토리 저장
     */
    private void saveTransactionHistory(Long accountId, BigDecimal amount, TransactionType transactionType, String description) {
        TransactionHistory history = TransactionHistory.builder()
                .account(VirtualAccount.builder().id(accountId).build()) // 실제로는 조회해서 설정
                .transactionType(transactionType)
                .amount(amount)
                .description(description)
                .status(TransactionStatus.COMPLETED)
                .build();

        // TODO: TransactionHistoryRepository.save() 호출
    }

    /**
     * 잔액 조회 (BigDecimal 반환 - 기존 메서드)
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalanceAmount(Long accountId) {
        // TODO: AccountBalance 조회 로직
        return BigDecimal.ZERO;
    }
} 