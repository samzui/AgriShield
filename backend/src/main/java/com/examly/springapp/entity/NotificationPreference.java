package com.examly.springapp.entity;
import com.examly.springapp.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="notification_preferences",uniqueConstraints=@UniqueConstraint(columnNames= {
    "user_id","channel"
})) public class NotificationPreference extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private User user;
    @Enumerated(EnumType.STRING) private NotificationChannel channel;
    private boolean enabled=true;
}
