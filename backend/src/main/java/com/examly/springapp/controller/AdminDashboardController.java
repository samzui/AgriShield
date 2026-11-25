package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.service.AnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/dashboard") @PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {
    private final AnalyticsService analytics;
    public AdminDashboardController(AnalyticsService analytics) {
        this.analytics=analytics;
    }
    @GetMapping public ApiResponse<?> dashboard() {
        return ApiResponse.ok("Admin dashboard",analytics.dashboard());
    }
}
