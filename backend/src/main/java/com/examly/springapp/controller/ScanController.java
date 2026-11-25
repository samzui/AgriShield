package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.entity.ScanRecord;
import com.examly.springapp.repository.ScanRecordRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
@RestController @RequestMapping("/api/scanners") @PreAuthorize("isAuthenticated()")
public class ScanController {
    private final ScanRecordRepository repo;
    public ScanController(ScanRecordRepository repo) {
        this.repo=repo;
    }
    @PostMapping("/ingest") public ApiResponse<?> ingest(@RequestParam String scanType,@RequestParam String payloadReference) {
        ScanRecord r=new ScanRecord();
        r.setScanType(scanType);
        r.setPayloadReference(payloadReference);
        r.setStatus("ACCEPTED");
        r.setResult("Scanner payload registered for verification");
        r.setScannedAt(Instant.now());
        return ApiResponse.ok("Scan accepted",repo.save(r));
    }
}
