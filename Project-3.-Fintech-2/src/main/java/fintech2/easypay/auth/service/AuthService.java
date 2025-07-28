package fintech2.easypay.auth.service;

import fintech2.easypay.auth.dto.LoginRequest;
import fintech2.easypay.auth.dto.RegisterRequest;
import fintech2.easypay.auth.entity.User;
import fintech2.easypay.auth.repository.UserRepository;
import fintech2.easypay.account.entity.AccountBalance;
import fintech2.easypay.account.repository.AccountBalanceRepository;
import fintech2.easypay.audit.service.AuditLogService;
import fintech2.easypay.audit.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final AccountBalanceRepository accountBalanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginHistoryService loginHistoryService;
    private final AuditLogService auditLogService;
    private final AlarmService alarmService;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^010-\\d{4}-\\d{4}$");

    @Transactional
    public ResponseEntity<?> register(RegisterRequest req) {
        try {
            // 유효성 검사
            // 1. 휴대폰 번호 중복 체크
            if (userRepository.findByPhoneNumber(req.getPhoneNumber()).isPresent()) {
                auditLogService.logWarning(null, "REGISTER_ATTEMPT", "USER", req.getPhoneNumber(), "중복된 휴대폰 번호로 가입 시도");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "DUPLICATE_PHONE", "message", "이미 가입된 휴대폰 번호입니다"));
            }
            // 2. 비밀번호 규칙
            if (req.getPassword() == null || req.getPassword().length() < 6) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "INVALID_PASSWORD", "message", "비밀번호는 6자 이상이어야 합니다"));
            }
            // 3. 휴대폰 번호 형식
            if (!PHONE_PATTERN.matcher(req.getPhoneNumber()).matches()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "INVALID_PHONE", "message", "휴대폰 번호 형식이 올바르지 않습니다"));
            }
            // 4. 비밀번호 암호화
            String encoded = passwordEncoder.encode(req.getPassword());
            // 5. 가상계좌번호 생성
            String accountNumber = generateAccountNumber();
            // 6. User 저장
            User user = new User();
            user.setPhoneNumber(req.getPhoneNumber());
            user.setPassword(encoded);
            user.setName(req.getName());
            user.setAccountNumber(accountNumber);
            userRepository.save(user);
            // 7. AccountBalance 생성
            AccountBalance accountBalance = AccountBalance.builder()
                    .accountNumber(accountNumber)
                    .balance(BigDecimal.ZERO)
                    .build();
            accountBalanceRepository.save(accountBalance);
            // 8. JWT 발급
            String jwt = jwtService.generateAccessToken(user.getPhoneNumber());
            // 9. 감사로그 기록
            auditLogService.logSuccess(user.getId(), "USER_REGISTER", "USER", user.getId().toString(), 
                    null, "회원가입 완료 - 계좌번호: " + accountNumber);
            // 10. 응답
            Map<String, Object> resp = new HashMap<>();
            resp.put("message", "회원가입이 완료되었습니다");
            resp.put("accessToken", jwt);
            resp.put("accountNumber", accountNumber);
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (Exception e) {
            auditLogService.logError(null, "USER_REGISTER", "USER", req.getPhoneNumber(), "회원가입 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "회원가입 중 오류가 발생했습니다"));
        }
    }

    @Transactional
    public ResponseEntity<?> login(LoginRequest req) {
        String ipAddress = "127.0.0.1"; // 실제로는 HttpServletRequest에서 추출
        String userAgent = "Browser"; // 실제로는 HttpServletRequest에서 추출
        
        try {
            Optional<User> userOpt = userRepository.findByPhoneNumber(req.getPhoneNumber());
            if (userOpt.isEmpty()) {
                // 계정 없음 이력 기록
                loginHistoryService.recordLoginFailure(req.getPhoneNumber(), null, "존재하지 않는 계정", ipAddress, userAgent, 0, false);
                auditLogService.logWarning(null, "LOGIN_ATTEMPT", "USER", req.getPhoneNumber(), "존재하지 않는 계정으로 로그인 시도");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "INVALID_CREDENTIALS", "message", "휴대폰 번호 또는 비밀번호가 올바르지 않습니다"));
            }
            
            User user = userOpt.get();
            
            // 계정 잠금 확인
            if (user.isAccountLocked()) {
                loginHistoryService.recordAccountLocked(req.getPhoneNumber(), user.getId(), user.getLockReason(), ipAddress, userAgent);
                auditLogService.logWarning(user.getId(), "LOGIN_BLOCKED", "USER", user.getId().toString(), "계정 잠금으로 로그인 차단");
                
                // 계정 잠금 알림 발송
                alarmService.sendAccountLockAlert(req.getPhoneNumber(), user.getId().toString(), user.getLockReason());
                
                return ResponseEntity.status(HttpStatus.LOCKED)
                        .body(Map.of("error", "ACCOUNT_LOCKED", "message", "계정이 잠겨있습니다"));
            }
            
            // 비밀번호 검증
            if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
                // 로그인 실패 처리
                user.incrementLoginFailCount();
                userRepository.save(user);
                
                loginHistoryService.recordLoginFailure(req.getPhoneNumber(), user.getId(), "잘못된 비밀번호", 
                        ipAddress, userAgent, user.getLoginFailCount(), user.isAccountLocked());
                
                auditLogService.logWarning(user.getId(), "LOGIN_FAIL", "USER", user.getId().toString(), 
                        "로그인 실패 - 실패 횟수: " + user.getLoginFailCount());
                
                // 로그인 실패 알림 발송
                alarmService.sendLoginFailureAlert(req.getPhoneNumber(), user.getId().toString(), "잘못된 비밀번호");
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "INVALID_CREDENTIALS", "message", "휴대폰 번호 또는 비밀번호가 올바르지 않습니다"));
            }
            
            // 로그인 성공
            user.resetLoginFailCount();
            userRepository.save(user);
            
            String jwt = jwtService.generateAccessToken(user.getPhoneNumber());
            
            // 로그인 성공 이력 기록
            loginHistoryService.recordLoginSuccess(req.getPhoneNumber(), user.getId(), ipAddress, userAgent);
            auditLogService.logSuccess(user.getId(), "USER_LOGIN", "USER", user.getId().toString(), null, "로그인 성공");
            
            // 로그인 성공 알림 생성
            String loginTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분"));
            alarmService.sendBusinessEvent("LOGIN_SUCCESS", user.getId().toString(), 
                String.format("로그인 성공 - %s에 로그인되었습니다", loginTime));
            
            Map<String, Object> resp = new HashMap<>();
            resp.put("accessToken", jwt);
            return ResponseEntity.ok(resp);
            
        } catch (Exception e) {
            auditLogService.logError(null, "USER_LOGIN", "USER", req.getPhoneNumber(), "로그인 처리 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "로그인 중 오류가 발생했습니다"));
        }
    }

    private String generateAccountNumber() {
        // 예시: VA + 8자리 랜덤 + 2자리 체크섬
        String num = String.valueOf((long)(Math.random() * 1_0000_0000L) + 10_000_000L);
        String base = "VA" + num;
        String checksum = String.valueOf((base.hashCode() & 0xFF));
        return base + checksum;
    }
} 