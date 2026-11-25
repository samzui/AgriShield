package com.examly.springapp.entity;
import com.examly.springapp.enums.Role;
import jakarta.persistence.*;
import lombok.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="role_permissions",uniqueConstraints=@UniqueConstraint(columnNames= {
    "role","permission_id"
})) public class RolePermission extends BaseEntity {
    @Enumerated(EnumType.STRING) private Role role;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private Permission permission;
    private boolean enabled=true;
}
