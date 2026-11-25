package com.examly.springapp.repository;
import com.examly.springapp.entity.Workflow;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface WorkflowRepository extends JpaRepository<Workflow,Long> {
}
