package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name="audit_logs", indexes= {
    @Index(name="idx_audit_user_time", columnList="userId,timestamp"),
    @Index(name="idx_audit_entity", columnList="entityType,entityId"),
    @Index(name="idx_audit_action_time", columnList="action,timestamp")
})
public class AuditLog extends BaseEntity {
    private Long userId;
    @Column(nullable=false,length=80) private String action;
    @Column(length=80) private String entityType;
    @Column(length=120) private String entityId;
    @Column(nullable=false) private Instant timestamp;
    @Column(length=64) private String ipAddress;
    @Column(length=120) private String purpose;
    @Column(columnDefinition="TEXT") private String previousValue;
    @Column(columnDefinition="TEXT") private String newValue;
    @Column(nullable=false,length=30) private String result;
    @Column(columnDefinition="TEXT") private String metadata;
    @Column(nullable=false,length=64) private String eventHash;
    @PreUpdate
    @PreRemove
    private void preventMutation() {
        throw new IllegalStateException("Audit logs are append-only");
    }
}
