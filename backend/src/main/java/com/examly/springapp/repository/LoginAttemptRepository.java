package com.examly.springapp.repository;
import com.examly.springapp.entity.LoginAttempt;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt,Long> {
}
