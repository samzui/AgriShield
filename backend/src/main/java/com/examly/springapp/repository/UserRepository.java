package com.examly.springapp.repository;
import com.examly.springapp.entity.User;
import com.examly.springapp.enums.Role;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByUserId(String userId);
    Optional<User> findByPhoneNumber(String phone);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByUserId(String userId);
    boolean existsByPhoneNumber(String phone);
    List<User> findByRoleAndIsActiveTrue(Role role);
}
