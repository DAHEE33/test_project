package fintech2.easypay.audit.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AlarmService {

    // 시스템 알람 (관리자용 - 시스템 에러, 보안 이슈 등)
    public void sendSystemAlert(String service, String message, Exception ex) {
        log.error("[SYSTEM_ALERT] Service: {}, Error: {}", service, message, ex);
        
        // 관리자에게 시스템 에러 알림
        sendAdminNotification("SYSTEM_ERROR", service + ": " + message, ex);
        
        // TODO: Slack 웹훅 전송
        // TODO: 이메일 알림 전송
        // TODO: SMS 알림 전송
    }

    // 비즈니스 이벤트 알람 (사용자용 - 거래내역, 잔액 변동 등)
    public void sendBusinessEvent(String eventType, String userId, String description) {
        log.info("[BUSINESS_EVENT] Type: {}, User: {}, Description: {}", eventType, userId, description);
        
        // 사용자에게 비즈니스 이벤트 알림
        sendUserNotification(userId, eventType, description);
        
        // TODO: Slack 웹훅 전송
        // TODO: 이메일 알림 전송 (중요 이벤트만)
    }

    // 잔액 변동 알람 (사용자용)
    public void sendBalanceChangeAlert(String accountNumber, String userId, String changeType, String amount, String balanceAfter) {
        String message = String.format("계좌 %s의 잔액이 %s되었습니다. 금액: %s원, 잔액: %s원", 
            accountNumber, changeType, amount, balanceAfter);
        
        log.info("[BALANCE_ALERT] {}", message);
        sendUserNotification(userId, "BALANCE_CHANGE", message);
    }

    // 잔액 부족 경고 알람 (사용자용)
    public void sendInsufficientBalanceAlert(String accountNumber, String userId, String currentBalance, String requiredAmount) {
        String message = String.format("계좌 %s의 잔액이 부족합니다. 현재 잔액: %s원, 필요 금액: %s원", 
            accountNumber, currentBalance, requiredAmount);
        
        log.warn("[INSUFFICIENT_BALANCE] {}", message);
        sendUserNotification(userId, "INSUFFICIENT_BALANCE", message);
    }

    // 계정 잠금 알람 (사용자용)
    public void sendAccountLockAlert(String phoneNumber, String userId, String reason) {
        String message = String.format("계정이 잠겼습니다. 휴대폰: %s, 사유: %s", phoneNumber, reason);
        
        log.warn("[ACCOUNT_LOCK] {}", message);
        sendUserNotification(userId, "ACCOUNT_LOCK", message);
        
        // 관리자에게도 보안 이슈 알림
        sendAdminNotification("SECURITY_ISSUE", "계정 잠금: " + phoneNumber + " - " + reason, null);
    }

    // 로그인 실패 알람 (사용자용)
    public void sendLoginFailureAlert(String phoneNumber, String userId, String reason) {
        String message = String.format("로그인 실패. 휴대폰: %s, 사유: %s", phoneNumber, reason);
        
        log.warn("[LOGIN_FAILURE] {}", message);
        sendUserNotification(userId, "LOGIN_FAILURE", message);
        
        // 관리자에게도 보안 이슈 알림
        sendAdminNotification("SECURITY_ISSUE", "로그인 실패: " + phoneNumber + " - " + reason, null);
    }

    // 사용자 알림 개수 조회
    public int getUnreadNotificationCount(String userPrincipal) {
        // 실제로는 DB에서 사용자별 읽지 않은 알림 개수를 조회해야 함
        // 현재는 모의 데이터로 구현
        if (userPrincipal == null) {
            return 0;
        }
        
        // 사용자별로 다른 알림 개수 반환 (테스트용)
        int hashCode = userPrincipal.hashCode();
        int count = Math.abs(hashCode) % 5; // 0~4개
        
        log.info("[NOTIFICATION_COUNT] User: {}, Count: {}", userPrincipal, count);
        return count;
    }

    // 사용자 알림 (거래내역, 잔액 변동 등)
    public void sendUserNotification(String userId, String type, String message) {
        log.info("[USER_NOTIFICATION] User: {}, Type: {}, Message: {}", userId, type, message);
        
        // 사용자 알림 유형별 처리
        switch (type) {
            case "BALANCE_CHANGE":
                // 잔액 변동 알림
                log.info("[USER_BALANCE] {}", message);
                break;
            case "INSUFFICIENT_BALANCE":
                // 잔액 부족 알림
                log.warn("[USER_BALANCE_WARNING] {}", message);
                break;
            case "ACCOUNT_LOCK":
                // 계정 잠금 알림
                log.warn("[USER_ACCOUNT_LOCK] {}", message);
                break;
            case "LOGIN_FAILURE":
                // 로그인 실패 알림
                log.warn("[USER_LOGIN_FAILURE] {}", message);
                break;
            default:
                // 기타 사용자 알림
                log.info("[USER_GENERAL] {}", message);
        }
        
        // TODO: 사용자별 알림 저장
        // TODO: 푸시 알림 전송
        // TODO: 이메일 알림 전송
    }

    // 관리자 알림 (시스템 에러, 보안 이슈 등)
    public void sendAdminNotification(String type, String message, Exception ex) {
        log.error("[ADMIN_NOTIFICATION] Type: {}, Message: {}", type, message, ex);
        
        // 관리자 알림 유형별 처리
        switch (type) {
            case "SYSTEM_ERROR":
                // 시스템 에러 알림
                log.error("[ADMIN_SYSTEM_ERROR] {}", message, ex);
                break;
            case "SECURITY_ISSUE":
                // 보안 이슈 알림
                log.error("[ADMIN_SECURITY] {}", message);
                break;
            case "DATABASE_ERROR":
                // 데이터베이스 에러 알림
                log.error("[ADMIN_DATABASE] {}", message, ex);
                break;
            case "NETWORK_ERROR":
                // 네트워크 에러 알림
                log.error("[ADMIN_NETWORK] {}", message, ex);
                break;
            default:
                // 기타 관리자 알림
                log.error("[ADMIN_GENERAL] {}", message, ex);
        }
        
        // TODO: 관리자에게 Slack 알림
        // TODO: 관리자에게 이메일 알림
        // TODO: 관리자 대시보드에 표시
    }
} 