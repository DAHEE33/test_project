package fintech2.easypay.auth.service;

import fintech2.easypay.auth.entity.LoginHistory;
import fintech2.easypay.auth.entity.User;
import fintech2.easypay.auth.repository.LoginHistoryRepository;
import fintech2.easypay.common.LoginResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
@Transactional
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    /**
     * 로그인 성공 이력 기록
     */
    public void recordLoginSuccess(User user, HttpServletRequest request) {
        LoginHistory history = LoginHistory.builder()
                .userId(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .result(LoginResult.SUCCESS)
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .failCount(0)
                .isLocked(false)
                .build();
        
        loginHistoryRepository.save(history);
    }

    /**
     * 로그인 실패 이력 기록
     */
    public void recordLoginFailure(String phoneNumber, Long userId, String failureReason, 
                                 HttpServletRequest request, int failCount, boolean isLocked) {
        LoginHistory history = LoginHistory.builder()
                .userId(userId)
                .phoneNumber(phoneNumber)
                .result(LoginResult.INVALID_PASSWORD)
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .failureReason(failureReason)
                .failCount(failCount)
                .isLocked(isLocked)
                .build();
        
        loginHistoryRepository.save(history);
    }

    /**
     * 계정 잠금 이력 기록
     */
    public void recordAccountLocked(String phoneNumber, Long userId, String reason, 
                                  HttpServletRequest request) {
        LoginHistory history = LoginHistory.builder()
                .userId(userId)
                .phoneNumber(phoneNumber)
                .result(LoginResult.ACCOUNT_LOCKED)
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .failureReason(reason)
                .failCount(5)
                .isLocked(true)
                .build();
        
        loginHistoryRepository.save(history);
    }

    /**
     * 계정 없음 이력 기록
     */
    public void recordAccountNotFound(String phoneNumber, HttpServletRequest request) {
        LoginHistory history = LoginHistory.builder()
                .userId(null)
                .phoneNumber(phoneNumber)
                .result(LoginResult.ACCOUNT_NOT_FOUND)
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .failureReason("존재하지 않는 계정")
                .failCount(0)
                .isLocked(false)
                .build();
        
        loginHistoryRepository.save(history);
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
} 