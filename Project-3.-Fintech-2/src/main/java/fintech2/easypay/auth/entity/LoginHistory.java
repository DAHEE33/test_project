package fintech2.easypay.auth.entity;

import fintech2.easypay.common.LoginResult;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginResult result;
    
    @Column(nullable = false)
    private String ipAddress;
    
    @Column
    private String userAgent;
    
    @Column
    private String failureReason; // 실패 사유
    
    @Column
    private Integer failCount; // 현재 실패 횟수
    
    @Column
    private Boolean isLocked; // 잠금 상태
    
    @CreationTimestamp
    private LocalDateTime createdAt;
} 