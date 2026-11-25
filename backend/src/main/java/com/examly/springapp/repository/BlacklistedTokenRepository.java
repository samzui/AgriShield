package com.examly.springapp.repository;
import com.examly.springapp.entity.BlacklistedToken;
import org.springframework.data.jpa.repository.*;
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken,Long> {
    boolean existsByToken(String token);
}
