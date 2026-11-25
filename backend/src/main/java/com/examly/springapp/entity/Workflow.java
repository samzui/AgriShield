package com.examly.springapp.entity;
import com.examly.springapp.enums.WorkflowStatus;
import jakarta.persistence.*;
import lombok.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="workflows") public class Workflow extends BaseEntity {
    private String entityType;
    private Long entityId;
    private String workflowType;
    @Enumerated(EnumType.STRING) private WorkflowStatus status;
    private int currentStage;
}
