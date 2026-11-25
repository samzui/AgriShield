package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.entity.User;
import com.examly.springapp.enums.Role;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/admin") @PreAuthorize("hasRole('ADMIN')") public class AdminController {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHistoryRepository passwordHistory;
    private final NotificationRepository notifications;
    private final NotificationPreferenceRepository notificationPreferences;
    private final OtpVerificationRepository otpVerifications;
    public AdminController(UserRepository u,RefreshTokenRepository refreshTokens,PasswordHistoryRepository passwordHistory,NotificationRepository notifications,NotificationPreferenceRepository notificationPreferences,OtpVerificationRepository otpVerifications) {
        users=u;
        this.refreshTokens=refreshTokens;
        this.passwordHistory=passwordHistory;
        this.notifications=notifications;
        this.notificationPreferences=notificationPreferences;
        this.otpVerifications=otpVerifications;
    }@GetMapping("/users")public ApiResponse<List<Map<String,Object>>> users() {
        var x=users.findAll().stream().sorted(java.util.Comparator.comparing(User::getCreatedAt).reversed()).map(u->{
            Map<String,Object> m=new LinkedHashMap<String,Object>();
            m.put("id",u.getId());
            m.put("userId",u.getUserId());
            m.put("name",u.getName());
            m.put("email",u.getEmail());
            m.put("phone",u.getPhoneNumber());
            m.put("role",u.getRole());
            m.put("active",u.isActive());
            m.put("verified",u.isEmailVerified());
            m.put("approved",u.isSupervisorApproved());
            m.put("createdAt",u.getCreatedAt());
            // An authority-role account is "pending" only while it has never been approved AND hasn't been
            // explicitly rejected (rejection deactivates the account, reusing the existing active flag).
            boolean requiresApproval=u.getRole()!=Role.FARMER&&u.getRole()!=Role.GUEST;
            m.put("pendingApproval",requiresApproval&&!u.isSupervisorApproved()&&u.isActive());
            m.put("rejected",requiresApproval&&!u.isSupervisorApproved()&&!u.isActive());
            return m;
        }).toList();
        return ApiResponse.ok("Users",x);
    }@PutMapping("/users/{id}/role")public ApiResponse<Void> role(@PathVariable Long id,@RequestParam Role role) {
        var u=users.findById(id).orElseThrow();
        u.setRole(role);
        users.save(u);
        return ApiResponse.ok("Role updated",null);
    }@PutMapping("/users/{id}/status")public ApiResponse<Void> status(@PathVariable Long id,@RequestParam boolean active) {
        var u=users.findById(id).orElseThrow();
        u.setActive(active);
        users.save(u);
        return ApiResponse.ok("User status updated",null);
    }@PutMapping("/users/{id}/approve")public ApiResponse<Void> approve(@PathVariable Long id) {
        var u=users.findById(id).orElseThrow();
        u.setSupervisorApproved(true);
        u.setActive(true);
        users.save(u);
        return ApiResponse.ok("User approved",null);
    }@PutMapping("/users/{id}/reject")public ApiResponse<Void> reject(@PathVariable Long id) {
        var u=users.findById(id).orElseThrow();
        u.setSupervisorApproved(false);
        u.setActive(false);
        users.save(u);
        return ApiResponse.ok("User rejected",null);
    }
    // Permanently removes a suspended account and its own auth-adjacent records (refresh tokens, password
    // history, notifications/preferences, pending OTPs for its email) so the same email can genuinely
    // register again from scratch and go through OTP verification as a brand-new account. Only a suspended
    // (inactive) account may be deleted — enforced here, not just hidden in the UI — and a user who still has
    // real business records (policies, surveys, claims) linked via a required foreign key is reported back
    // with a clear, honest error instead of a raw database failure.
    @DeleteMapping("/users/{id}")
    @Transactional
    public ApiResponse<Void> delete(@PathVariable Long id) {
        User u=users.findById(id).orElseThrow(()->new ResourceNotFoundException("User not found"));
        if(u.isActive())throw new IllegalArgumentException("Only a suspended account can be deleted. Suspend this account first.");
        refreshTokens.deleteAll(refreshTokens.findAll().stream().filter(t->t.getUser().getId().equals(id)).toList());
        passwordHistory.deleteAll(passwordHistory.findAll().stream().filter(p->p.getUser().getId().equals(id)).toList());
        notificationPreferences.deleteAll(notificationPreferences.findAll().stream().filter(p->p.getUser().getId().equals(id)).toList());
        notifications.deleteAll(notifications.findByUserIdOrderByCreatedAtDesc(id));
        otpVerifications.deleteAll(otpVerifications.findAll().stream().filter(o->o.getTarget().equalsIgnoreCase(u.getEmail())).toList());
        try {
            users.delete(u);
            users.flush();
        }catch(DataIntegrityViolationException e) {
            throw new IllegalArgumentException("This account has existing policies, surveys or claims linked to it and cannot be permanently deleted.");
        }
        return ApiResponse.ok("User deleted",null);
    }
}
