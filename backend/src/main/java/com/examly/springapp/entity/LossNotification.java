package com.examly.springapp.entity;
import com.examly.springapp.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.*;
import java.time.*;
import java.util.*;
@Entity @Table(name="loss_notifications") @Getter @Setter @NoArgsConstructor
public class LossNotification extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="policy_id") private Policy policy;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private LossType lossType;
    @Column(nullable=false,precision=12,scale=4) private BigDecimal affectedAreaHa;
    @Column(precision=5,scale=2) private BigDecimal lossPercentageEstimate;
    @Column(nullable=false) private Instant notificationDate;
    @Column(precision=10,scale=7) private BigDecimal geoLat;
    @Column(precision=10,scale=7) private BigDecimal geoLng;
    @ElementCollection @CollectionTable(name="loss_notification_photos",joinColumns=@JoinColumn(name="notification_id")) @Column(name="photo_url") private List<String> photoUrls=new ArrayList<>();
    @Enumerated(EnumType.STRING) @Column(nullable=false) private LossNotificationStatus status;
}
