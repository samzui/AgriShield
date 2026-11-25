package com.examly.springapp.dto.request;
import com.examly.springapp.enums.Season;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public final class PolicyRequests {
    private PolicyRequests() {
    } public record EnrollRequest(@NotNull Long farmerId,@NotBlank String cropName,@NotNull Season season,@NotNull @DecimalMin("0.0001") BigDecimal sownAreaHa,@NotBlank String bankAccountNo,String bankHolderName,String bankName,String bankIfsc,String bankBranch,@NotNull @DecimalMin("0.01") BigDecimal sumInsured) {
    }
}
