package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.service.WorkflowService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/workflows")
@PreAuthorize("hasAnyRole('INSURANCE_OFFICER','STATE_OFFICER','ADMIN')")
public class WorkflowController {
    private final WorkflowService service;
    public WorkflowController(WorkflowService service) {
        this.service=service;
    }
    @PostMapping
    public ApiResponse<?> start(@RequestParam String type,@RequestParam String entityType,@RequestParam Long entityId,@RequestBody(required=false)List<WorkflowService.StageSpec> stages) {
        // Admin is monitoring-only across approval workflows (matching the same rule enforced on claims), so
        // every stage of the default template - including Completion - belongs to the Insurance Officer.
        List<WorkflowService.StageSpec> actual=(stages==null||stages.isEmpty())?List.of(new WorkflowService.StageSpec("Review","INSURANCE_OFFICER",24),new WorkflowService.StageSpec("Approval","INSURANCE_OFFICER",24),new WorkflowService.StageSpec("Completion","INSURANCE_OFFICER",24)):stages;
        return ApiResponse.ok("Workflow started",service.start(type,entityType,entityId,actual));
    }
    @GetMapping("/{id}/stages") public ApiResponse<?> stages(@PathVariable Long id) {
        return ApiResponse.ok("Workflow stages",service.stages(id));
    }
    @GetMapping public ApiResponse<?> list() {
        return ApiResponse.ok("Workflows",service.list());
    }
    @GetMapping("/{id}") public ApiResponse<?> get(@PathVariable Long id) {
        return ApiResponse.ok("Workflow",service.get(id));
    }
    @PutMapping("/{id}/advance") public ApiResponse<?> advance(@PathVariable Long id,@RequestParam(required=false)String note) {
        service.advance(id,note);
        return ApiResponse.ok("Workflow advanced",service.get(id));
    }
}
