package fintech2.easypay.auth.service;

import fintech2.easypay.auth.entity.RefreshToken;
import fintech2.easypay.auth.entity.User;
import fintech2.easypay.auth.repository.RefreshTokenRepository;
import fintech2.easypay.common.exception.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private TokenService tokenService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .phoneNumber("010-1234-5678")
                .password("encodedPassword")
                .name("홍길동")
                .accountNumber("VA12345678")
                .loginFailCount(0)
                .isLocked(false)
                .createdAt(LocalDateTime.now())
                .build();

        testRefreshToken = RefreshToken.builder()
                .id(1L)
                .token("test-refresh-token")
                .userId(1L)
                .phoneNumber("010-1234-5678")
                .expiresAt(LocalDateTime.now().plusDays(30))
                .isRevoked(false)
                .build();
    }

    @Test
    void generateTokenPair_성공() {
        // Given
        when(jwtService.generateAccessToken(testUser.getPhoneNumber())).thenReturn("access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // When
        TokenService.TokenPair result = tokenService.generateTokenPair(testUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isNotNull();
        
        verify(jwtService).generateAccessToken(testUser.getPhoneNumber());
        verify(refreshTokenRepository).revokeAllByUserId(testUser.getId(), any(LocalDateTime.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refreshAccessToken_성공() {
        // Given
        String refreshTokenValue = "valid-refresh-token";
        when(refreshTokenRepository.findByToken(refreshTokenValue)).thenReturn(Optional.of(testRefreshToken));
        when(jwtService.generateAccessToken(testRefreshToken.getPhoneNumber())).thenReturn("new-access-token");

        // When
        String result = tokenService.refreshAccessToken(refreshTokenValue);

        // Then
        assertThat(result).isEqualTo("new-access-token");
        verify(refreshTokenRepository).findByToken(refreshTokenValue);
        verify(jwtService).generateAccessToken(testRefreshToken.getPhoneNumber());
    }

    @Test
    void refreshAccessToken_유효하지않은토큰() {
        // Given
        String invalidToken = "invalid-refresh-token";
        when(refreshTokenRepository.findByToken(invalidToken)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tokenService.refreshAccessToken(invalidToken))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("유효하지 않은 Refresh Token입니다");

        verify(refreshTokenRepository).findByToken(invalidToken);
    }

    @Test
    void refreshAccessToken_만료된토큰() {
        // Given
        String expiredToken = "expired-refresh-token";
        testRefreshToken.setExpiresAt(LocalDateTime.now().minusDays(1)); // 만료된 토큰
        when(refreshTokenRepository.findByToken(expiredToken)).thenReturn(Optional.of(testRefreshToken));

        // When & Then
        assertThatThrownBy(() -> tokenService.refreshAccessToken(expiredToken))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("만료된 Refresh Token입니다");

        verify(refreshTokenRepository).findByToken(expiredToken);
    }

    @Test
    void revokeRefreshToken_성공() {
        // Given
        String refreshTokenValue = "valid-refresh-token";
        when(refreshTokenRepository.findByToken(refreshTokenValue)).thenReturn(Optional.of(testRefreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // When
        tokenService.revokeRefreshToken(refreshTokenValue);

        // Then
        verify(refreshTokenRepository).findByToken(refreshTokenValue);
        verify(testRefreshToken).revoke();
        verify(refreshTokenRepository).save(testRefreshToken);
    }

    @Test
    void revokeAllUserTokens_성공() {
        // Given
        Long userId = 1L;

        // When
        tokenService.revokeAllUserTokens(userId);

        // Then
        verify(refreshTokenRepository).revokeAllByUserId(userId, any(LocalDateTime.class));
    }
} 