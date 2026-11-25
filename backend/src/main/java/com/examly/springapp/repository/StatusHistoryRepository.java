package com.examly.springapp.repository;
import com.examly.springapp.entity.StatusHistory;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface StatusHistoryRepository extends JpaRepository<StatusHistory,Long> {
    List<StatusHistory> findByEntityTypeAndEntityIdOrderByChangedAtAsc(String entityType, Long entityId);
}
