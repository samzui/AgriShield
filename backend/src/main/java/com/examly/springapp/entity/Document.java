package com.examly.springapp.entity;
import com.examly.springapp.enums.DocumentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="documents") public class Document extends BaseEntity {
    private String entityType;
    private Long entityId;
    private String category;
    private String sensitivity;
    private String originalFilename;
    private String storageKey;
    private LocalDate expiryDate;
    private LocalDate retentionDate;
    private String mimeType;
    private long sizeBytes;
    private String checksum;
    @Enumerated(EnumType.STRING) private DocumentStatus status=DocumentStatus.ACTIVE;
    private String retentionPolicyCode;
    private boolean signed;
}
