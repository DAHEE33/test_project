package fintech2.easypay.audit.entity;

import fintech2.easypay.common.AuditResult;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private String action;
    private String resourceType;
    private String resourceId;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    private String ipAddress;
    private String userAgent;

    @Enumerated(EnumType.STRING)
    private AuditResult result;

    @CreationTimestamp
    private LocalDateTime createdAt;
} 