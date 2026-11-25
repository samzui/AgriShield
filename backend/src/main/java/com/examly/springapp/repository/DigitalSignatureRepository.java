package com.examly.springapp.repository;
import com.examly.springapp.entity.DigitalSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DigitalSignatureRepository extends JpaRepository<DigitalSignature,Long> {
    List<DigitalSignature> findByDocumentId(Long documentId);
}
