package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.service.HealthIntegrationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/integrations") @PreAuthorize("hasRole('ADMIN')") public class HealthIntegrationController {
    private final HealthIntegrationService s;
    public HealthIntegrationController(HealthIntegrationService s) {
        this.s=s;
    }@GetMapping("/status")public ApiResponse<?> status() {
        return ApiResponse.ok("Integration status",s.status());
    }
}
