package com.examly.springapp.repository;
import com.examly.springapp.entity.Document;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface DocumentRepository extends JpaRepository<Document,Long> {
}
