package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.integration.SatelliteAssessmentProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
@RestController @RequestMapping("/api/satellite") @PreAuthorize("hasAnyRole('SURVEYOR','INSURANCE_OFFICER','STATE_OFFICER','ADMIN')")
public class SatelliteController {
    private final SatelliteAssessmentProvider provider;
    public SatelliteController(SatelliteAssessmentProvider provider) {
        this.provider=provider;
    }
    @GetMapping("/assessment") public ApiResponse<?> assessment(@RequestParam BigDecimal lat,@RequestParam BigDecimal lng) {
        return ApiResponse.ok("Satellite assessment",provider.assess(lat,lng));
    }
}
