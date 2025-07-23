package fintech2.easypay.auth.entity;

import fintech2.easypay.account.entity.VirtualAccount;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @Builder @AllArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private String accountNumber; // 가상계좌번호 (1:1)

    // 계정 잠금 관련 필드
    private Integer loginFailCount = 0;
    private boolean isLocked = false;
    private LocalDateTime lockExpiresAt;
    private String lockReason;

    // VirtualAccount 객체 반환 (호환성을 위해)
    public VirtualAccount getVirtualAccount() {
        if (accountNumber != null) {
            return VirtualAccount.builder()
                .accountNumber(accountNumber)
                .build();
        }
        return null;
    }

    public void incrementLoginFailCount() {
        this.loginFailCount++;
        if (this.loginFailCount >= 5) {
            this.isLocked = true;
            this.lockExpiresAt = LocalDateTime.now().plusMinutes(30);
            this.lockReason = "로그인 5회 실패로 인한 계정 잠금";
        }
    }

    public void resetLoginFailCount() {
        this.loginFailCount = 0;
        this.isLocked = false;
        this.lockExpiresAt = null;
        this.lockReason = null;
    }

    public boolean isAccountLocked() {
        if (!isLocked) return false;
        if (lockExpiresAt != null && LocalDateTime.now().isAfter(lockExpiresAt)) {
            resetLoginFailCount();
            return false;
        }
        return true;
    }
} 