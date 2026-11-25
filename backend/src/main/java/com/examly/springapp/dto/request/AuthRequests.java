package com.examly.springapp.dto.request;
import com.examly.springapp.enums.Role;
import jakarta.validation.constraints.*;
public final class AuthRequests {
    private AuthRequests() {
    }
    public record RegisterRequest(
    @NotBlank @Size(min=2,max=100) String name,
    @NotBlank @Size(max=50) String userId,
    @NotBlank @Email String email,
    @NotBlank @Pattern(regexp="\\d{10}",message="Phone Number must be exactly 10 digits long") String phone,
    @NotBlank @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",message="Password must meet security requirements") String password,
    @NotNull Role role
    ) {
    }
    public record LoginRequest(@NotBlank String identifier,@NotBlank String password) {
    }
    public record RefreshRequest(@NotBlank String refreshToken) {
    }
    public record OtpRequest(@NotBlank String target,@NotBlank String otp,String purpose) {
    }
    public record PasswordResetRequest(
    @NotBlank @Email String email,
    @NotBlank String otp,
    @NotBlank @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",message="Password must meet security requirements") String newPassword
    ) {
    }
}
