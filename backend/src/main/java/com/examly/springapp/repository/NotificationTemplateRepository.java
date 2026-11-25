package com.examly.springapp.repository;
import com.examly.springapp.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate,Long> {
    Optional<NotificationTemplate> findByCode(String code);
    boolean existsByCode(String code);
}
