package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.entity.ComplianceDeadline;
import com.examly.springapp.service.ComplianceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/compliance") @PreAuthorize("hasRole('ADMIN')")
public class ComplianceController {
    private final ComplianceService service;
    public ComplianceController(ComplianceService service) {
        this.service=service;
    }
    @GetMapping("/calendar") public ApiResponse<?> calendar(@RequestParam(defaultValue="90")int days) {
        return ApiResponse.ok("Compliance calendar",service.upcoming(days));
    }
    @PostMapping("/deadlines") public ApiResponse<?> create(@RequestBody ComplianceDeadline d) {
        return ApiResponse.ok("Deadline created",service.create(d));
    }
    @PutMapping("/deadlines/{id}/complete") public ApiResponse<?> complete(@PathVariable Long id) {
        return ApiResponse.ok("Deadline completed",service.complete(id));
    }
}
