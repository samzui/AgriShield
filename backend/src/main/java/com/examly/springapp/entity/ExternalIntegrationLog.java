package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="external_integration_logs") public class ExternalIntegrationLog extends BaseEntity {
    private String systemName;
    private String operation;
    private String status;
    private int attempts;
    private Instant startedAt;
    private Instant completedAt;
    @Lob private String errorMessage;
    private String requestId;
    private String environment;
    private long latencyMs;
    private String httpStatus;
}
