package com.examly.springapp.repository;
import com.examly.springapp.entity.ComplianceDeadline;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
public interface ComplianceDeadlineRepository extends JpaRepository<ComplianceDeadline,Long> {
    List<ComplianceDeadline> findByDueDateBetween(LocalDate from,LocalDate to);
}
