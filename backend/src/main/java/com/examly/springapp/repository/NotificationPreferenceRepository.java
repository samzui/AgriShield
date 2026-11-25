package com.examly.springapp.repository;
import com.examly.springapp.entity.NotificationPreference;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference,Long> {
}
