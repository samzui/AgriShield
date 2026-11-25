package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="system_configurations",uniqueConstraints=@UniqueConstraint(columnNames="configKey")) public class SystemConfiguration extends BaseEntity {
    @Column(nullable=false,unique=true) private String configKey;
    @Lob private String configValue;
    private boolean encrypted;
}
