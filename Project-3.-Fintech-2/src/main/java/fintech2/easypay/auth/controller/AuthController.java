package fintech2.easypay.auth.controller;

import fintech2.easypay.auth.dto.AuthResponse;
import fintech2.easypay.auth.dto.LoginRequest;
import fintech2.easypay.auth.dto.RegisterRequest;
import fintech2.easypay.auth.dto.TokenRefreshRequest;
import fintech2.easypay.auth.entity.User;
import fintech2.easypay.auth.service.AuthService;
import fintech2.easypay.auth.service.TokenService;
import fintech2.easypay.common.exception.AuthException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    /**
     * 회원가입 API
     * POST /auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // 1. 사용자 생성
            User user = authService.createUser(request.getPhoneNumber(), request.getPassword());
            
            // 2. User와 VirtualAccount 함께 조회
            User userWithAccount = authService.getUserWithAccount(user.getId());
            
            // 3. JWT 토큰 발급
            TokenService.TokenPair tokenPair = tokenService.generateTokenPair(userWithAccount);
            
            // 4. 응답 생성
            AuthResponse response = AuthResponse.builder()
                    .message("회원가입이 완료되었습니다")
                    .accessToken(tokenPair.getAccessToken())
                    .refreshToken(tokenPair.getRefreshToken())
                    .accountNumber(userWithAccount.getVirtualAccount() != null ? userWithAccount.getVirtualAccount().getAccountNumber() : null)
                    .build();
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (AuthException e) {
            // 비즈니스 예외 처리
            AuthResponse errorResponse = AuthResponse.builder()
                    .error(e.getErrorCode())
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            // 기타 예외 처리
            AuthResponse errorResponse = AuthResponse.builder()
                    .message("회원가입 중 오류가 발생했습니다")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 로그인 API
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, 
                                            HttpServletRequest httpRequest) {
        try {
            // 1. 로그인 처리
            TokenService.TokenPair tokenPair = authService.login(request.getPhoneNumber(), 
                request.getPassword(), httpRequest);
            
            // 2. 응답 생성
            AuthResponse response = AuthResponse.builder()
                    .accessToken(tokenPair.getAccessToken())
                    .refreshToken(tokenPair.getRefreshToken())
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (AuthException e) {
            // 인증 관련 예외 처리
            AuthResponse errorResponse = AuthResponse.builder()
                    .message(e.getMessage())
                    .build();
            
            if ("ACCOUNT_LOCKED".equals(e.getErrorCode())) {
                return ResponseEntity.status(HttpStatus.LOCKED).body(errorResponse);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
        } catch (Exception e) {
            // 기타 예외 처리
            AuthResponse errorResponse = AuthResponse.builder()
                    .message("로그인 중 오류가 발생했습니다")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 토큰 갱신 API
     * POST /auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        try {
            // 1. Refresh Token으로 새로운 Access Token 발급
            String newAccessToken = authService.refreshToken(request.getRefreshToken());
            
            // 2. 응답 생성
            AuthResponse response = AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .message("토큰이 갱신되었습니다")
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (AuthException e) {
            AuthResponse errorResponse = AuthResponse.builder()
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            AuthResponse errorResponse = AuthResponse.builder()
                    .message("토큰 갱신 중 오류가 발생했습니다")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 로그아웃 API
     * POST /auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(@RequestHeader("Authorization") String token) {
        try {
            // TODO: 토큰에서 사용자 ID 추출하여 로그아웃 처리
            // 현재는 간단한 응답만 반환
            
            AuthResponse response = AuthResponse.builder()
                    .message("로그아웃이 완료되었습니다")
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            AuthResponse errorResponse = AuthResponse.builder()
                    .message("로그아웃 중 오류가 발생했습니다")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 휴대폰 번호 중복 체크 API
     * GET /auth/check-phone/{phoneNumber}
     */
    @GetMapping("/check-phone/{phoneNumber}")
    public ResponseEntity<AuthResponse> checkPhoneNumber(@PathVariable String phoneNumber) {
        try {
            // 휴대폰 번호 형식 검증
            if (!phoneNumber.matches("^010-\\d{4}-\\d{4}$")) {
                AuthResponse errorResponse = AuthResponse.builder()
                        .message("휴대폰 번호 형식이 올바르지 않습니다")
                        .build();
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 중복 체크는 회원가입 시에만 수행하므로 여기서는 형식만 검증
            AuthResponse response = AuthResponse.builder()
                    .message("사용 가능한 휴대폰 번호입니다")
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            AuthResponse errorResponse = AuthResponse.builder()
                    .message("휴대폰 번호 확인 중 오류가 발생했습니다")
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
} 