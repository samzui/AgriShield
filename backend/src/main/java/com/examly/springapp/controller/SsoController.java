package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.service.SsoService;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth/sso")
public class SsoController {
    private final SsoService service;
    public SsoController(SsoService service) {
        this.service=service;
    }@PostMapping("/exchange")public ApiResponse<?> exchange(@RequestBody String assertion) {
        return ApiResponse.ok("SSO exchange result",service.exchange(assertion));
    }
}
