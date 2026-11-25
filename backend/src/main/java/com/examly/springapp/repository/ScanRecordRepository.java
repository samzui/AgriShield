package com.examly.springapp.repository;

import com.examly.springapp.entity.ScanRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanRecordRepository extends JpaRepository<ScanRecord, Long> {
}
