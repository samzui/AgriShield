package com.examly.springapp.entity;
import com.examly.springapp.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="notifications") public class Notification extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private User user;
    private String eventType;
    @Lob private String message;
    @Enumerated(EnumType.STRING) private NotificationChannel channel;
    @Enumerated(EnumType.STRING) private NotificationStatus status=NotificationStatus.PENDING;
    private int retryCount;
    private Instant sentAt;
    private String templateCode;
    private Instant nextAttemptAt;
    private String escalationLevel;
}
