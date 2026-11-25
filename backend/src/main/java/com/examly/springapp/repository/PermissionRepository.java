package com.examly.springapp.repository;
import com.examly.springapp.entity.Permission;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface PermissionRepository extends JpaRepository<Permission,Long> {
}
