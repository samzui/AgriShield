package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="scan_records")
public class ScanRecord extends BaseEntity {
    @Column(nullable=false,length=40) private String scanType;
    @Column(nullable=false,length=256) private String payloadReference;
    @Column(nullable=false,length=30) private String status;
    private String result;
    private Instant scannedAt;
}
