package com.examly.springapp.entity;
import com.examly.springapp.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.*;
import java.time.*;
import java.util.*;
@Entity @Table(name="survey_assignments") @Getter @Setter @NoArgsConstructor
public class SurveyAssignment extends BaseEntity {
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="notification_id",unique=true) private LossNotification notification;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="surveyor_id") private User surveyor;
    private LocalDate assignedDate;
    private LocalDate surveyDate;
    @Column(precision=12,scale=4) private BigDecimal yieldAssessedKgHa;
    @Column(precision=5,scale=2) private BigDecimal lossAssessedPercentage;
    @ElementCollection @CollectionTable(name="survey_photos",joinColumns=@JoinColumn(name="survey_id")) @Column(name="photo_url") private List<String> surveyPhotos=new ArrayList<>();
    @Enumerated(EnumType.STRING) @Column(nullable=false) private SurveyStatus status;
    private String ccePlotId;
    private BigDecimal ccePlotAreaHa;
    private BigDecimal sampleYieldKgHa;
    private BigDecimal satelliteNdvi;
    private BigDecimal satelliteVegetationHealth;
    private BigDecimal surveyGeoLat;
    private BigDecimal surveyGeoLng;
    private String assessmentSource;
}
