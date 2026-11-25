package com.examly.springapp.repository;

import com.examly.springapp.entity.SurveyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SurveyAssignmentRepository extends JpaRepository<SurveyAssignment, Long> {

    @Query("""
        select distinct s
        from SurveyAssignment s
        left join fetch s.notification n
        left join fetch n.policy p
        left join fetch p.farmer f
        left join fetch s.surveyor sv
        left join fetch s.surveyPhotos photos
        where sv.id = :surveyorId
        order by s.surveyDate desc
    """)
    List<SurveyAssignment> findAssignmentsForSurveyor(@Param("surveyorId") Long surveyorId);

    @Query("""
        select distinct s
        from SurveyAssignment s
        left join fetch s.notification n
        left join fetch n.policy p
        left join fetch p.farmer f
        left join fetch s.surveyor sv
        left join fetch s.surveyPhotos photos
        order by s.surveyDate desc
    """)
    List<SurveyAssignment> findAllWithDetails();

    @Query("""
        select distinct s
        from SurveyAssignment s
        left join fetch s.notification n
        left join fetch n.policy p
        left join fetch p.farmer f
        left join fetch s.surveyor sv
        left join fetch s.surveyPhotos photos
        where f.id = :farmerId
        order by s.surveyDate desc
    """)
    List<SurveyAssignment> findAssignmentsForFarmer(@Param("farmerId") Long farmerId);

    @Query("""
        select distinct s
        from SurveyAssignment s
        left join fetch s.notification n
        left join fetch n.policy p
        left join fetch p.farmer f
        left join fetch s.surveyor sv
        left join fetch s.surveyPhotos photos
        where s.id = :id
    """)
    Optional<SurveyAssignment> findByIdWithDetails(@Param("id") Long id);

    List<SurveyAssignment> findBySurveyorId(Long id);

    Optional<SurveyAssignment> findByNotificationId(Long id);
}
