package com.examly.springapp.repository;
import com.examly.springapp.entity.ExternalIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ExternalIntegrationRepository extends JpaRepository<ExternalIntegration,Long> {
    Optional<ExternalIntegration> findByCode(String code);
}
