package fintech2.easypay.auth.service;

import fintech2.easypay.account.entity.AccountBalance;
import fintech2.easypay.account.entity.VirtualAccount;
import fintech2.easypay.account.repository.AccountBalanceRepository;
import fintech2.easypay.account.repository.VirtualAccountRepository;
import fintech2.easypay.auth.entity.User;
import fintech2.easypay.auth.repository.UserRepository;
import fintech2.easypay.common.AccountStatus;
import fintech2.easypay.common.UserStatus;
import fintech2.easypay.common.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final VirtualAccountRepository virtualAccountRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final LoginHistoryService loginHistoryService;
    private final EntityManager entityManager;

    /**
     * 회원가입 - User와 VirtualAccount, AccountBalance를 함께 생성
     */
    public User createUser(String phoneNumber, String password) {
        // 1. 휴대폰 번호 중복 체크
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new AuthException("DUPLICATE_PHONE", "이미 가입된 휴대폰 번호입니다");
        }

        // 2. 비밀번호 규칙 검증
        validatePassword(password);

        // 3. User 생성
        User user = User.builder()
                .phoneNumber(phoneNumber)
                .password(passwordEncoder.encode(password)) // 비밀번호 암호화
                .status(UserStatus.ACTIVE)
                .build();

        // 4. User 저장 (ID 생성)
        User savedUser = userRepository.save(user);

        // 5. VirtualAccount 생성
        VirtualAccount virtualAccount = VirtualAccount.builder()
                .accountNumber(generateAccountNumber())
                .status(AccountStatus.ACTIVE)
                .userId(savedUser.getId())
                .build();

        // 6. VirtualAccount 저장
        VirtualAccount savedVirtualAccount = virtualAccountRepository.save(virtualAccount);

        // 7. AccountBalance 생성 (초기 잔액 0원)
        AccountBalance accountBalance = AccountBalance.builder()
                .accountId(savedVirtualAccount.getId())
                .balance(java.math.BigDecimal.ZERO)
                .build();
        
        accountBalanceRepository.save(accountBalance);

        // 8. User에 VirtualAccount 설정
        savedUser.setVirtualAccount(savedVirtualAccount);

        // 9. User 다시 저장
        return userRepository.save(savedUser);
    }

    /**
     * User와 VirtualAccount 함께 조회
     */
    public User getUserWithAccount(Long userId) {
        return userRepository.findUserWithAccount(userId)
                .orElseThrow(() -> new AuthException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다"));
    }

    /**
     * 로그인 처리
     */
    public TokenService.TokenPair login(String phoneNumber, String password, HttpServletRequest request) {
        // 1. 사용자 존재 여부 확인
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    // 계정이 없는 경우 이력 기록
                    loginHistoryService.recordAccountNotFound(phoneNumber, request);
                    return null;
                });

        if (user == null) {
            throw new AuthException("INVALID_CREDENTIALS", "휴대폰 번호 또는 비밀번호가 올바르지 않습니다");
        }

        // 2. 계정 잠금 상태 확인
        if (user.isAccountLocked()) {
            loginHistoryService.recordAccountLocked(phoneNumber, user.getId(), 
                user.getLockReason(), request);
            throw new AuthException("ACCOUNT_LOCKED", "계정이 잠겨있습니다. " + 
                user.getLockReason() + " (해제 예정: " + user.getLockExpiresAt() + ")");
        }

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(password, user.getPassword())) {
            // 로그인 실패 처리 (별도 트랜잭션)
            handleLoginFailure(user, phoneNumber, request);
            throw new AuthException("INVALID_CREDENTIALS", "휴대폰 번호 또는 비밀번호가 올바르지 않습니다");
        }

        // 4. 로그인 성공 처리
        user.resetLoginFailCount();
        userRepository.save(user);
        
        // 로그인 성공 이력 기록
        loginHistoryService.recordLoginSuccess(user, request);

        // 5. 토큰 쌍 생성
        return tokenService.generateTokenPair(user);
    }

    /**
     * 로그인 실패 처리 (별도 트랜잭션)
     */
    @Transactional
    public void handleLoginFailure(User user, String phoneNumber, HttpServletRequest request) {
        // 사용자를 다시 조회하여 최신 상태 확인
        User currentUser = userRepository.findById(user.getId()).orElse(user);
        
        currentUser.incrementLoginFailCount();
        
        // 계정 잠금 상태 확인 및 저장
        if (currentUser.isAccountLocked()) {
            System.out.println("ACCOUNT LOCKED! Fail count: " + currentUser.getLoginFailCount() + 
                             ", Lock reason: " + currentUser.getLockReason() + 
                             ", Lock expires: " + currentUser.getLockExpiresAt());
        }
        
        // 변경사항 저장
        User savedUser = userRepository.save(currentUser);
        
        // 디버깅 로그
        System.out.println("Login failed. Fail count: " + savedUser.getLoginFailCount() + 
                         ", Is locked: " + savedUser.isAccountLocked() + 
                         ", Lock reason: " + savedUser.getLockReason());
        
        // 로그인 실패 이력 기록
        loginHistoryService.recordLoginFailure(phoneNumber, savedUser.getId(), 
            "잘못된 비밀번호", request, savedUser.getLoginFailCount(), savedUser.isAccountLocked());
    }

    /**
     * 로그아웃 처리
     */
    public void logout(Long userId) {
        tokenService.revokeAllUserTokens(userId);
    }

    /**
     * 토큰 갱신
     */
    public String refreshToken(String refreshToken) {
        return tokenService.refreshAccessToken(refreshToken);
    }

    /**
     * 비밀번호 규칙 검증
     */
    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new AuthException("INVALID_PASSWORD", "비밀번호는 6자 이상이어야 합니다");
        }
        
        // 추가 비밀번호 규칙 (실제로는 더 복잡한 규칙 적용)
        if (!password.matches(".*[A-Za-z].*")) {
            throw new AuthException("INVALID_PASSWORD", "비밀번호는 영문자를 포함해야 합니다");
        }
    }

    /**
     * 계좌번호 생성 (비즈니스 규칙: "VA" + 8자리 숫자 + 2자리 체크섬)
     */
    private String generateAccountNumber() {
        Random random = new Random();
        String baseNumber;
        String fullAccountNumber;
        
        do {
            // 8자리 숫자 생성
            int eightDigitNumber = 10000000 + random.nextInt(90000000); // 10000000 ~ 99999999
            baseNumber = "VA" + eightDigitNumber;
            
            // 2자리 체크섬 생성 (간단한 체크섬 알고리즘)
            String checksum = generateChecksum(baseNumber);
            fullAccountNumber = baseNumber + checksum;
            
        } while (virtualAccountRepository.existsByAccountNumber(fullAccountNumber));
        
        return fullAccountNumber;
    }

    /**
     * 체크섬 생성 (간단한 알고리즘)
     */
    private String generateChecksum(String baseNumber) {
        // 간단한 체크섬: 각 자리의 합을 100으로 나눈 나머지
        int sum = 0;
        for (char c : baseNumber.toCharArray()) {
            if (Character.isDigit(c)) {
                sum += Character.getNumericValue(c);
            }
        }
        int checksum = sum % 100;
        return String.format("%02d", checksum);
    }
} 