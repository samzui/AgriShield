package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasAnyRole('INSURANCE_OFFICER','STATE_OFFICER','ADMIN')")
public class AnalyticsController {
    private final AnalyticsService service;
    public AnalyticsController(AnalyticsService service) {
        this.service=service;
    }
    @GetMapping("/dashboard") public ApiResponse<?> dashboard() {
        return ApiResponse.ok("Operational KPI dashboard",service.dashboard());
    }
    @GetMapping("/claim-ratio") public ApiResponse<?> claimRatio() {
        return ApiResponse.ok("Claim ratio",service.dashboard().claimRatio());
    }
    @GetMapping("/loss-patterns") public ApiResponse<?> lossPatterns() {
        return ApiResponse.ok("Loss patterns",service.lossPatterns());
    }
    @GetMapping("/trends") public ApiResponse<?> trends(@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to) {
        return ApiResponse.ok("Trends",service.trends(from,to));
    }
    @GetMapping("/benchmarks") public ApiResponse<?> benchmarks() {
        return ApiResponse.ok("Benchmarks",service.benchmarks());
    }
    @GetMapping("/anomalies") public ApiResponse<?> anomalies() {
        return ApiResponse.ok("Anomalies",service.anomalies());
    }
    @GetMapping("/funnel") public ApiResponse<?> funnel() {
        return ApiResponse.ok("Funnel",service.funnel());
    }
    @GetMapping("/weekly-insight") public ApiResponse<?> weeklyInsight() {
        return ApiResponse.ok("Weekly insight",service.weeklyInsight());
    }
}
