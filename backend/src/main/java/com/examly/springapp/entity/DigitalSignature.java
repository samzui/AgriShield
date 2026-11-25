package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="digital_signatures")
public class DigitalSignature extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="document_id") private Document document;
    @Column(nullable=false) private Long signerUserId;
    @Column(nullable=false,length=256) private String signatureReference;
    @Column(nullable=false) private Instant signedAt;
    private String algorithm;
}
