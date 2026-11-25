package com.examly.springapp.repository;

import com.examly.springapp.entity.LossNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LossNotificationRepository extends JpaRepository<LossNotification, Long>, JpaSpecificationExecutor<LossNotification> {
    @Query("select l.id, l.lossType, l.affectedAreaHa, l.lossPercentageEstimate, l.notificationDate, l.geoLat, l.geoLng, l.status, p.id, p.policyNumber, p.cropName from LossNotification l join l.policy p where p.farmer.id = :farmerId order by l.notificationDate desc")
    List<Object[]> findListViewByFarmerId(@Param("farmerId") Long farmerId);

    @Query("select l.id, l.lossType, l.affectedAreaHa, l.lossPercentageEstimate, l.notificationDate, l.geoLat, l.geoLng, l.status, p.id, p.policyNumber, p.cropName from LossNotification l join l.policy p order by l.notificationDate desc")
    List<Object[]> findAllListViews();
}
