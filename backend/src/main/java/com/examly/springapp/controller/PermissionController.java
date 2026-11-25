package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.entity.Permission;
import com.examly.springapp.entity.RolePermission;
import com.examly.springapp.enums.Role;
import com.examly.springapp.repository.PermissionRepository;
import com.examly.springapp.repository.RolePermissionRepository;
import com.examly.springapp.service.PermissionService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/permissions")
@PreAuthorize("hasRole('ADMIN')")
public class PermissionController {
    private final PermissionRepository permissions;
    private final RolePermissionRepository mappings;
    private final PermissionService service;
    public PermissionController(PermissionRepository permissions,RolePermissionRepository mappings,PermissionService service) {
        this.permissions=permissions;
        this.mappings=mappings;
        this.service=service;
    }
    @GetMapping public ApiResponse<?> all() {
        return ApiResponse.ok("Permissions",mappings.findAll());
    }
    @GetMapping("/role/{role}") public ApiResponse<?> byRole(@PathVariable Role role) {
        return ApiResponse.ok("Role permissions",mappings.findAll().stream().filter(x->x.getRole()==role).toList());
    }
    @PostMapping public ApiResponse<?> create(@RequestParam @NotBlank String code,@RequestParam String description) {
        return ApiResponse.ok("Permission created",service.ensure(code,description));
    }
    @PutMapping("/role/{role}/{code}") public ApiResponse<?> set(@PathVariable Role role,@PathVariable String code,@RequestParam boolean enabled) {
        service.set(role,code,enabled);
        return ApiResponse.ok("Permission updated",null);
    }
}
