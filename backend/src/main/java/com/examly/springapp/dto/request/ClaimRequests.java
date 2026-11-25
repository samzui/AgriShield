package com.examly.springapp.dto.request;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public final class ClaimRequests {
    private ClaimRequests() {
    } public record InitiateRequest(@NotNull Long policyId,@NotNull Long surveyId,@NotNull @DecimalMin("0.01") BigDecimal claimedAmount) {
    } public record ApproveRequest(@NotNull @DecimalMin("0.01") BigDecimal approvedAmount, String reason) {
    } public record DisburseRequest(@NotBlank String dbtUtr) {
    }
}
