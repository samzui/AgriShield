package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="scheduled_reports")
public class ScheduledReport extends BaseEntity {
    @Column(nullable=false,length=120) private String name;
    @Column(nullable=false,length=40) private String reportType;
    @Column(nullable=false,length=20) private String format;
    @Column(nullable=false,length=1000) private String recipientList;
    @Column(nullable=false,length=120) private String cronExpression;
    private boolean active=true;
}
