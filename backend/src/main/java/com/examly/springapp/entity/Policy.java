package com.examly.springapp.entity;
import com.examly.springapp.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.*;
import java.time.*;
@Entity @Table(name="policies",indexes= {
    @Index(name="idx_policy_farmer",columnList="farmer_id"),@Index(name="idx_policy_status",columnList="status")
}) @Getter @Setter @NoArgsConstructor
public class Policy extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="farmer_id") private User farmer;
    @Column(nullable=false) private String cropName;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Season season;
    @Column(nullable=false,precision=12,scale=4) private BigDecimal sownAreaHa;
    @Convert(converter=com.examly.springapp.util.Aes256Converter.class)
    @Column(nullable=false,length=512) private String bankAccountNo;
    @Column(length=100) private String bankHolderName;
    @Column(length=100) private String bankName;
    @Column(length=11) private String bankIfsc;
    @Column(length=200) private String bankBranch;
    @Column(nullable=false,precision=15,scale=2) private BigDecimal sumInsured;
    @Column(nullable=false,precision=15,scale=2) private BigDecimal premiumFarmer;
    @Column(nullable=false,precision=15,scale=2) private BigDecimal premiumState;
    @Column(nullable=false,precision=15,scale=2) private BigDecimal premiumCentre;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private PolicyStatus status;
    @Column(nullable=false) private LocalDate enrollmentDate;
    @Column(length=80) private String policyNumber;
    private String premiumRateCode;
}
