package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="blacklisted_tokens") public class BlacklistedToken extends BaseEntity {
    @Column(nullable=false,unique=true,length=600) private String token;
    @Column(nullable=false) private Instant expiresAt;
    @Column(nullable=false) private String reason;
}
