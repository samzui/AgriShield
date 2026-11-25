package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Getter @Setter @NoArgsConstructor
@Table(name="device_registrations", uniqueConstraints=@UniqueConstraint(columnNames= {
    "user_id","device_fingerprint"
}))
public class DeviceRegistration extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id") private User user;
    @Column(name="device_fingerprint", nullable=false, length=128) private String deviceFingerprint;
    private String deviceName;
    private String ipAddress;
    private Instant lastSeenAt;
    private boolean trusted;
}
