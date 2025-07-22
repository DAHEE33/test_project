package fintech2.easypay.auth.entity;

import fintech2.easypay.account.entity.VirtualAccount;
import fintech2.easypay.common.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private VirtualAccount virtualAccount;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime lastLoginAt; // 마지막 로그인 시간

    private LocalDateTime withdrawnAt; // 탈퇴 시간

    @Builder.Default
    private Integer loginFailCount = 0; // 로그인 실패 횟수

    private LocalDateTime accountLockedAt; // 계정 잠금 시간
    
    private LocalDateTime lockExpiresAt; // 잠금 만료 시간
    
    private String lockReason; // 잠금 사유
    
    @Builder.Default
    private Boolean isLocked = false; // 잠금 상태

    /**
     * 계정 잠금 처리
     */
    public void lockAccount(String reason, int lockMinutes) {
        this.isLocked = true;
        this.accountLockedAt = LocalDateTime.now();
        this.lockExpiresAt = LocalDateTime.now().plusMinutes(lockMinutes);
        this.lockReason = reason;
    }

    /**
     * 계정 잠금 해제
     */
    public void unlockAccount() {
        this.isLocked = false;
        this.accountLockedAt = null;
        this.lockExpiresAt = null;
        this.lockReason = null;
        this.loginFailCount = 0;
    }

    /**
     * 잠금 상태 확인
     */
    public boolean isAccountLocked() {
        if (!this.isLocked) {
            return false;
        }
        
        // 잠금 만료 시간이 지났으면 자동 해제
        if (this.lockExpiresAt != null && LocalDateTime.now().isAfter(this.lockExpiresAt)) {
            unlockAccount();
            return false;
        }
        
        return true;
    }

    /**
     * 로그인 실패 처리
     */
    public void incrementLoginFailCount() {
        this.loginFailCount++;
        
        // 5회 실패 시 30분 잠금
        if (this.loginFailCount >= 5) {
            lockAccount("로그인 5회 연속 실패", 30);
        }
    }

    /**
     * 로그인 성공 처리
     */
    public void resetLoginFailCount() {
        this.loginFailCount = 0;
        this.lastLoginAt = LocalDateTime.now();
    }
} 