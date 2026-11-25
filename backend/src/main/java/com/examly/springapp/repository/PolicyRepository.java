package com.examly.springapp.repository;
import com.examly.springapp.entity.Policy;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface PolicyRepository extends JpaRepository<Policy,Long>, JpaSpecificationExecutor<Policy> {
    List<Policy> findByFarmerId(Long id);
}
