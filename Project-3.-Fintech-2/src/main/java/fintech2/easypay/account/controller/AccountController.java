package fintech2.easypay.account.controller;

import fintech2.easypay.auth.dto.UserPrincipal;
import fintech2.easypay.account.dto.BalanceResponse;
import fintech2.easypay.account.dto.AccountInfoResponse;
import fintech2.easypay.account.dto.TransactionResponse;
import fintech2.easypay.account.dto.DepositRequest;
import fintech2.easypay.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * 잔액 조회 API (JWT 인증 필요)
     * 
     * 사용법:
     * 1. 로그인 API로 JWT 토큰 획득
     * 2. Authorization 헤더에 "Bearer {JWT}" 추가
     * 3. @AuthenticationPrincipal로 사용자 정보 자동 추출
     */
    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable Long accountId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        // userPrincipal에서 사용자 정보 추출
        Long userId = userPrincipal.getId();
        String phoneNumber = userPrincipal.getPhoneNumber();
        String userAccountNumber = userPrincipal.getAccountNumber();
        
        // 계좌 소유자 검증 (보안)
        if (!accountService.isAccountOwner(accountId, userId)) {
            return ResponseEntity.status(403).body(
                BalanceResponse.builder()
                    .success(false)
                    .message("해당 계좌에 대한 접근 권한이 없습니다.")
                    .build()
            );
        }
        
        // 비즈니스 로직 실행
        BalanceResponse balance = accountService.getBalance(accountId);
        return ResponseEntity.ok(balance);
    }

    /**
     * 계좌 정보 조회 API (JWT 인증 필요)
     */
    @GetMapping("/info")
    public ResponseEntity<AccountInfoResponse> getAccountInfo(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Long userId = userPrincipal.getId();
        String phoneNumber = userPrincipal.getPhoneNumber();
        
        AccountInfoResponse accountInfo = accountService.getAccountInfo(userId);
        return ResponseEntity.ok(accountInfo);
    }

    /**
     * 입금 API (JWT 인증 필요)
     */
    @PostMapping("/{accountId}/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable Long accountId,
            @RequestBody DepositRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        
        Long userId = userPrincipal.getId();
        
        // 계좌 소유자 검증
        if (!accountService.isAccountOwner(accountId, userId)) {
            return ResponseEntity.status(403).body(
                TransactionResponse.builder()
                    .success(false)
                    .message("해당 계좌에 대한 접근 권한이 없습니다.")
                    .build()
            );
        }
        
        TransactionResponse result = accountService.deposit(accountId, request.getAmount(), request.getDescription());
        return ResponseEntity.ok(result);
    }
} 