package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.entity.AuditLog;
import com.examly.springapp.entity.User;
import com.examly.springapp.repository.AuditLogRepository;
import com.examly.springapp.repository.UserRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;
@RestController @RequestMapping("/api/audit-logs") @PreAuthorize("hasRole('ADMIN')")
public class AuditController {
    private final AuditLogRepository repository;
    private final UserRepository users;
    public AuditController(AuditLogRepository repository,UserRepository users) {
        this.repository=repository;
        this.users=users;
    }
    @GetMapping public ApiResponse<?> search(@RequestParam(required=false)String action,@RequestParam(required=false)String entityType,@RequestParam(required=false)String entityId,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant from,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant to) {
        var logs=repository.findTop500ByOrderByTimestampDesc().stream().filter(x->action==null||x.getAction().equalsIgnoreCase(action)).filter(x->entityType==null||entityType.equalsIgnoreCase(x.getEntityType())).filter(x->entityId==null||entityId.equals(x.getEntityId())).filter(x->from==null||!x.getTimestamp().isBefore(from)).filter(x->to==null||!x.getTimestamp().isAfter(to)).toList();
        return ApiResponse.ok("Audit logs",withActors(logs));
    }
    @GetMapping("/user/{userId}") public ApiResponse<?> byUser(@PathVariable Long userId) {
        return ApiResponse.ok("Audit logs",withActors(repository.findByUserIdOrderByTimestampDesc(userId)));
    }
    @GetMapping("/entity/{entityType}/{entityId}") public ApiResponse<?> byEntity(@PathVariable String entityType,@PathVariable String entityId) {
        return ApiResponse.ok("Audit logs",withActors(repository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType,entityId)));
    }
    // Audit rows store only the actor's numeric user id; resolve it to a human-readable name + role so the
    // Audit Trail page doesn't have to show raw IDs. A missing/deleted user (or a system event) degrades
    // gracefully instead of failing the whole listing.
    private List<Map<String,Object>> withActors(List<AuditLog> logs) {
        Map<Long,User> byId=users.findAllById(logs.stream().map(AuditLog::getUserId).filter(Objects::nonNull).distinct().toList()).stream().collect(java.util.stream.Collectors.toMap(User::getId,u->u));
        return logs.stream().map(log-> {
            Map<String,Object> m=new LinkedHashMap<>();
            m.put("id",log.getId());
            m.put("timestamp",log.getTimestamp());
            m.put("userId",log.getUserId());
            User actor=log.getUserId()==null?null:byId.get(log.getUserId());
            m.put("actorName",actor!=null?actor.getName():"System");
            m.put("actorRole",actor!=null?actor.getRole().name():"SYSTEM");
            m.put("action",log.getAction());
            m.put("entityType",log.getEntityType());
            m.put("entityId",log.getEntityId());
            m.put("purpose",log.getPurpose());
            m.put("result",log.getResult());
            m.put("ipAddress",log.getIpAddress());
            return m;
        }).toList();
    }
}
