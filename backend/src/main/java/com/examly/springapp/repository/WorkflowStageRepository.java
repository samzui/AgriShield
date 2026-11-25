package com.examly.springapp.repository;
import com.examly.springapp.entity.WorkflowStage;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface WorkflowStageRepository extends JpaRepository<WorkflowStage,Long> {
}
