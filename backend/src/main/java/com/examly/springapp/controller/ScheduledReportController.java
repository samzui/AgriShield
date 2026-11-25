package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.entity.ScheduledReport;
import com.examly.springapp.repository.ScheduledReportRepository;
import com.examly.springapp.service.AuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/reports/scheduled") @PreAuthorize("hasAnyRole('INSURANCE_OFFICER','STATE_OFFICER','ADMIN')")
public class ScheduledReportController {
    private final ScheduledReportRepository repo;
    private final AuditService audit;
    public ScheduledReportController(ScheduledReportRepository repo,AuditService audit) {
        this.repo=repo;
        this.audit=audit;
    }
    @GetMapping public ApiResponse<?> list() {
        return ApiResponse.ok("Scheduled reports",repo.findAll());
    }
    @PostMapping public ApiResponse<?> create(@RequestBody ScheduledReport r) {
        r.setId(null);
        var saved=repo.save(r);
        audit.log("CREATE","SCHEDULED_REPORT",String.valueOf(saved.getId()),"SUCCESS",null,saved.getName(),"scheduled reporting");
        return ApiResponse.ok("Scheduled report created",saved);
    }
    @PutMapping("/{id}") public ApiResponse<?> update(@PathVariable Long id,@RequestBody ScheduledReport input) {
        var r=repo.findById(id).orElseThrow(()->new IllegalArgumentException("Scheduled report not found"));
        r.setName(input.getName());
        r.setReportType(input.getReportType());
        r.setFormat(input.getFormat());
        r.setRecipientList(input.getRecipientList());
        r.setCronExpression(input.getCronExpression());
        r.setActive(input.isActive());
        return ApiResponse.ok("Scheduled report updated",repo.save(r));
    }
}
