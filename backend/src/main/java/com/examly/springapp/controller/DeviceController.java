package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.service.DeviceRecognitionService;
import com.examly.springapp.util.SecurityUtil;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/devices") @PreAuthorize("isAuthenticated()")
public class DeviceController {
    private final DeviceRecognitionService service;
    private final UserRepository users;
    public DeviceController(DeviceRecognitionService service,UserRepository users) {
        this.service=service;
        this.users=users;
    }
    @PostMapping("/trust") public ApiResponse<?> trust(@RequestParam String fingerprint,@RequestParam(defaultValue="Browser")String deviceName) {
        var u=users.findByUserId(SecurityUtil.username()).orElseThrow();
        return ApiResponse.ok("Device trusted",service.register(u,fingerprint,deviceName,"",true));
    }
}
