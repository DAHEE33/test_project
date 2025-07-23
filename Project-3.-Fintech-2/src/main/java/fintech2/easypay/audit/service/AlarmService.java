package fintech2.easypay.audit.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AlarmService {

    // 시스템 알람 (에러/경고)
    public void sendSystemAlert(String service, String message, Exception ex) {
        log.error("[SYSTEM_ALERT] Service: {}, Error: {}", service, message, ex);
        
        // TODO: Slack 웹훅 전송
        // TODO: 이메일 알림 전송
        // TODO: SMS 알림 전송
    }

    // 비즈니스 이벤트 알람 (중요 이벤트)
    public void sendBusinessEvent(String eventType, String userId, String description) {
        log.info("[BUSINESS_EVENT] Type: {}, User: {}, Description: {}", eventType, userId, description);
        
        // TODO: Slack 웹훅 전송
        // TODO: 이메일 알림 전송 (중요 이벤트만)
    }

    // 잔액 변동 알람
    public void sendBalanceChangeAlert(String accountNumber, String userId, String changeType, String amount, String balanceAfter) {
        String message = String.format("계좌 %s의 잔액이 %s되었습니다. 금액: %s원, 잔액: %s원", 
            accountNumber, changeType, amount, balanceAfter);
        
        log.info("[BALANCE_ALERT] {}", message);
        sendBusinessEvent("BALANCE_CHANGE", userId, message);
    }

    // 잔액 부족 경고 알람
    public void sendInsufficientBalanceAlert(String accountNumber, String userId, String currentBalance, String requiredAmount) {
        String message = String.format("계좌 %s의 잔액이 부족합니다. 현재 잔액: %s원, 필요 금액: %s원", 
            accountNumber, currentBalance, requiredAmount);
        
        log.warn("[INSUFFICIENT_BALANCE] {}", message);
        sendBusinessEvent("INSUFFICIENT_BALANCE", userId, message);
    }

    // 계정 잠금 알람
    public void sendAccountLockAlert(String userId, String phoneNumber, String reason) {
        String message = String.format("계정이 잠겼습니다. 사용자: %s, 휴대폰: %s, 사유: %s", 
            userId, phoneNumber, reason);
        
        log.warn("[ACCOUNT_LOCK] {}", message);
        sendBusinessEvent("ACCOUNT_LOCK", userId, message);
    }

    // 로그인 실패 알람
    public void sendLoginFailureAlert(String userId, String phoneNumber, String reason) {
        String message = String.format("로그인 실패. 사용자: %s, 휴대폰: %s, 사유: %s", 
            userId, phoneNumber, reason);
        
        log.warn("[LOGIN_FAILURE] {}", message);
        sendBusinessEvent("LOGIN_FAILURE", userId, message);
    }
} 