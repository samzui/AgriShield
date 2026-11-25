package com.examly.springapp.dto.request;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
public final class SurveyRequests {
    private SurveyRequests() {
    } public record AssignRequest(@NotNull Long notificationId,@NotNull Long surveyorId,@NotNull LocalDate surveyDate) {
    } public record SubmitRequest(@NotNull @DecimalMin("0") BigDecimal yieldAssessedKgHa,@NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal lossAssessedPercentage,List<String> surveyPhotos) {
    }
}
