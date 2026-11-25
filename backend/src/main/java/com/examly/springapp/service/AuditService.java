package com.examly.springapp.service;
import com.examly.springapp.entity.AuditLog;
import com.examly.springapp.repository.AuditLogRepository;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
@Service
public class AuditService {
    private final AuditLogRepository repository;
    private final UserRepository users;
    private final HttpServletRequest request;
    public AuditService(AuditLogRepository repository, UserRepository users, HttpServletRequest request) {
        this.repository = repository;
        this.users = users;
        this.request = request;
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog log(String action, String type, String id, String result, String oldValue, String newValue, String purpose) {
        AuditLog audit = new AuditLog();
        try {
            users.findByUserId(SecurityUtil.username()).ifPresent(user -> audit.setUserId(user.getId()));
        } catch (Exception ignored) {
            // Anonymous authentication events can legitimately have a null user id.
        }
        audit.setAction(action);
        audit.setEntityType(type);
        audit.setEntityId(id);
        audit.setTimestamp(Instant.now());
        try {
            audit.setIpAddress(request.getRemoteAddr());
            audit.setMetadata(request.getMethod() + " " + request.getRequestURI());
        } catch (IllegalStateException ignored) {
            audit.setIpAddress("SYSTEM");
            audit.setMetadata("SYSTEM");
        }
        audit.setPurpose(purpose);
        audit.setResult(result == null ? "SUCCESS" : result);
        audit.setPreviousValue(oldValue);
        audit.setNewValue(newValue);
        audit.setEventHash(hash(audit));
        return repository.save(audit);
    }
    private String hash(AuditLog audit) {
        String payload=String.join("|",String.valueOf(audit.getUserId()),audit.getAction(),String.valueOf(audit.getEntityType()),String.valueOf(audit.getEntityId()),String.valueOf(audit.getTimestamp()),String.valueOf(audit.getIpAddress()),audit.getResult(),String.valueOf(audit.getPurpose()),String.valueOf(audit.getNewValue()));
        try {
            String secret=System.getenv().getOrDefault("AUDIT_HMAC_SECRET","change-me-to-a-32-byte-aes-key!!");
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        }catch(Exception e) {
            throw new IllegalStateException("Unable to hash audit event",e);
        }
    }
}
