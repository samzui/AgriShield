package com.examly.springapp.controller;
import com.examly.springapp.dto.request.AuthRequests.*;
import com.examly.springapp.dto.response.*;
import com.examly.springapp.service.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/auth") public class AuthController {
    private final AuthService s;
    // Read-only, unauthenticated (registration/OTP happen before login) so the OTP screen can tell the user
    // whether a real email was actually sent, instead of always pointing them at the server console.
    @Value("${app.mail.mode:console}") private String mailMode;
    public AuthController(AuthService s) {
        this.s=s;
    }@GetMapping("/mail-mode")public ApiResponse<Map<String,String>> mailMode() {
        return ApiResponse.ok("Mail delivery mode",Map.of("mode",mailMode));
    }@PostMapping("/register")public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest r) {
        return ResponseEntity.status(201).body(ApiResponse.ok("Registration successful! Please verify your email.",s.register(r)));
    }@PostMapping("/login")public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest r) {
        return ApiResponse.ok("Login successful",s.login(r));
    }@PostMapping("/resend-otp")
    public ApiResponse<Void> resend(@RequestParam String target,@RequestParam(defaultValue="EMAIL_VERIFICATION") String purpose) {
        s.resendOtp(target,purpose);
        return ApiResponse.ok("OTP sent", null);
    }
    @PostMapping("/verify-email")public ApiResponse<Void> verify(@Valid @RequestBody OtpRequest r) {
        s.verifyEmail(r);
        return ApiResponse.ok("Email verified",null);
    }@PostMapping("/request-password-reset")public ApiResponse<Void> requestReset(@RequestParam String email) {
        s.requestPasswordReset(email);
        return ApiResponse.ok("Password reset OTP sent",null);
    }@PostMapping("/reset-password")public ApiResponse<Void> reset(@Valid @RequestBody PasswordResetRequest r) {
        s.resetPassword(r);
        return ApiResponse.ok("Password reset successful",null);
    }@PostMapping("/logout")public ApiResponse<Void> logout(@RequestHeader(value="Authorization",required=false)String h) {
        if(h!=null&&h.startsWith("Bearer "))s.logout(h.substring(7));
        return ApiResponse.ok("Logged out",null);
    }@PostMapping("/refresh")public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest r) {
        return ApiResponse.ok("Refresh token rotated",s.refresh(r.refreshToken()));
    }
}
