package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="refresh_tokens",indexes=@Index(name="idx_refresh_token",columnList="token"))
public class RefreshToken extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private User user;
    @Column(nullable=false,unique=true,length=512) private String token;
    @Column(nullable=false) private Instant expiresAt;
    @Column(nullable=false) private boolean revoked=false;
}
