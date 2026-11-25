package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="integration_dead_letters")
public class IntegrationDeadLetter extends BaseEntity {
    @Column(nullable=false,length=80) private String integrationCode;
    @Lob @Column(nullable=false) private String payload;
    @Column(nullable=false,length=80) private String errorType;
    @Column(nullable=false) private Instant failedAt;
    private boolean resolved;
}
