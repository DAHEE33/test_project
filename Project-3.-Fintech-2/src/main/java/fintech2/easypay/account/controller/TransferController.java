package fintech2.easypay.account.controller;

import fintech2.easypay.account.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 송금 API 컨트롤러
 * 송금 비즈니스 로직을 처리하는 엔드포인트 제공
 */
@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    /**
     * 계좌 간 송금
     * POST /transfers
     */
    @PostMapping
    public ResponseEntity<?> transfer(@RequestBody Map<String, Object> request,
                                    @RequestHeader("Authorization") String token) {
        try {
            String fromAccountNumber = (String) request.get("fromAccountNumber");
            String toAccountNumber = (String) request.get("toAccountNumber");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String description = (String) request.get("description");
            String userId = extractUserIdFromToken(token); // TODO: JWT에서 사용자 ID 추출

            Map<String, Object> result = transferService.transfer(
                fromAccountNumber, toAccountNumber, amount, description, userId);

            return ResponseEntity.ok(result);

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "INVALID_AMOUNT", "message", "잘못된 금액 형식입니다"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "INVALID_REQUEST", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "TRANSFER_FAILED", "message", "송금 처리 중 오류가 발생했습니다"));
        }
    }

    /**
     * 송금 내역 조회
     * GET /transfers/{accountNumber}/history
     */
    @GetMapping("/{accountNumber}/history")
    public ResponseEntity<?> getTransferHistory(@PathVariable String accountNumber,
                                              @RequestHeader("Authorization") String token) {
        try {
            String userId = extractUserIdFromToken(token); // TODO: JWT에서 사용자 ID 추출

            Map<String, Object> result = transferService.getTransferHistory(accountNumber, userId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "HISTORY_FAILED", "message", "송금 내역 조회 중 오류가 발생했습니다"));
        }
    }

    /**
     * JWT 토큰에서 사용자 ID 추출 (임시 구현)
     * TODO: 실제 JWT 서비스를 통해 구현
     */
    private String extractUserIdFromToken(String token) {
        // TODO: JWT 토큰에서 사용자 ID 추출 로직 구현
        return "USER_" + System.currentTimeMillis(); // 임시 구현
    }
} 