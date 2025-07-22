package fintech2.easypay.auth.service;

import fintech2.easypay.account.entity.VirtualAccount;
import fintech2.easypay.auth.entity.User;
import fintech2.easypay.auth.repository.UserRepository;
import fintech2.easypay.common.AccountStatus;
import fintech2.easypay.common.UserStatus;
import fintech2.easypay.common.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @Mock
    private LoginHistoryService loginHistoryService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private VirtualAccount testAccount;

    @BeforeEach
    void setUp() {
        testAccount = VirtualAccount.builder()
                .id(1L)
                .accountNumber("VA1234567800")
                .status(AccountStatus.ACTIVE)
                .build();

        testUser = User.builder()
                .id(1L)
                .phoneNumber("010-1234-5678")
                .password("encodedPassword")
                .status(UserStatus.ACTIVE)
                .virtualAccount(testAccount)
                .loginFailCount(0)
                .isLocked(false)
                .build();
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void createUser_Success() {
        // given
        String phoneNumber = "010-1234-5678";
        String password = "password123";
        
        when(userRepository.existsByPhoneNumber(phoneNumber)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // when
        User result = authService.createUser(phoneNumber, password);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(result.getVirtualAccount()).isNotNull();
        assertThat(result.getVirtualAccount().getAccountNumber()).startsWith("VA");
        
        verify(userRepository).existsByPhoneNumber(phoneNumber);
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("중복 휴대폰 번호로 회원가입 실패 테스트")
    void createUser_DuplicatePhoneNumber_ThrowsException() {
        // given
        String phoneNumber = "010-1234-5678";
        String password = "password123";
        
        when(userRepository.existsByPhoneNumber(phoneNumber)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.createUser(phoneNumber, password))
                .isInstanceOf(AuthException.class)
                .hasMessage("이미 가입된 휴대폰 번호입니다");
        
        verify(userRepository).existsByPhoneNumber(phoneNumber);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("비밀번호 규칙 위반으로 회원가입 실패 테스트")
    void createUser_InvalidPassword_ThrowsException() {
        // given
        String phoneNumber = "010-1234-5678";
        String shortPassword = "123"; // 6자 미만
        
        when(userRepository.existsByPhoneNumber(phoneNumber)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.createUser(phoneNumber, shortPassword))
                .isInstanceOf(AuthException.class)
                .hasMessage("비밀번호는 6자 이상이어야 합니다");
        
        verify(userRepository).existsByPhoneNumber(phoneNumber);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void login_Success() {
        // given
        String phoneNumber = "010-1234-5678";
        String password = "password123";
        
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenService.generateTokenPair(testUser)).thenReturn(
            TokenService.TokenPair.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .build()
        );

        // when
        TokenService.TokenPair result = authService.login(phoneNumber, password, httpServletRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("accessToken");
        assertThat(result.getRefreshToken()).isEqualTo("refreshToken");
        
        verify(userRepository).findByPhoneNumber(phoneNumber);
        verify(passwordEncoder).matches(password, testUser.getPassword());
        verify(loginHistoryService).recordLoginSuccess(testUser, httpServletRequest);
        verify(tokenService).generateTokenPair(testUser);
    }

    @Test
    @DisplayName("존재하지 않는 계정으로 로그인 실패 테스트")
    void login_AccountNotFound_ThrowsException() {
        // given
        String phoneNumber = "010-1234-5678";
        String password = "password123";
        
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(phoneNumber, password, httpServletRequest))
                .isInstanceOf(AuthException.class)
                .hasMessage("휴대폰 번호 또는 비밀번호가 올바르지 않습니다");
        
        verify(userRepository).findByPhoneNumber(phoneNumber);
        verify(loginHistoryService).recordAccountNotFound(phoneNumber, httpServletRequest);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 실패 테스트")
    void login_InvalidPassword_ThrowsException() {
        // given
        String phoneNumber = "010-1234-5678";
        String wrongPassword = "wrongPassword";
        
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(wrongPassword, testUser.getPassword())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // when & then
        assertThatThrownBy(() -> authService.login(phoneNumber, wrongPassword, httpServletRequest))
                .isInstanceOf(AuthException.class)
                .hasMessage("휴대폰 번호 또는 비밀번호가 올바르지 않습니다");
        
        verify(userRepository).findByPhoneNumber(phoneNumber);
        verify(passwordEncoder).matches(wrongPassword, testUser.getPassword());
        verify(loginHistoryService).recordLoginFailure(eq(phoneNumber), eq(testUser.getId()), 
            anyString(), eq(httpServletRequest), eq(1), eq(false));
    }

    @Test
    @DisplayName("잠긴 계정으로 로그인 실패 테스트")
    void login_LockedAccount_ThrowsException() {
        // given
        String phoneNumber = "010-1234-5678";
        String password = "password123";
        
        testUser.setIsLocked(true);
        testUser.setLockReason("로그인 5회 연속 실패");
        testUser.setLockExpiresAt(LocalDateTime.now().plusMinutes(30));
        
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUser));

        // when & then
        assertThatThrownBy(() -> authService.login(phoneNumber, password, httpServletRequest))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("계정이 잠겨있습니다");
        
        verify(userRepository).findByPhoneNumber(phoneNumber);
        verify(loginHistoryService).recordAccountLocked(eq(phoneNumber), eq(testUser.getId()), 
            eq("로그인 5회 연속 실패"), eq(httpServletRequest));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("5회 연속 실패로 계정 잠금 테스트")
    void login_FiveConsecutiveFailures_LocksAccount() {
        // given
        String phoneNumber = "010-1234-5678";
        String wrongPassword = "wrongPassword";
        
        testUser.setLoginFailCount(4); // 4회 실패 상태
        
        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(wrongPassword, testUser.getPassword())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // when
        assertThatThrownBy(() -> authService.login(phoneNumber, wrongPassword, httpServletRequest))
                .isInstanceOf(AuthException.class);

        // then
        verify(userRepository).save(any(User.class));
        // 5회 실패 시 계정이 잠겨야 함
        assertThat(testUser.getLoginFailCount()).isEqualTo(5);
        assertThat(testUser.getIsLocked()).isTrue();
    }
} 