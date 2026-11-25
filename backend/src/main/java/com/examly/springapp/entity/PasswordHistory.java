package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="password_history") public class PasswordHistory extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private User user;
    @Column(nullable=false) private String passwordHash;
    @Column(nullable=false) private Instant changedAt;
}
