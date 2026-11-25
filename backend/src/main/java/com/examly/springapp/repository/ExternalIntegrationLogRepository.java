package com.examly.springapp.repository;
import com.examly.springapp.entity.ExternalIntegrationLog;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ExternalIntegrationLogRepository extends JpaRepository<ExternalIntegrationLog,Long> {
}
