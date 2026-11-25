package com.examly.springapp.entity;
import com.examly.springapp.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="external_integrations",uniqueConstraints=@UniqueConstraint(columnNames="code"))
public class ExternalIntegration extends BaseEntity {
    @Column(nullable=false,unique=true,length=80) private String code;
    @Column(nullable=false,length=160) private String displayName;
    private String baseUrl;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private IntegrationAuthType authType=IntegrationAuthType.NONE;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private IntegrationStatus status=IntegrationStatus.DISABLED;
    private Instant lastCheckedAt;
    private long failureCount;
    private String environment;
}
