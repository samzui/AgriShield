package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="permissions",uniqueConstraints=@UniqueConstraint(columnNames="code")) public class Permission extends BaseEntity {
    @Column(nullable=false,unique=true) private String code;
    private String description;
}
