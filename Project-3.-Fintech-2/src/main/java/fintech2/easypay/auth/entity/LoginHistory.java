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

    private String phoneNumber;
    private Long userId;
    private String userAgent;
    private String ipAddress;

    @Enumerated(EnumType.STRING)
    private LoginResult result;

    private String failReason;
    private Integer failCount;
    private boolean isLocked;

    @CreationTimestamp
    private LocalDateTime loginAt;
} 