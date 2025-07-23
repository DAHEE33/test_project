package fintech2.easypay.auth.service;

import fintech2.easypay.auth.dto.LoginRequest;
import fintech2.easypay.auth.dto.RegisterRequest;
import fintech2.easypay.auth.entity.User;
import fintech2.easypay.auth.repository.UserRepository;
import fintech2.easypay.common.exception.AuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private LoginHistoryService loginHistoryService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private String phoneNumber = "010-1234-5678";
    private String password = "password123";
    private String name = "홍길동";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .phoneNumber(phoneNumber)
                .password("encodedPassword")
                .name(name)
                .accountNumber("VA12345678")
                .loginFailCount(0)
                .isLocked(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void 회원가입_성공() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setPhoneNumber(phoneNumber);
        request.setPassword(password);
        request.setName(name);

        when(userRepository.existsByPhoneNumber(phoneNumber)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(phoneNumber)).thenReturn("jwt-token");

        // When
        ResponseEntity<?> response = authService.register(request);

        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(201);
        verify(userRepository).existsByPhoneNumber(phoneNumber);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 회원가입_중복전화번호() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setPhoneNumber(phoneNumber);
        request.setPassword(password);
        request.setName(name);

        when(userRepository.existsByPhoneNumber(phoneNumber)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("이미 등록된 전화번호입니다");

        verify(userRepository).existsByPhoneNumber(phoneNumber);
    }

    @Test
    void 회원가입_비밀번호규칙위반() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setPhoneNumber(phoneNumber);
        request.setPassword("123"); // 짧은 비밀번호
        request.setName(name);

        when(userRepository.existsByPhoneNumber(phoneNumber)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("비밀번호는 8자 이상이어야 합니다");

        verify(userRepository).existsByPhoneNumber(phoneNumber);
    }

    @Test
    void 로그인_성공() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setPhoneNumber(phoneNumber);
        request.setPassword(password);

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(java.util.Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);
        when(jwtService.generateAccessToken(phoneNumber)).thenReturn("jwt-token");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        // When
        ResponseEntity<?> response = authService.login(request);

        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        verify(loginHistoryService).recordLoginSuccess(testUser, httpServletRequest);
        verify(userRepository).save(testUser); // 로그인 성공 시 실패 카운트 리셋
    }

    @Test
    void 로그인_계정없음() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setPhoneNumber(phoneNumber);
        request.setPassword(password);

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(java.util.Optional.empty());
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("계정을 찾을 수 없습니다");

        verify(loginHistoryService).recordAccountNotFound(phoneNumber, httpServletRequest);
    }

    @Test
    void 로그인_비밀번호불일치() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setPhoneNumber(phoneNumber);
        request.setPassword("wrongPassword");

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(java.util.Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다");

        verify(loginHistoryService).recordLoginFailure(eq(phoneNumber), eq(testUser.getId()),
                anyString(), eq(httpServletRequest), eq(1), eq(false));
        verify(userRepository).save(testUser); // 실패 카운트 증가
    }

    @Test
    void 로그인_계정잠금() {
        // Given
        testUser.setLocked(true);
        testUser.setLockExpiresAt(LocalDateTime.now().plusMinutes(30));

        LoginRequest request = new LoginRequest();
        request.setPhoneNumber(phoneNumber);
        request.setPassword(password);

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(java.util.Optional.of(testUser));
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("계정이 잠겨있습니다");

        verify(loginHistoryService).recordAccountLocked(eq(phoneNumber), eq(testUser.getId()),
                anyString(), eq(httpServletRequest));
    }

    @Test
    void 로그인_5회실패시_계정잠금() {
        // Given
        testUser.setLoginFailCount(4); // 4회 실패 상태

        LoginRequest request = new LoginRequest();
        request.setPhoneNumber(phoneNumber);
        request.setPassword("wrongPassword");

        when(userRepository.findByPhoneNumber(phoneNumber)).thenReturn(java.util.Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", testUser.getPassword())).thenReturn(false);
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");

        // When & Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("비밀번호가 일치하지 않습니다");

        verify(userRepository).save(testUser);
        assertThat(testUser.isLocked()).isTrue();
        assertThat(testUser.getLoginFailCount()).isEqualTo(5);
    }
} 