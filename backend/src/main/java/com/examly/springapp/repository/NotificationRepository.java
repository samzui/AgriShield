package com.examly.springapp.repository;
import com.examly.springapp.entity.Notification;
import com.examly.springapp.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface NotificationRepository extends JpaRepository<Notification,Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByStatusIn(Collection<NotificationStatus> statuses);
}
