package com.examly.springapp.repository;
import com.examly.springapp.entity.Claim;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface ClaimRepository extends JpaRepository<Claim,Long>, JpaSpecificationExecutor<Claim> {
    Optional<Claim> findBySurveyId(Long surveyId);
}
