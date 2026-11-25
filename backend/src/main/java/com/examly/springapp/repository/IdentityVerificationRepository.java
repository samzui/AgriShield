package com.examly.springapp.repository;
import com.examly.springapp.entity.IdentityVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface IdentityVerificationRepository extends JpaRepository<IdentityVerification,Long> {
    Optional<IdentityVerification> findByUserId(Long userId);
}
