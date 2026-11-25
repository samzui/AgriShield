package com.examly.springapp.repository;
import com.examly.springapp.entity.RefreshToken;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {
    Optional<RefreshToken> findByToken(String token);
}
