package com.examly.springapp.repository;
import com.examly.springapp.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion,Long> {
    List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(Long id);
    Optional<DocumentVersion> findByDocumentIdAndVersionNumber(Long id,int version);
}
