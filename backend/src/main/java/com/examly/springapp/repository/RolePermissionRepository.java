package com.examly.springapp.repository;
import com.examly.springapp.entity.RolePermission;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface RolePermissionRepository extends JpaRepository<RolePermission,Long> {
}
