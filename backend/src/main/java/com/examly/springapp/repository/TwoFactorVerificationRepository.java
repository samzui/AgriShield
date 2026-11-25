package com.examly.springapp.repository;
import com.examly.springapp.entity.TwoFactorVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TwoFactorVerificationRepository extends JpaRepository<TwoFactorVerification,Long> {
    Optional<TwoFactorVerification> findByChallengeId(String challengeId);
}
