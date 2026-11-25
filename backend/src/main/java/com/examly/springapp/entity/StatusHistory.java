package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="status_history") public class StatusHistory extends BaseEntity {
    private String entityType;
    private Long entityId;
    private String previousStatus;
    private String newStatus;
    private Long changedBy;
    private Instant changedAt;
    @Lob private String reason;
}
