package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="notification_templates",uniqueConstraints=@UniqueConstraint(columnNames="code"))
public class NotificationTemplate extends BaseEntity {
    @Column(nullable=false,unique=true,length=80) private String code;
    @Column(nullable=false,length=160) private String subject;
    @Lob @Column(nullable=false) private String body;
    private boolean active=true;
}
