package com.examly.springapp.entity;
import com.examly.springapp.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="two_factor_verifications")
public class TwoFactorVerification extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id") private User user;
    @Column(nullable=false,length=40) private String operation;
    @Column(nullable=false,unique=true,length=128) private String challengeId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private VerificationStatus status=VerificationStatus.PENDING;
    @Column(nullable=false) private Instant expiresAt;
}
