package com.examly.springapp.repository;
import com.examly.springapp.entity.PasswordHistory;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory,Long> {
}
