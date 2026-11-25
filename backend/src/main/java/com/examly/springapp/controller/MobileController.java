package com.examly.springapp.controller;
import com.examly.springapp.dto.request.MobileRequests.SyncRequest;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.service.MobileSyncService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/mobile") @PreAuthorize("isAuthenticated()")
public class MobileController {
    private final MobileSyncService service;
    public MobileController(MobileSyncService service) {
        this.service=service;
    }@PostMapping("/sync")public ApiResponse<?> sync(@Valid @RequestBody SyncRequest r) {
        return ApiResponse.ok("Mobile synchronization completed",service.sync(r));
    }
}
