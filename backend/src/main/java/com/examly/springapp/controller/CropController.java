package com.examly.springapp.controller;
import com.examly.springapp.dto.request.CropRequests.CropRequest;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.enums.Season;
import com.examly.springapp.service.CropService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/crops")
public class CropController {
    private final CropService service;
    public CropController(CropService service) {
        this.service=service;
    }
    @PostMapping @PreAuthorize("hasAnyRole('INSURANCE_OFFICER','ADMIN')") public ApiResponse<?> create(@Valid @RequestBody CropRequest r) {
        return ApiResponse.ok("Crop created",service.create(r));
    }
    @GetMapping @PreAuthorize("isAuthenticated()") public ApiResponse<?> list(@RequestParam(required=false)Season season) {
        return ApiResponse.ok("Crops",service.list(season));
    }
    @GetMapping("/{id}") @PreAuthorize("isAuthenticated()") public ApiResponse<?> get(@PathVariable Long id) {
        return ApiResponse.ok("Crop",service.get(id));
    }
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('INSURANCE_OFFICER','ADMIN')") public ApiResponse<?> update(@PathVariable Long id,@Valid @RequestBody CropRequest r) {
        return ApiResponse.ok("Crop updated",service.update(id,r));
    }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Crop deactivated",null);
    }
}
