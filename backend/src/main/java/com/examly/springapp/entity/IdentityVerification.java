package com.examly.springapp.entity;
import com.examly.springapp.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="identity_verifications", uniqueConstraints=@UniqueConstraint(columnNames="user_id"))
public class IdentityVerification extends BaseEntity {
    @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id",unique=true) private User user;
    @Column(nullable=false,length=64) private String documentType;
    @Column(nullable=false,length=256) private String documentReferenceHash;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private VerificationStatus status=VerificationStatus.PENDING;
    private Instant verifiedAt;
    private String providerReference;
}
