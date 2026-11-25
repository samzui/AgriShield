package com.examly.springapp.controller;
import com.examly.springapp.dto.request.DocumentRequests.*;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.entity.Document;
import com.examly.springapp.integration.DocumentStorageService;
import com.examly.springapp.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
@RestController
@RequestMapping("/api/documents")
@PreAuthorize("isAuthenticated()")
public class DocumentController {
    private final DocumentService service;
    private final DocumentStorageService storage;
    public DocumentController(DocumentService service,DocumentStorageService storage) {
        this.service=service;
        this.storage=storage;
    }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ResponseEntity<ApiResponse<Document>> upload(@RequestParam String entityType,@RequestParam Long entityId,@RequestParam String category,@RequestParam(defaultValue="NORMAL")String sensitivity,@RequestParam(required=false)LocalDate expiryDate,@RequestParam(required=false)LocalDate retentionDate,@RequestPart MultipartFile file)throws Exception {
        return ResponseEntity.status(201).body(ApiResponse.ok("Document uploaded",service.upload(entityType,entityId,category,sensitivity,expiryDate,retentionDate,file)));
    }
    @PostMapping(value="/{id}/versions",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ApiResponse<?> version(@PathVariable Long id,@RequestPart MultipartFile file,@RequestParam(defaultValue="New document version")String comment)throws Exception {
        return ApiResponse.ok("Document version created",service.addVersion(id,file,comment));
    }
    @GetMapping("/{id}") public ApiResponse<?> get(@PathVariable Long id) {
        return ApiResponse.ok("Document",service.get(id));
    }
    @GetMapping("/{id}/versions") public ApiResponse<?> versions(@PathVariable Long id) {
        return ApiResponse.ok("Document versions",service.versions(id));
    }
    @PostMapping("/{id}/rollback") public ApiResponse<?> rollback(@PathVariable Long id,@Valid @RequestBody RollbackRequest r) {
        return ApiResponse.ok("Document rolled back",service.rollback(id,r));
    }
    // Admin can view/manage documents but must not sign them; signing stays with operational officers.
    @PostMapping("/{id}/sign") @PreAuthorize("hasAnyRole('INSURANCE_OFFICER','STATE_OFFICER')") public ApiResponse<?> sign(@PathVariable Long id,@Valid @RequestBody SignatureRequest r) {
        return ApiResponse.ok("Document signed",service.sign(id,r.signatureReference(),r.algorithm()));
    }
    @GetMapping("/{id}/signature") public ApiResponse<?> signature(@PathVariable Long id) {
        return ApiResponse.ok("Document signature",service.latestSignatureView(id));
    }
    @GetMapping("/{id}/content") public ResponseEntity<byte[]> content(@PathVariable Long id)throws Exception {
        var d=service.get(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.getMimeType()==null?"application/octet-stream":d.getMimeType())).body(storage.read(d.getStorageKey()));
    }
    @GetMapping("/search") public ApiResponse<?> search(@RequestParam String q) {
        return ApiResponse.ok("Document search",service.search(q));
    }
}
