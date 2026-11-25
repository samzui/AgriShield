package com.examly.springapp.controller;

import com.examly.springapp.dto.request.LossRequests.CreateRequest;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.entity.LossNotification;
import com.examly.springapp.entity.User;
import com.examly.springapp.enums.Role;
import com.examly.springapp.repository.LossNotificationRepository;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.service.DocumentService;
import com.examly.springapp.service.LossNotificationService;
import com.examly.springapp.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loss-notifications")
public class LossNotificationController {
    private final LossNotificationService service;
    private final LossNotificationRepository repo;
    private final UserRepository users;
    private final DocumentService docService;

    public LossNotificationController(LossNotificationService service, LossNotificationRepository repo, UserRepository users, DocumentService docService) {
        this.service = service;
        this.repo = repo;
        this.users = users;
        this.docService = docService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FARMER','BANK_OFFICER','ADMIN')")
    public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody CreateRequest r) {
        return ResponseEntity.status(201).body(ApiResponse.ok("Loss notification submitted", view(service.create(r))));
    }

    @PostMapping(value = "/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('FARMER','BANK_OFFICER','ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<?>> uploadPhoto(@PathVariable Long id, @RequestPart("file") MultipartFile file) throws Exception {
        LossNotification loss = service.get(id);
        var doc = docService.upload("LOSS_NOTIFICATION", id, "EVIDENCE", "NORMAL", null, null, file);
        String url = "/api/documents/" + doc.getId() + "/content";
        loss.getPhotoUrls().add(url);
        repo.save(loss);
        return ResponseEntity.status(201).body(ApiResponse.ok("Photo uploaded", url));
    }

    @GetMapping
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> list() {
        User user = users.findByUserId(SecurityUtil.username()).orElseThrow();
        return ApiResponse.ok("Loss notifications", service.listViewsFor(user));
    }

    @GetMapping("/{id}")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> get(@PathVariable Long id) {
        return ApiResponse.ok("Loss notification", viewDetail(service.get(id)));
    }

    @GetMapping("/policy/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> policy(@PathVariable Long id) {
        return ApiResponse.ok("Loss notifications", repo.findAll().stream().filter(loss -> loss.getPolicy().getId().equals(id)).map(this::view).toList());
    }

    private Map<String, Object> view(LossNotification loss) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", loss.getId());
        result.put("lossType", loss.getLossType());
        result.put("affectedAreaHa", loss.getAffectedAreaHa());
        result.put("lossPercentageEstimate", loss.getLossPercentageEstimate());
        result.put("notificationDate", loss.getNotificationDate());
        result.put("geoLat", loss.getGeoLat());
        result.put("geoLng", loss.getGeoLng());
        result.put("status", loss.getStatus());
        if (loss.getPolicy() != null) {
            Map<String, Object> policy = new LinkedHashMap<>();
            policy.put("id", loss.getPolicy().getId());
            policy.put("policyNumber", loss.getPolicy().getPolicyNumber());
            policy.put("cropName", loss.getPolicy().getCropName());
            result.put("policy", policy);
            result.put("policyId", loss.getPolicy().getId());
        }
        return result;
    }

    // Detail view — includes photoUrls; materialized into a plain ArrayList while the
    // Hibernate session is still open, since Jackson serializes the response after the
    // @Transactional method (and its session) has already returned/closed.
    private Map<String, Object> viewDetail(LossNotification loss) {
        Map<String, Object> result = new LinkedHashMap<>(view(loss));
        result.put("photoUrls", new ArrayList<>(loss.getPhotoUrls()));
        return result;
    }
}
