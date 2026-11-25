package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="workflow_stages") public class WorkflowStage extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private Workflow workflow;
    private int stageOrder;
    private String stageName;
    private String requiredRole;
    private int slaHours;
    private boolean completed;
    private Long completedBy;
}
