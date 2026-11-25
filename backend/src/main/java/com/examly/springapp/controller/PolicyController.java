package com.examly.springapp.controller;
import com.examly.springapp.dto.request.PolicyRequests.EnrollRequest;
import com.examly.springapp.dto.response.*;
import com.examly.springapp.entity.Policy;
import com.examly.springapp.enums.PolicyStatus;
import com.examly.springapp.service.PolicyService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/policies")
public class PolicyController {
    private final PolicyService service;
    public PolicyController(PolicyService service) {
        this.service=service;
    }
    @PostMapping("/enroll") @PreAuthorize("hasAnyRole('FARMER','BANK_OFFICER','ADMIN')") public ResponseEntity<ApiResponse<?>> enroll(@Valid @RequestBody EnrollRequest r) {
        return ResponseEntity.status(201).body(ApiResponse.ok("Policy enrolled",view(service.enroll(r))));
    }
    @GetMapping("/{id}") @PreAuthorize("isAuthenticated()") @Transactional(readOnly=true) public ApiResponse<?> get(@PathVariable Long id) {
        return ApiResponse.ok("Policy",view(service.get(id)));
    }
    @GetMapping("/farmer/{id}") @PreAuthorize("isAuthenticated()") @Transactional(readOnly=true) public ApiResponse<?> farmer(@PathVariable Long id) {
        return ApiResponse.ok("Policies",service.farmer(id).stream().map(this::view).toList());
    }
    @GetMapping @PreAuthorize("isAuthenticated()") @Transactional(readOnly=true) public ApiResponse<?> list(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size) {
        var pg=service.list(page,size);
        Map<String,Object> resp=new LinkedHashMap<>();
        resp.put("content",pg.content().stream().map(this::view).toList());
        resp.put("page",pg.page());resp.put("size",pg.size());resp.put("totalElements",pg.totalElements());resp.put("totalPages",pg.totalPages());resp.put("last",pg.last());
        return ApiResponse.ok("Policies",resp);
    }
    @PutMapping("/{id}/status") @PreAuthorize("hasAnyRole('INSURANCE_OFFICER','STATE_OFFICER','ADMIN')") public ApiResponse<?> status(@PathVariable Long id,@RequestParam PolicyStatus status) {
        return ApiResponse.ok("Policy status updated",view(service.updateStatus(id,status)));
    }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Policy lapsed",null);
    }
    private Map<String,Object> view(Policy p) {
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("id",p.getId());
        m.put("policyNumber",p.getPolicyNumber());
        m.put("cropName",p.getCropName());
        m.put("season",p.getSeason());
        m.put("sownAreaHa",p.getSownAreaHa());
        m.put("sumInsured",p.getSumInsured());
        m.put("premiumFarmer",p.getPremiumFarmer());
        m.put("premiumState",p.getPremiumState());
        m.put("premiumCentre",p.getPremiumCentre());
        m.put("status",p.getStatus());
        m.put("enrollmentDate",p.getEnrollmentDate());
        // Mask bank account — show only last 4 digits
        String acct=p.getBankAccountNo();
        m.put("bankAccountNo",(acct!=null&&acct.length()>4)?"****"+acct.substring(acct.length()-4):"****");
        m.put("bankHolderName",p.getBankHolderName());
        m.put("bankName",p.getBankName());
        m.put("bankIfsc",p.getBankIfsc());
        m.put("bankBranch",p.getBankBranch());
        if(p.getFarmer()!=null){m.put("farmerId",p.getFarmer().getId());m.put("farmerName",p.getFarmer().getName());}
        return m;
    }
}
