package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.service.IdentityVerificationService;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/identity") @PreAuthorize("isAuthenticated()")
public class IdentityController {
    private final IdentityVerificationService service;
    public IdentityController(IdentityVerificationService service) {
        this.service=service;
    }
    @PostMapping("/verify") public ApiResponse<?> verify(@RequestParam Long userId,@RequestParam @NotBlank String documentType,@RequestParam @Pattern(regexp="\\d{12}")String documentNumber) {
        return ApiResponse.ok("Identity verification completed",service.verify(userId,documentType,documentNumber));
    }
}
