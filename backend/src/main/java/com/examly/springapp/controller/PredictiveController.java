package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.service.AnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/analytics/predictive")
@PreAuthorize("hasAnyRole('INSURANCE_OFFICER','STATE_OFFICER','ADMIN')")
public class PredictiveController {
    private final AnalyticsService service;
    public PredictiveController(AnalyticsService service) {
        this.service=service;
    }
    @GetMapping("/forecast") public ApiResponse<?> forecast(@RequestParam(defaultValue="7")int horizon) {
        return ApiResponse.ok("Demand forecast",service.forecastDemand(horizon));
    }
}
