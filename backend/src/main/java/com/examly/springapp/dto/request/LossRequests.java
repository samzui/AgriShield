package com.examly.springapp.dto.request;
import com.examly.springapp.enums.LossType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
public final class LossRequests {
    private LossRequests() {
    } public record CreateRequest(@NotNull Long policyId,@NotNull LossType lossType,@NotNull @DecimalMin("0.0001") BigDecimal affectedAreaHa,@DecimalMin("0") @DecimalMax("100") BigDecimal lossPercentageEstimate,@NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal geoLat,@NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal geoLng,List<String> photoUrls) {
    }
}
