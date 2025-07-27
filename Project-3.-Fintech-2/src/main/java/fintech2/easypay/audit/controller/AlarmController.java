package fintech2.easypay.audit.controller;

import fintech2.easypay.audit.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getNotificationCount(@AuthenticationPrincipal String userPrincipal) {
        try {
            // 실제로는 사용자별 알림 개수를 DB에서 조회해야 함
            // 현재는 모의 데이터로 구현
            int count = alarmService.getUnreadNotificationCount(userPrincipal);
            
            Map<String, Object> response = new HashMap<>();
            response.put("count", count);
            response.put("success", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("count", 0);
            response.put("success", false);
            response.put("message", "알림 개수 조회에 실패했습니다");
            
            return ResponseEntity.ok(response);
        }
    }
} 