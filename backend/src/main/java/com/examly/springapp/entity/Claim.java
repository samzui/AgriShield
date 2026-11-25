package com.examly.springapp.entity;
import com.examly.springapp.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.*;
import java.time.*;
@Entity @Table(name="claims",indexes= {
    @Index(name="idx_claim_status",columnList="status")
}) @Getter @Setter @NoArgsConstructor
public class Claim extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="policy_id") private Policy policy;
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="survey_id",unique=true) private SurveyAssignment survey;
    @Column(nullable=false,precision=15,scale=2) private BigDecimal claimedAmount;
    @Column(precision=15,scale=2) private BigDecimal approvedAmount;
    @Column(length=100) private String dbtUtr;
    private LocalDate disbursedDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private ClaimStatus status;
}
