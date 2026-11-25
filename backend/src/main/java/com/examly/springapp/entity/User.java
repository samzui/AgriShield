package com.examly.springapp.entity;
import com.examly.springapp.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Table(name="users",indexes= {
    @Index(name="idx_user_email",columnList="email"),@Index(name="idx_user_phone",columnList="phoneNumber")
}) @Getter @Setter @NoArgsConstructor
public class User extends BaseEntity {
    @Column(nullable=false,length=100) private String name;
    @Column(nullable=false,unique=true,length=50) private String userId;
    @Column(nullable=false,unique=true,length=10) private String phoneNumber;
    @Column(nullable=false,unique=true,length=150) private String email;
    @Column(nullable=false) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Role role;
    private Instant lastLogin;
    @Column(nullable=false) private boolean isActive=true;
    @Column(nullable=false) private boolean emailVerified=false;
    @Column(nullable=false) private boolean supervisorApproved=false;
    private Instant lockedUntil;
    private Instant passwordChangedAt;
    private Instant passwordExpiresAt;
    private String assignmentScope;
    private boolean twoFactorEnabled;
    private Instant lastActivityAt;
}
