package com.examly.springapp.repository;
import com.examly.springapp.entity.ScheduledReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ScheduledReportRepository extends JpaRepository<ScheduledReport,Long> {
    List<ScheduledReport> findByActiveTrue();
}
