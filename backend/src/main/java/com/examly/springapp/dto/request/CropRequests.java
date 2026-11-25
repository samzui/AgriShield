package com.examly.springapp.dto.request;
import com.examly.springapp.enums.Season;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public final class CropRequests {
    private CropRequests() {
    }
    public record CropRequest(@NotBlank String cropName,@NotNull Season season,@NotNull @DecimalMin("0.01") BigDecimal standardYieldKgHa,@NotNull @DecimalMin("0.01") BigDecimal mspRate) {
    }
}
