package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@MappedSuperclass @Getter @Setter
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,updatable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    @PrePersist void prePersist() {
        Instant n=Instant.now();
        createdAt=n;
        updatedAt=n;
    }
    @PreUpdate void preUpdate() {
        updatedAt=Instant.now();
    }
}
