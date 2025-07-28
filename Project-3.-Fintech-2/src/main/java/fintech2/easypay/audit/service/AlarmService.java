package fintech2.easypay.audit.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AlarmService {

    // 실제로는 DB에 저장해야 하지만, 현재는 메모리에 저장 (테스트용)
    private final Map<String, List<Map<String, Object>>> userAlarms = new ConcurrentHashMap<>();
    
    // 읽음 처리된 알림 추적
    private final Map<String, LocalDateTime> userReadTimes = new ConcurrentHashMap<>();
    
    // 이상거래 감지 임계값
    private static final BigDecimal SUSPICIOUS_AMOUNT_THRESHOLD = new BigDecimal("1000000"); // 100만원
    private static final BigDecimal LARGE_AMOUNT_THRESHOLD = new BigDecimal("500000"); // 50만원
    private static final int FREQUENT_TRANSACTION_THRESHOLD = 5; // 5분 내 5회 이상 거래

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

    // 이상거래 감지 및 알림
    public void detectSuspiciousTransaction(String accountNumber, String userId, BigDecimal amount, String transactionType) {
        // 1. 대금액 거래 감지
        if (amount.compareTo(SUSPICIOUS_AMOUNT_THRESHOLD) > 0) {
            String message = String.format("⚠️ 이상거래 감지: 계좌 %s에서 %s원의 대금액 거래가 발생했습니다. 거래유형: %s", 
                accountNumber, amount.toString(), transactionType);
            
            log.warn("[SUSPICIOUS_TRANSACTION] {}", message);
            sendUserNotification(userId, "SUSPICIOUS_TRANSACTION", message);
            sendAdminNotification("SUSPICIOUS_TRANSACTION", 
                String.format("대금액 거래 감지 - 계좌: %s, 금액: %s원, 유형: %s", accountNumber, amount, transactionType), null);
        }
        
        // 2. 큰 금액 거래 알림
        else if (amount.compareTo(LARGE_AMOUNT_THRESHOLD) > 0) {
            String message = String.format("💰 큰 금액 거래: 계좌 %s에서 %s원의 거래가 발생했습니다. 거래유형: %s", 
                accountNumber, amount.toString(), transactionType);
            
            log.info("[LARGE_TRANSACTION] {}", message);
            sendUserNotification(userId, "LARGE_TRANSACTION", message);
        }
        
        // 3. 빈번한 거래 감지 (실제로는 DB에서 최근 거래 내역을 조회해야 함)
        // 현재는 간단한 예시로 구현
        if (transactionType.equals("WITHDRAWAL") || transactionType.equals("PAYMENT")) {
            String message = String.format("⚡ 빈번한 거래 감지: 계좌 %s에서 잦은 %s 거래가 발생했습니다.", 
                accountNumber, transactionType);
            
            log.warn("[FREQUENT_TRANSACTION] {}", message);
            sendUserNotification(userId, "FREQUENT_TRANSACTION", message);
        }
    }

    // 사용자 알림 개수 조회
    public int getUnreadNotificationCount(String userPrincipal) {
        if (userPrincipal == null) {
            return 0;
        }
        
        List<Map<String, Object>> userAlarmList = userAlarms.get(userPrincipal);
        if (userAlarmList == null) {
            return 0;
        }
        
        // 읽음 처리된 시간 확인
        LocalDateTime readTime = userReadTimes.get(userPrincipal);
        if (readTime != null) {
            // 읽음 처리된 시간 이후의 알림만 카운트
            long unreadCount = userAlarmList.stream()
                .filter(alarm -> {
                    LocalDateTime alarmTime = (LocalDateTime) alarm.get("timestamp");
                    return alarmTime.isAfter(readTime);
                })
                .count();
            return (int) unreadCount;
        }
        
        // 읽음 처리된 적이 없으면 모든 알림을 읽지 않은 것으로 처리
        int count = userAlarmList.size();
        log.info("[NOTIFICATION_COUNT] User: {}, Count: {}", userPrincipal, count);
        return count;
    }

    // 알림 읽음 처리
    public void markNotificationsAsRead(String userId) {
        if (userId == null) {
            return;
        }
        
        userReadTimes.put(userId, LocalDateTime.now());
        log.info("[NOTIFICATION_READ] User: {}, Read time: {}", userId, LocalDateTime.now());
    }

    // 알림 목록 조회
    public List<Map<String, Object>> getNotificationList(String userPrincipal, String category) {
        List<Map<String, Object>> alarms = new ArrayList<>();
        
        if (userPrincipal == null) {
            return alarms;
        }
        
        // 사용자별 저장된 알림 가져오기
        List<Map<String, Object>> userAlarmList = userAlarms.get(userPrincipal);
        if (userAlarmList != null) {
            // 카테고리 필터링
            if (category.equals("all")) {
                alarms.addAll(userAlarmList);
            } else {
                for (Map<String, Object> alarm : userAlarmList) {
                    String alarmCategory = (String) alarm.get("category");
                    if (category.equals(alarmCategory)) {
                        alarms.add(alarm);
                    }
                }
            }
        }
        
        // 최신순으로 정렬
        alarms.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("timestamp");
            LocalDateTime timeB = (LocalDateTime) b.get("timestamp");
            return timeB.compareTo(timeA);
        });
        
        log.info("[NOTIFICATION_LIST] User: {}, Category: {}, Count: {}", userPrincipal, category, alarms.size());
        return alarms;
    }
    
    // 사용자 알림 (거래내역, 잔액 변동 등)
    public void sendUserNotification(String userId, String type, String message) {
        log.info("[USER_NOTIFICATION] User: {}, Type: {}, Message: {}", userId, type, message);
        
        // 사용자별 알림 저장
        Map<String, Object> alarm = createAlarm(type, message);
        saveUserAlarm(userId, alarm);
        
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
            case "LOGIN_SUCCESS":
                // 로그인 성공 알림
                log.info("[USER_LOGIN_SUCCESS] {}", message);
                break;
            case "SUSPICIOUS_TRANSACTION":
                // 이상거래 알림
                log.warn("[USER_SUSPICIOUS_TRANSACTION] {}", message);
                break;
            case "LARGE_TRANSACTION":
                // 큰 금액 거래 알림
                log.info("[USER_LARGE_TRANSACTION] {}", message);
                break;
            case "FREQUENT_TRANSACTION":
                // 빈번한 거래 알림
                log.warn("[USER_FREQUENT_TRANSACTION] {}", message);
                break;
            default:
                // 기타 사용자 알림
                log.info("[USER_GENERAL] {}", message);
        }
        
        // TODO: 푸시 알림 전송
        // TODO: 이메일 알림 전송
    }
    
    // 알림 생성
    private Map<String, Object> createAlarm(String type, String message) {
        Map<String, Object> alarm = new HashMap<>();
        alarm.put("id", System.currentTimeMillis()); // 임시 ID
        alarm.put("type", type);
        alarm.put("message", message);
        alarm.put("timestamp", LocalDateTime.now());
        alarm.put("level", getAlarmLevel(type));
        alarm.put("category", getAlarmCategory(type));
        return alarm;
    }
    
    // 알림 레벨 결정
    private String getAlarmLevel(String type) {
        switch (type) {
            case "ACCOUNT_LOCK":
            case "SYSTEM_ERROR":
            case "SUSPICIOUS_TRANSACTION":
                return "error";
            case "LOGIN_FAILURE":
            case "INSUFFICIENT_BALANCE":
            case "FREQUENT_TRANSACTION":
                return "warning";
            case "BALANCE_CHANGE":
            case "LOGIN_SUCCESS":
            case "LARGE_TRANSACTION":
            default:
                return "info";
        }
    }
    
    // 알림 카테고리 결정
    private String getAlarmCategory(String type) {
        switch (type) {
            case "BALANCE_CHANGE":
            case "INSUFFICIENT_BALANCE":
            case "SUSPICIOUS_TRANSACTION":
            case "LARGE_TRANSACTION":
            case "FREQUENT_TRANSACTION":
                return "balance";
            case "LOGIN_SUCCESS":
            case "LOGIN_FAILURE":
            case "ACCOUNT_LOCK":
                return "login";
            case "SYSTEM_ERROR":
                return "system";
            default:
                return "general";
        }
    }
    
    // 사용자별 알림 저장
    private void saveUserAlarm(String userId, Map<String, Object> alarm) {
        if (userId == null) {
            return;
        }
        
        // USER_REGISTER 알림은 사용자에게 표시하지 않음
        String type = (String) alarm.get("type");
        if ("USER_REGISTER".equals(type)) {
            log.info("[ALARM_FILTERED] USER_REGISTER alarm filtered for user: {}", userId);
            return;
        }
        
        List<Map<String, Object>> userAlarmList = userAlarms.computeIfAbsent(userId, k -> new ArrayList<>());
        
        // 최근 50개 알림만 유지
        if (userAlarmList.size() >= 50) {
            userAlarmList.remove(0);
        }
        
        userAlarmList.add(alarm);
        log.info("[USER_ALARM_SAVED] User: {}, Type: {}, Message: {}", userId, type, alarm.get("message"));
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
            case "SUSPICIOUS_TRANSACTION":
                // 이상거래 알림
                log.error("[ADMIN_SUSPICIOUS_TRANSACTION] {}", message);
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