package com.examly.springapp.repository;
import com.examly.springapp.entity.IntegrationDeadLetter;
import org.springframework.data.jpa.repository.JpaRepository;
public interface IntegrationDeadLetterRepository extends JpaRepository<IntegrationDeadLetter,Long> {
}
