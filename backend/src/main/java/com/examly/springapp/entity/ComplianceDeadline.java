package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="compliance_deadlines")
public class ComplianceDeadline extends BaseEntity {
    @Column(nullable=false,length=160) private String title;
    @Column(nullable=false,length=80) private String regulator;
    @Column(nullable=false) private LocalDate dueDate;
    private boolean completed;
    private String referenceNumber;
    private String notes;
}
