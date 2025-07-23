package fintech2.easypay.auth.service;

import fintech2.easypay.auth.entity.LoginHistory;
import fintech2.easypay.auth.entity.User;
import fintech2.easypay.auth.repository.LoginHistoryRepository;
import fintech2.easypay.common.LoginResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    @Transactional
    public void recordLoginSuccess(String phoneNumber, Long userId, String userAgent, String ipAddress) {
        LoginHistory history = LoginHistory.builder()
                .phoneNumber(phoneNumber)
                .userId(userId)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .result(LoginResult.SUCCESS)
                .loginAt(LocalDateTime.now())
                .build();
        
        loginHistoryRepository.save(history);
        log.info("로그인 성공 기록: 사용자 ID {}, IP {}", userId, ipAddress);
    }

    @Transactional
    public void recordLoginSuccess(User user, HttpServletRequest request) {
        recordLoginSuccess(user.getPhoneNumber(), user.getId(), 
            request.getHeader("User-Agent"), getClientIpAddress(request));
    }

    @Transactional
    public void recordLoginFailure(String phoneNumber, Long userId, String userAgent, String ipAddress, String reason, int failCount, boolean isLocked) {
        LoginHistory history = LoginHistory.builder()
                .phoneNumber(phoneNumber)
                .userId(userId)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .result(LoginResult.FAILED)
                .failReason(reason)
                .failCount(failCount)
                .isLocked(isLocked)
                .loginAt(LocalDateTime.now())
                .build();
        
        loginHistoryRepository.save(history);
        log.warn("로그인 실패 기록: 사용자 ID {}, IP {}, 사유: {}", userId, ipAddress, reason);
    }

    @Transactional
    public void recordLoginFailure(String phoneNumber, Long userId, String userAgent, HttpServletRequest request, int failCount, boolean isLocked) {
        recordLoginFailure(phoneNumber, userId, userAgent, getClientIpAddress(request), "비밀번호 불일치", failCount, isLocked);
    }

    @Transactional
    public void recordAccountNotFound(String phoneNumber, String userAgent, String ipAddress) {
        LoginHistory history = LoginHistory.builder()
                .phoneNumber(phoneNumber)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .result(LoginResult.ACCOUNT_NOT_FOUND)
                .failReason("계정을 찾을 수 없습니다")
                .loginAt(LocalDateTime.now())
                .build();
        
        loginHistoryRepository.save(history);
        log.warn("계정 없음 기록: 전화번호 {}, IP {}", phoneNumber, ipAddress);
    }

    @Transactional
    public void recordAccountNotFound(String phoneNumber, HttpServletRequest request) {
        recordAccountNotFound(phoneNumber, request.getHeader("User-Agent"), getClientIpAddress(request));
    }

    @Transactional
    public void recordAccountLocked(String phoneNumber, Long userId, String userAgent, String ipAddress, String reason) {
        LoginHistory history = LoginHistory.builder()
                .phoneNumber(phoneNumber)
                .userId(userId)
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .result(LoginResult.ACCOUNT_LOCKED)
                .failReason(reason)
                .isLocked(true)
                .loginAt(LocalDateTime.now())
                .build();
        
        loginHistoryRepository.save(history);
        log.warn("계정 잠금 기록: 사용자 ID {}, IP {}, 사유: {}", userId, ipAddress, reason);
    }

    @Transactional
    public void recordAccountLocked(String phoneNumber, Long userId, String userAgent, HttpServletRequest request) {
        recordAccountLocked(phoneNumber, userId, userAgent, getClientIpAddress(request), "로그인 5회 연속 실패");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
} 