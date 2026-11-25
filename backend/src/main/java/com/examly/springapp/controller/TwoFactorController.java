package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.service.TwoFactorService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth/2fa") @PreAuthorize("isAuthenticated()")
public class TwoFactorController {
    private final TwoFactorService service;
    public TwoFactorController(TwoFactorService service) {
        this.service=service;
    }
    @PostMapping("/challenge") public ApiResponse<?> challenge(@RequestParam Long userId,@RequestParam String operation) {
        return ApiResponse.ok("2FA challenge created",service.createChallenge(userId,operation));
    }
    @PostMapping("/verify") public ApiResponse<?> verify(@RequestParam String challengeId,@RequestParam String otp) {
        return ApiResponse.ok("2FA verified",service.verify(challengeId,otp));
    }
}
