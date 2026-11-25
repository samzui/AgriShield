package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="document_versions") public class DocumentVersion extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private Document document;
    private int versionNumber;
    private String storageKey;
    private String checksum;
    private Long uploadedBy;
    private Instant uploadedAt;
    private String comment;
}
