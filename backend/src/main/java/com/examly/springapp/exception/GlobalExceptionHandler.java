package com.examly.springapp.exception;
import com.examly.springapp.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestControllerAdvice public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ApiResponse<Map<String,String>>> validation(MethodArgumentNotValidException e) {
        Map<String,String> m=new LinkedHashMap<>();
        e.getBindingResult().getAllErrors().forEach(x->m.put(x instanceof FieldError f?f.getField():x.getObjectName(),x.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ApiResponse.ok("Validation failed",m));
    }
    @ExceptionHandler(InvalidNameException.class) ResponseEntity<ApiResponse<Void>> badName(InvalidNameException e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
    }
    @ExceptionHandler(InvalidPhoneException.class) ResponseEntity<ApiResponse<Void>> badPhone(InvalidPhoneException e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
    }
    @ExceptionHandler(DuplicateClaimException.class) ResponseEntity<ApiResponse<Void>> duplicate(DuplicateClaimException e) {
        return ResponseEntity.status(409).body(ApiResponse.fail(e.getMessage()));
    }
    @ExceptionHandler(UnauthorisedAccessException.class) ResponseEntity<ApiResponse<Void>> forbidden(UnauthorisedAccessException e) {
        return ResponseEntity.status(403).body(ApiResponse.fail(e.getMessage()));
    }
    // Spring Security 6's method-security denial (@PreAuthorize failing) throws this — without a handler it
    // was falling through to the generic 500 handler below instead of a proper 403.
    @ExceptionHandler(AuthorizationDeniedException.class) ResponseEntity<ApiResponse<Void>> accessDenied(AuthorizationDeniedException e) {
        return ResponseEntity.status(403).body(ApiResponse.fail("You do not have permission to perform this action"));
    }
    @ExceptionHandler(ResourceNotFoundException.class) ResponseEntity<ApiResponse<Void>> notFound(ResourceNotFoundException e) {
        return ResponseEntity.status(404).body(ApiResponse.fail(e.getMessage()));
    }
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<ApiResponse<Void>> illegal(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
    }
    @ExceptionHandler(Exception.class) ResponseEntity<ApiResponse<Void>> generic(Exception e,HttpServletRequest r) {
        log.error("Unhandled exception on {} {}", r.getMethod(), r.getRequestURI(), e);
        return ResponseEntity.status(500).body(ApiResponse.fail("An unexpected error occurred"));
    }
}
