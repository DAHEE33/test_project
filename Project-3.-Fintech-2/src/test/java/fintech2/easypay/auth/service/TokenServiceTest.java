package fintech2.easypay.auth.service;

import fintech2.easypay.auth.entity.RefreshToken;
import fintech2.easypay.auth.entity.User;
import fintech2.easypay.auth.repository.RefreshTokenRepository;
import fintech2.easypay.common.exception.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.anyString;
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

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .phoneNumber("010-1234-5678")
                .build();
    }

    @Test
    @DisplayName("토큰 쌍 생성 성공 테스트")
    void generateTokenPair_Success() {
        // given
        String accessToken = "accessToken123";
        String refreshToken = "refreshToken123";
        
        when(jwtService.generateAccessToken(testUser.getPhoneNumber())).thenReturn(accessToken);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(
            RefreshToken.builder().token(refreshToken).build()
        );

        // when
        TokenService.TokenPair result = tokenService.generateTokenPair(testUser);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(accessToken);
        assertThat(result.getRefreshToken()).isEqualTo(refreshToken);
        
        verify(jwtService).generateAccessToken(testUser.getPhoneNumber());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Refresh Token으로 Access Token 갱신 성공 테스트")
    void refreshAccessToken_Success() {
        // given
        String refreshTokenValue = "validRefreshToken";
        String newAccessToken = "newAccessToken";
        
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .userId(1L)
                .phoneNumber("010-1234-5678")
                .expiresAt(LocalDateTime.now().plusDays(30))
                .isRevoked(false)
                .build();
        
        when(refreshTokenRepository.findByToken(refreshTokenValue)).thenReturn(Optional.of(refreshToken));
        when(jwtService.generateAccessToken(refreshToken.getPhoneNumber())).thenReturn(newAccessToken);

        // when
        String result = tokenService.refreshAccessToken(refreshTokenValue);

        // then
        assertThat(result).isEqualTo(newAccessToken);
        verify(refreshTokenRepository).findByToken(refreshTokenValue);
        verify(jwtService).generateAccessToken(refreshToken.getPhoneNumber());
    }

    @Test
    @DisplayName("유효하지 않은 Refresh Token으로 갱신 실패 테스트")
    void refreshAccessToken_InvalidToken_ThrowsException() {
        // given
        String invalidRefreshToken = "invalidToken";
        
        when(refreshTokenRepository.findByToken(invalidRefreshToken)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tokenService.refreshAccessToken(invalidRefreshToken))
                .isInstanceOf(AuthException.class)
                .hasMessage("유효하지 않은 Refresh Token입니다");
        
        verify(refreshTokenRepository).findByToken(invalidRefreshToken);
        verify(jwtService, never()).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("만료된 Refresh Token으로 갱신 실패 테스트")
    void refreshAccessToken_ExpiredToken_ThrowsException() {
        // given
        String expiredRefreshToken = "expiredToken";
        
        RefreshToken refreshToken = RefreshToken.builder()
                .token(expiredRefreshToken)
                .userId(1L)
                .phoneNumber("010-1234-5678")
                .expiresAt(LocalDateTime.now().minusDays(1)) // 만료된 토큰
                .isRevoked(false)
                .build();
        
        when(refreshTokenRepository.findByToken(expiredRefreshToken)).thenReturn(Optional.of(refreshToken));

        // when & then
        assertThatThrownBy(() -> tokenService.refreshAccessToken(expiredRefreshToken))
                .isInstanceOf(AuthException.class)
                .hasMessage("만료된 Refresh Token입니다");
        
        verify(refreshTokenRepository).findByToken(expiredRefreshToken);
        verify(jwtService, never()).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("폐기된 Refresh Token으로 갱신 실패 테스트")
    void refreshAccessToken_RevokedToken_ThrowsException() {
        // given
        String revokedRefreshToken = "revokedToken";
        
        RefreshToken refreshToken = RefreshToken.builder()
                .token(revokedRefreshToken)
                .userId(1L)
                .phoneNumber("010-1234-5678")
                .expiresAt(LocalDateTime.now().plusDays(30))
                .isRevoked(true) // 폐기된 토큰
                .build();
        
        when(refreshTokenRepository.findByToken(revokedRefreshToken)).thenReturn(Optional.of(refreshToken));

        // when & then
        assertThatThrownBy(() -> tokenService.refreshAccessToken(revokedRefreshToken))
                .isInstanceOf(AuthException.class)
                .hasMessage("만료된 Refresh Token입니다");
        
        verify(refreshTokenRepository).findByToken(revokedRefreshToken);
        verify(jwtService, never()).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("Refresh Token 폐기 성공 테스트")
    void revokeRefreshToken_Success() {
        // given
        String refreshTokenValue = "validRefreshToken";
        
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .userId(1L)
                .phoneNumber("010-1234-5678")
                .expiresAt(LocalDateTime.now().plusDays(30))
                .isRevoked(false)
                .build();
        
        when(refreshTokenRepository.findByToken(refreshTokenValue)).thenReturn(Optional.of(refreshToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);

        // when
        tokenService.revokeRefreshToken(refreshTokenValue);

        // then
        verify(refreshTokenRepository).findByToken(refreshTokenValue);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        assertThat(refreshToken.getIsRevoked()).isTrue();
    }

    @Test
    @DisplayName("사용자 모든 토큰 폐기 성공 테스트")
    void revokeAllUserTokens_Success() {
        // given
        Long userId = 1L;

        // when
        tokenService.revokeAllUserTokens(userId);

        // then
        verify(refreshTokenRepository).revokeAllByUserId(userId, any(LocalDateTime.class));
    }
} 