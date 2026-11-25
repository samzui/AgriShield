package com.examly.springapp.controller;
import com.examly.springapp.dto.request.ScheduleRequests.*;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.service.SchedulingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/schedules")
@PreAuthorize("hasAnyRole('INSURANCE_OFFICER','STATE_OFFICER','ADMIN')")
public class ScheduleController {
    private final SchedulingService service;
    public ScheduleController(SchedulingService service) {
        this.service=service;
    }
    @PostMapping public ApiResponse<?> create(@Valid @RequestBody CreateRequest r) {
        return ApiResponse.ok("Schedule created",service.create(r));
    }
    @GetMapping public ApiResponse<?> list() {
        return ApiResponse.ok("Schedules",service.list());
    }
    @GetMapping("/availability") public ApiResponse<?> availability(@RequestParam Long resourceUserId,@RequestParam java.time.Instant start,@RequestParam java.time.Instant end) {
        return ApiResponse.ok("Availability",service.available(resourceUserId,start,end,null));
    }
    @PutMapping("/{id}/reschedule") public ApiResponse<?> reschedule(@PathVariable Long id,@Valid @RequestBody RescheduleRequest r) {
        return ApiResponse.ok("Schedule rescheduled",service.reschedule(id,r));
    }
    @PutMapping("/{id}/cancel") public ApiResponse<?> cancel(@PathVariable Long id,@Valid @RequestBody CancelRequest r) {
        return ApiResponse.ok("Schedule cancelled",service.cancel(id,r.reason()));
    }
}
