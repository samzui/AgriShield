package com.examly.springapp.repository;
import com.examly.springapp.entity.OtpVerification;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface OtpVerificationRepository extends JpaRepository<OtpVerification,Long> {
}
