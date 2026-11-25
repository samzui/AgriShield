package com.examly.springapp.controller;

import com.examly.springapp.dto.request.ClaimRequests.*;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.entity.Claim;
import com.examly.springapp.entity.LossNotification;
import com.examly.springapp.entity.Policy;
import com.examly.springapp.entity.SurveyAssignment;
import com.examly.springapp.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {
    private final ClaimService service;

    public ClaimController(ClaimService service) {
        this.service = service;
    }

    @PostMapping("/initiate")
    @Transactional
    @PreAuthorize("hasAnyRole('FARMER','INSURANCE_OFFICER','ADMIN')")
    public ResponseEntity<ApiResponse<?>> initiate(@Valid @RequestBody InitiateRequest r) {
        return ResponseEntity.status(201).body(ApiResponse.ok("Claim initiated", view(service.initiate(r))));
    }

    @PutMapping("/{id}/review")
    @Transactional
    @PreAuthorize("hasAnyRole('INSURANCE_OFFICER','ADMIN')")
    public ApiResponse<?> review(@PathVariable Long id) {
        return ApiResponse.ok("Claim under review", view(service.review(id)));
    }

    @PutMapping("/{id}/approve")
    @Transactional
    // Admin is monitoring-only for claims; approval/rejection stays with the Insurance Officer.
    @PreAuthorize("hasRole('INSURANCE_OFFICER')")
    public ApiResponse<?> approve(@PathVariable Long id, @Valid @RequestBody ApproveRequest r) {
        return ApiResponse.ok("Claim approved", view(service.approve(id, r)));
    }

    @PutMapping("/{id}/reject")
    @Transactional
    @PreAuthorize("hasRole('INSURANCE_OFFICER')")
    public ApiResponse<?> reject(@PathVariable Long id, @RequestParam(defaultValue = "Claim rejected") String reason) {
        return ApiResponse.ok("Claim rejected", view(service.reject(id, reason)));
    }

    @PutMapping("/{id}/disburse")
    @Transactional
    @PreAuthorize("hasAnyRole('BANK_OFFICER','ADMIN')")
    public ApiResponse<?> disburse(@PathVariable Long id, @Valid @RequestBody DisburseRequest r) {
        return ApiResponse.ok("Claim disbursed", view(service.disburse(id, r)));
    }

    @GetMapping("/{id}")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> get(@PathVariable Long id) {
        return ApiResponse.ok("Claim", view(service.get(id)));
    }

    @GetMapping("/{id}/history")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> history(@PathVariable Long id) {
        return ApiResponse.ok("Claim history", service.history(id));
    }

    @GetMapping
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = service.list(page, size);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", result.getContent().stream().map(this::view).toList());
        response.put("page", result.getNumber());
        response.put("size", result.getSize());
        response.put("totalElements", result.getTotalElements());
        response.put("totalPages", result.getTotalPages());
        response.put("last", result.isLast());
        return ApiResponse.ok("Claims", response);
    }

    // Builds the full response the frontend renders (claim + farmer + policy + survey assessment + original loss
    // evidence). Every caller of this method must run inside an active transaction (@Transactional on the calling
    // controller method) since it walks several lazy relations; element collections are copied into plain
    // ArrayLists so the result stays serializable once that transaction has closed.
    private Map<String, Object> view(Claim claim) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", claim.getId());
        result.put("createdAt", claim.getCreatedAt());
        result.put("claimedAmount", claim.getClaimedAmount());
        result.put("approvedAmount", claim.getApprovedAmount());
        result.put("dbtUtr", claim.getDbtUtr());
        result.put("disbursedDate", claim.getDisbursedDate());
        result.put("status", claim.getStatus());
        if (claim.getPolicy() != null) {
            Policy policy = claim.getPolicy();
            Map<String, Object> policyView = new LinkedHashMap<>();
            policyView.put("id", policy.getId());
            policyView.put("policyNumber", policy.getPolicyNumber());
            policyView.put("cropName", policy.getCropName());
            policyView.put("season", policy.getSeason());
            policyView.put("sownAreaHa", policy.getSownAreaHa());
            policyView.put("sumInsured", policy.getSumInsured());
            String acct = policy.getBankAccountNo();
            policyView.put("bankAccountNo", (acct != null && acct.length() > 4) ? "****" + acct.substring(acct.length() - 4) : "****");
            policyView.put("bankHolderName", policy.getBankHolderName());
            policyView.put("bankName", policy.getBankName());
            policyView.put("bankIfsc", policy.getBankIfsc());
            policyView.put("bankBranch", policy.getBankBranch());
            if (policy.getFarmer() != null) {
                policyView.put("farmerId", policy.getFarmer().getId());
                policyView.put("farmerName", policy.getFarmer().getName());
                policyView.put("farmerPhone", policy.getFarmer().getPhoneNumber());
                policyView.put("farmerEmail", policy.getFarmer().getEmail());
            }
            result.put("policy", policyView);
            result.put("policyId", policy.getId());
        }
        if (claim.getSurvey() != null) {
            SurveyAssignment survey = claim.getSurvey();
            result.put("surveyId", survey.getId());
            Map<String, Object> surveyView = new LinkedHashMap<>();
            surveyView.put("id", survey.getId());
            surveyView.put("status", survey.getStatus());
            surveyView.put("surveyDate", survey.getSurveyDate());
            surveyView.put("yieldAssessedKgHa", survey.getYieldAssessedKgHa());
            surveyView.put("lossAssessedPercentage", survey.getLossAssessedPercentage());
            surveyView.put("surveyPhotos", new ArrayList<>(survey.getSurveyPhotos()));
            if (survey.getSurveyor() != null) {
                surveyView.put("surveyorId", survey.getSurveyor().getId());
                surveyView.put("surveyorName", survey.getSurveyor().getName());
            }
            if (survey.getNotification() != null) {
                LossNotification n = survey.getNotification();
                Map<String, Object> lossView = new LinkedHashMap<>();
                lossView.put("id", n.getId());
                lossView.put("lossType", n.getLossType());
                lossView.put("affectedAreaHa", n.getAffectedAreaHa());
                lossView.put("lossPercentageEstimate", n.getLossPercentageEstimate());
                lossView.put("notificationDate", n.getNotificationDate());
                lossView.put("geoLat", n.getGeoLat());
                lossView.put("geoLng", n.getGeoLng());
                lossView.put("photoUrls", new ArrayList<>(n.getPhotoUrls()));
                surveyView.put("loss", lossView);
            }
            result.put("survey", surveyView);
        }
        return result;
    }
}
