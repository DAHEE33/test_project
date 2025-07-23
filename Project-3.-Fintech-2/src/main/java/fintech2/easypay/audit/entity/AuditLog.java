package fintech2.easypay.audit.entity;

import fintech2.easypay.common.AuditResult;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    
    private LocalDateTime createdAt = LocalDateTime.now();
} 