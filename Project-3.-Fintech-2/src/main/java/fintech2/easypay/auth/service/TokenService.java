package fintech2.easypay.auth.service;

import fintech2.easypay.auth.entity.RefreshToken;
import fintech2.easypay.auth.entity.User;
import fintech2.easypay.auth.repository.RefreshTokenRepository;
import fintech2.easypay.common.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TokenService {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Access Token과 Refresh Token 생성
     */
    public TokenPair generateTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user.getPhoneNumber());
        String refreshToken = generateRefreshToken(user);
        
        return TokenPair.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Refresh Token 생성 및 저장
     */
    private String generateRefreshToken(User user) {
        // 기존 Refresh Token 폐기
        refreshTokenRepository.revokeAllByUserId(user.getId(), LocalDateTime.now());
        
        // 새로운 Refresh Token 생성
        String tokenValue = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30); // 30일
        
        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .userId(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .expiresAt(expiresAt)
                .isRevoked(false)
                .build();
        
        refreshTokenRepository.save(refreshToken);
        
        return tokenValue;
    }

    /**
     * Refresh Token으로 새로운 Access Token 발급
     */
    public String refreshAccessToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new AuthException("INVALID_REFRESH_TOKEN", "유효하지 않은 Refresh Token입니다"));
        
        if (!refreshToken.isValid()) {
            throw new AuthException("EXPIRED_REFRESH_TOKEN", "만료된 Refresh Token입니다");
        }
        
        return jwtService.generateAccessToken(refreshToken.getPhoneNumber());
    }

    /**
     * Refresh Token 폐기
     */
    public void revokeRefreshToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new AuthException("INVALID_REFRESH_TOKEN", "유효하지 않은 Refresh Token입니다"));
        
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
    }

    /**
     * 사용자의 모든 Refresh Token 폐기 (로그아웃)
     */
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());
    }

    /**
     * 만료된 Refresh Token 정리 (스케줄링)
     */
    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }

    /**
     * 토큰 쌍을 위한 내부 클래스
     */
    @lombok.Builder
    @lombok.Data
    public static class TokenPair {
        private String accessToken;
        private String refreshToken;
    }
} 