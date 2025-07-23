package fintech2.easypay.audit.service;

import fintech2.easypay.audit.entity.AuditLog;
import fintech2.easypay.audit.repository.AuditLogRepository;
import fintech2.easypay.common.AuditResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final AlarmService alarmService;

    @Transactional
    public void logSuccess(Long userId, String action, String resourceType, String resourceId, String oldValue, String newValue) {
        AuditLog log = createAuditLog(userId, action, resourceType, resourceId, oldValue, newValue, AuditResult.SUCCESS);
        auditLogRepository.save(log);
        
        // 중요 비즈니스 이벤트는 알람 전송
        if (isImportantEvent(action)) {
            alarmService.sendBusinessEvent(action, userId != null ? userId.toString() : "anonymous", action + " 완료");
        }
    }

    @Transactional
    public void logSuccess(String action, String resourceType, String resourceId, String description, Map<String, Object> details) {
        AuditLog log = createAuditLog(null, action, resourceType, resourceId, null, description, AuditResult.SUCCESS);
        auditLogRepository.save(log);
        
        // 중요 비즈니스 이벤트는 알람 전송
        if (isImportantEvent(action)) {
            alarmService.sendBusinessEvent(action, "system", description);
        }
    }

    @Transactional
    public void logError(Long userId, String action, String resourceType, String resourceId, String error) {
        AuditLog log = createAuditLog(userId, action, resourceType, resourceId, null, error, AuditResult.ERROR);
        auditLogRepository.save(log);
        
        // 에러는 항상 알람
        alarmService.sendSystemAlert(resourceType, error, null);
    }

    @Transactional
    public void logError(String action, String resourceType, String resourceId, String description, Exception exception) {
        AuditLog log = createAuditLog(null, action, resourceType, resourceId, null, description, AuditResult.ERROR);
        auditLogRepository.save(log);
        
        // 에러는 항상 알람
        alarmService.sendSystemAlert(resourceType, description, exception);
    }

    @Transactional
    public void logWarning(Long userId, String action, String resourceType, String resourceId, String warning) {
        AuditLog log = createAuditLog(userId, action, resourceType, resourceId, null, warning, AuditResult.WARNING);
        auditLogRepository.save(log);
    }

    @Transactional
    public void logWarning(String action, String resourceType, String resourceId, String description) {
        AuditLog log = createAuditLog(null, action, resourceType, resourceId, null, description, AuditResult.WARNING);
        auditLogRepository.save(log);
    }

    private AuditLog createAuditLog(Long userId, String action, String resourceType, String resourceId, String oldValue, String newValue, AuditResult result) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setResult(result);
        return log;
    }

    private boolean isImportantEvent(String action) {
        return action.contains("REGISTER") || action.contains("LOGIN") || action.contains("BALANCE_UPDATE") || action.contains("ACCOUNT_CREATE");
    }
} 