package com.examly.springapp.controller;

import com.examly.springapp.dto.request.SurveyRequests.*;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.entity.SurveyAssignment;
import com.examly.springapp.enums.Role;
import com.examly.springapp.repository.SurveyAssignmentRepository;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.service.SurveyService;
import com.examly.springapp.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/survey-assignments")
public class SurveyController {

    private final SurveyService service;
    private final SurveyAssignmentRepository repo;
    private final UserRepository users;

    public SurveyController(
            SurveyService service,
            SurveyAssignmentRepository repo,
            UserRepository users) {
        this.service = service;
        this.repo = repo;
        this.users = users;
    }

    // Only the Insurance Officer schedules/assigns surveys — Admin monitors, State Officer and other
    // roles do not own this action.
    @PostMapping
    @PreAuthorize("hasRole('INSURANCE_OFFICER')")
    public ResponseEntity<ApiResponse<?>> assign(@Valid @RequestBody AssignRequest request) {
        SurveyAssignment created = service.assign(request);
        SurveyAssignment loaded = repo.findByIdWithDetails(created.getId())
                .orElseThrow(() -> new IllegalArgumentException("Survey assignment not found"));
        return ResponseEntity.status(201).body(ApiResponse.ok("Survey assigned", toView(loaded)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> list() {
        var currentUser = users.findByUserId(SecurityUtil.username()).orElse(null);
        List<SurveyAssignment> assignments;

        if (currentUser == null
                || currentUser.getRole() == Role.ADMIN
                || currentUser.getRole() == Role.INSURANCE_OFFICER
                || currentUser.getRole() == Role.STATE_OFFICER) {
            assignments = repo.findAllWithDetails();
        } else if (currentUser.getRole() == Role.SURVEYOR) {
            assignments = repo.findAssignmentsForSurveyor(currentUser.getId());
        } else {
            assignments = repo.findAssignmentsForFarmer(currentUser.getId());
        }

        // Do not return raw JPA entities here. Their lazy relationships can cause
        // serialization failures and circular object traversal, resulting in HTTP 500.
        return ApiResponse.ok(
                "Survey assignments",
                assignments.stream().map(this::toView).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> get(@PathVariable Long id) {
        SurveyAssignment assignment = repo.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Survey assignment not found"));

        var currentUser = users.findByUserId(SecurityUtil.username()).orElseThrow();
        boolean privileged = currentUser.getRole() == Role.ADMIN
                || currentUser.getRole() == Role.INSURANCE_OFFICER
                || currentUser.getRole() == Role.STATE_OFFICER;
        boolean isSurveyor = assignment.getSurveyor().getId().equals(currentUser.getId());
        boolean isFarmer = assignment.getNotification().getPolicy().getFarmer().getId()
                .equals(currentUser.getId());

        if (!privileged && !isSurveyor && !isFarmer) {
            throw new com.examly.springapp.exception.UnauthorisedAccessException(
                    "You can access only your assigned or own surveys");
        }

        return ApiResponse.ok("Survey assignment", toView(assignment));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('SURVEYOR','ADMIN')")
    public ApiResponse<?> start(@PathVariable Long id) {
        service.start(id);
        SurveyAssignment updated = repo.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Survey assignment not found"));
        return ApiResponse.ok("Survey started", toView(updated));
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('SURVEYOR','ADMIN')")
    public ApiResponse<?> submit(
            @PathVariable Long id,
            @Valid @RequestBody SubmitRequest request) {
        service.submit(id, request);
        SurveyAssignment updated = repo.findByIdWithDetails(id)
            .orElseThrow(() -> new IllegalArgumentException("Survey assignment not found"));
        return ApiResponse.ok("Survey submitted", toView(updated));
    }

    private Map<String, Object> toView(SurveyAssignment assignment) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", assignment.getId());
        view.put("notificationId", assignment.getNotification().getId());
        view.put("surveyorId", assignment.getSurveyor().getId());
        view.put("surveyorName", assignment.getSurveyor().getName());
        view.put("assignedDate", assignment.getAssignedDate());
        view.put("surveyDate", assignment.getSurveyDate());
        view.put("status", assignment.getStatus());
        view.put("yieldAssessedKgHa", assignment.getYieldAssessedKgHa());
        view.put("lossAssessedPercentage", assignment.getLossAssessedPercentage());
        view.put("surveyPhotos", assignment.getSurveyPhotos());
        view.put("surveyGeoLat", assignment.getSurveyGeoLat());
        view.put("surveyGeoLng", assignment.getSurveyGeoLng());
        view.put("ccePlotId", assignment.getCcePlotId());
        view.put("ccePlotAreaHa", assignment.getCcePlotAreaHa());
        view.put("assessmentSource", assignment.getAssessmentSource());
        view.put("notificationGeoLat", assignment.getNotification().getGeoLat());
        view.put("notificationGeoLng", assignment.getNotification().getGeoLng());
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("id", assignment.getNotification().getPolicy().getId());
        policy.put("policyNumber", assignment.getNotification().getPolicy().getPolicyNumber());
        policy.put("cropName", assignment.getNotification().getPolicy().getCropName());
        view.put("policy", policy);
        view.put("policyNumber", assignment.getNotification().getPolicy().getPolicyNumber());
        view.put("cropName", assignment.getNotification().getPolicy().getCropName());
        view.put("farmerName", assignment.getNotification().getPolicy().getFarmer().getName());
        view.put("farmer", Map.of(
            "id", assignment.getNotification().getPolicy().getFarmer().getId(),
            "name", assignment.getNotification().getPolicy().getFarmer().getName()));
        return view;
    }
}
