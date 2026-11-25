package com.examly.springapp.controller;
import com.examly.springapp.dto.request.IntegrationRequests.*;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.service.IntegrationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/integrations") @PreAuthorize("hasRole('ADMIN')")
public class IntegrationController {
    private final IntegrationService service;
    public IntegrationController(IntegrationService service) {
        this.service=service;
    }
    @GetMapping public ApiResponse<?> status() {
        return ApiResponse.ok("Integrations",service.status());
    }
    @PostMapping public ApiResponse<?> register(@RequestParam String code,@RequestParam String name,@RequestParam(required=false)String baseUrl,@RequestParam(defaultValue="staging")String environment) {
        return ApiResponse.ok("Integration registered",service.register(code,name,baseUrl,environment));
    }
    @PostMapping("/{code}/check") public ApiResponse<?> check(@PathVariable String code) {
        return ApiResponse.ok("Connectivity check completed",service.check(code));
    }
    @PostMapping("/sync") public ApiResponse<?> sync(@Valid @RequestBody SyncRequest r) {
        return ApiResponse.ok("Bulk synchronization accepted",service.sync(r.integrationCode(),r.payload(),r.format()));
    }
    @PostMapping("/webhook") public ApiResponse<?> webhook(@Valid @RequestBody WebhookRequest r,@RequestHeader("X-Integration-Secret")String secret) {
        return ApiResponse.ok("Webhook accepted",service.webhook(r.integrationCode(),r.payload(),r.signature(),secret));
    }
}
