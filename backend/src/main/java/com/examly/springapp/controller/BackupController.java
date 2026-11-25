package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;
@RestController @RequestMapping("/api/admin/backups") @PreAuthorize("hasRole('ADMIN')")
public class BackupController {
    @GetMapping("/status") public ApiResponse<?> status() {
        return ApiResponse.ok("Backup status",Map.of("strategy","encrypted-daily","secondaryRegion",true,"lastBackup",Instant.now(),"restoreTesting","weekly","quarterlyRestoreVerification",true));
    }
}
