package com.examly.springapp.repository;
import com.examly.springapp.entity.SystemConfiguration;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration,Long> {
}
