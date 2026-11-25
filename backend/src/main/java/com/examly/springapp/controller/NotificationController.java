package com.examly.springapp.controller;
import com.examly.springapp.dto.request.NotificationRequests.*;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.entity.User;
import com.examly.springapp.enums.Role;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.service.NotificationService;
import com.examly.springapp.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    private final UserRepository users;
    public NotificationController(NotificationService service,UserRepository users) {
        this.service=service;
        this.users=users;
    }
    @GetMapping("/user/{id}") @PreAuthorize("isAuthenticated()") public ApiResponse<?> user(@PathVariable Long id) {
        User current=users.findByUserId(SecurityUtil.username()).orElseThrow();
        if(!current.getId().equals(id) && current.getRole()!=Role.ADMIN) throw new com.examly.springapp.exception.UnauthorisedAccessException("You can access only your own notifications");
        return ApiResponse.ok("Notifications",service.userNotifications(id));
    }
    @PostMapping("/templates") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<?> template(@Valid @RequestBody TemplateRequest r) {
        return ApiResponse.ok("Template saved",service.saveTemplate(r.code(),r.subject(),r.body(),r.active()));
    }
    @PutMapping("/preferences/{userId}") @PreAuthorize("isAuthenticated()") public ApiResponse<?> preference(@PathVariable Long userId,@Valid @RequestBody PreferenceRequest r) {
        User current=users.findByUserId(SecurityUtil.username()).orElseThrow();
        if(!current.getId().equals(userId) && current.getRole()!=Role.ADMIN) throw new com.examly.springapp.exception.UnauthorisedAccessException("You can update only your own notification preferences");
        User u=users.findById(userId).orElseThrow(()->new IllegalArgumentException("User not found"));
        service.setPreference(u,r.channel(),r.enabled());
        return ApiResponse.ok("Preference updated",null);
    }
    @PostMapping("/broadcast") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<?> broadcast(@Valid @RequestBody BroadcastRequest r) {
        Role role=r.role()==null?null:Role.valueOf(r.role());
        return ApiResponse.ok("Broadcast queued",service.broadcast(r.eventType(),r.message(),r.channel(),role));
    }
}
