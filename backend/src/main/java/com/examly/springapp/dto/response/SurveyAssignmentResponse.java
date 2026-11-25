package com.examly.springapp.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SurveyAssignmentResponse(
        Long id,
        Instant createdAt,
        Instant updatedAt,
        Long notificationId,
        String farmerName,
        String cropName,
        String location,
        LocalDate assignedDate,
        LocalDate surveyDate,
        BigDecimal yieldAssessedKgHa,
        BigDecimal lossAssessedPercentage,
        List<String> surveyPhotos,
        String status,
        String ccePlotId,
        BigDecimal ccePlotAreaHa,
        BigDecimal sampleYieldKgHa,
        BigDecimal satelliteNdvi,
        BigDecimal satelliteVegetationHealth,
        BigDecimal surveyGeoLat,
        BigDecimal surveyGeoLng,
        String assessmentSource
) {}
