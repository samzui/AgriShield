package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="otp_verifications") public class OtpVerification extends BaseEntity {
    @Column(nullable=false) private String purpose;
    @Column(nullable=false) private String target;
    @Column(nullable=false) private String otpHash;
    @Column(nullable=false) private Instant expiresAt;
    @Column(nullable=false) private boolean used=false;
    private int attempts;
}
