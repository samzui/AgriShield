package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="login_attempts") public class LoginAttempt extends BaseEntity {
    private String usernameOrEmail;
    private String ipAddress;
    private Instant attemptedAt;
    private boolean successful;
}
