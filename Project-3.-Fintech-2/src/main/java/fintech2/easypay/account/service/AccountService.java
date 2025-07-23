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

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountBalanceRepository accountBalanceRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final AuditLogService auditLogService;
    private final AlarmService alarmService;

    public ResponseEntity<?> getBalance(String accountNumber, String token) {
        try {
            Optional<AccountBalance> accountOpt = accountBalanceRepository.findById(accountNumber);
            
            if (accountOpt.isEmpty()) {
                auditLogService.logWarning("BALANCE_INQUIRY", "ACCOUNT", accountNumber, "계좌를 찾을 수 없습니다");
                throw new AccountNotFoundException("계좌를 찾을 수 없습니다: " + accountNumber);
            }

            AccountBalance ab = accountOpt.get();
            Map<String, Object> resp = new HashMap<>();
            resp.put("accountNumber", ab.getAccountNumber());
            resp.put("balance", ab.getBalance());
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

            Optional<AccountBalance> accountOpt = accountBalanceRepository.findById(accountNumber);
            if (accountOpt.isEmpty()) {
                auditLogService.logWarning("BALANCE_UPDATE", "ACCOUNT", accountNumber, "계좌를 찾을 수 없습니다");
                throw new AccountNotFoundException("계좌를 찾을 수 없습니다: " + accountNumber);
            }

            AccountBalance account = accountOpt.get();
            BigDecimal balanceBefore = account.getBalance();
            BigDecimal balanceAfter = balanceBefore.add(amount);

            // 잔액 부족 검증 (출금인 경우)
            if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
                auditLogService.logWarning("BALANCE_INSUFFICIENT", "ACCOUNT", accountNumber, "잔액 부족");
                
                // 잔액 부족 알람 발송
                alarmService.sendInsufficientBalanceAlert(
                    accountNumber, 
                    "USER", // TODO: 실제 사용자 ID로 변경
                    balanceBefore.toString(), 
                    amount.abs().toString()
                );
                
                throw new InsufficientBalanceException("잔액이 부족합니다. 현재 잔액: " + balanceBefore + "원, 요청 금액: " + amount + "원");
            }

            // 잔액 업데이트
            account.setBalance(balanceAfter);
            accountBalanceRepository.save(account);

            // 거래내역 기록
            TransactionHistory th = new TransactionHistory();
            th.setAccountNumber(accountNumber);
            th.setTransactionType(transactionType);
            th.setAmount(amount);
            th.setBalanceBefore(balanceBefore);
            th.setBalanceAfter(balanceAfter);
            th.setDescription(description);
            th.setStatus(TransactionStatus.COMPLETED);
            transactionHistoryRepository.save(th);

            // 잔액 변동 알람 발송
            String changeType = amount.compareTo(BigDecimal.ZERO) > 0 ? "입금" : "출금";
            alarmService.sendBalanceChangeAlert(
                accountNumber,
                "USER", // TODO: 실제 사용자 ID로 변경
                changeType,
                amount.abs().toString(),
                balanceAfter.toString()
            );

            // 응답 생성
            Map<String, Object> response = new HashMap<>();
            response.put("accountNumber", accountNumber);
            response.put("balanceBefore", balanceBefore);
            response.put("balanceAfter", balanceAfter);
            response.put("transactionType", transactionType);
            response.put("amount", amount);
            response.put("message", "잔액이 성공적으로 변경되었습니다");

            auditLogService.logSuccess("BALANCE_UPDATE", "ACCOUNT", accountNumber, "잔액 변경 성공", response);
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