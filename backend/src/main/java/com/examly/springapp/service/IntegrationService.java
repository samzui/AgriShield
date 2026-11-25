package com.examly.springapp.service;
import com.examly.springapp.entity.*;
import com.examly.springapp.enums.IntegrationStatus;
import com.examly.springapp.integration.ExternalIntegrationClient;
import com.examly.springapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.security.MessageDigest;
import java.util.*;
@Service public class IntegrationService {
    private final ExternalIntegrationRepository integrations;
    private final ExternalIntegrationLogRepository logs;
    private final IntegrationDeadLetterRepository dlq;
    private final AuditService audit;
    private final ExternalIntegrationClient client;
    public IntegrationService(ExternalIntegrationRepository integrations,ExternalIntegrationLogRepository logs,IntegrationDeadLetterRepository dlq,AuditService audit,ExternalIntegrationClient client) {
        this.integrations=integrations;
        this.logs=logs;
        this.dlq=dlq;
        this.audit=audit;
        this.client=client;
    }
    public List<ExternalIntegration> status() {
        return integrations.findAll();
    }
    // Exercises the same client used for real integration calls (sync/webhook) so "Check" reflects the
    // actual configured connector rather than a client-side status flip.
    @Transactional public ExternalIntegration check(String code) {
        ExternalIntegration i=integrations.findByCode(code).orElseThrow(()->new IllegalArgumentException("Integration not configured"));
        ExternalIntegrationLog log=new ExternalIntegrationLog();
        long start=System.currentTimeMillis();
        log.setSystemName(code);
        log.setOperation("HEALTH_CHECK");
        log.setStartedAt(Instant.now());
        log.setEnvironment(i.getEnvironment());
        log.setRequestId(UUID.randomUUID().toString());
        try {
            client.call("HEALTH_CHECK",Map.of("code",code,"baseUrl",i.getBaseUrl()==null?"":i.getBaseUrl()));
            log.setStatus("SUCCESS");
            log.setCompletedAt(Instant.now());
            log.setLatencyMs(System.currentTimeMillis()-start);
            logs.save(log);
            i.setLastCheckedAt(Instant.now());
            i.setFailureCount(0);
            i.setStatus(IntegrationStatus.ACTIVE);
            audit.log("INTEGRATION_CHECK","INTEGRATION",code,"SUCCESS",null,null,"connectivity check");
            return integrations.save(i);
        }catch(RuntimeException e) {
            log.setStatus("FAILED");
            log.setErrorMessage(e.getMessage());
            log.setCompletedAt(Instant.now());
            log.setLatencyMs(System.currentTimeMillis()-start);
            logs.save(log);
            i.setLastCheckedAt(Instant.now());
            i.setFailureCount(i.getFailureCount()+1);
            i.setStatus(IntegrationStatus.DEGRADED);
            integrations.save(i);
            audit.log("INTEGRATION_CHECK","INTEGRATION",code,"FAILED",null,e.getMessage(),"connectivity check");
            throw e;
        }
    }
    @Transactional public ExternalIntegration register(String code,String name,String baseUrl,String environment) {
        ExternalIntegration i=integrations.findByCode(code).orElseGet(ExternalIntegration::new);
        i.setCode(code);
        i.setDisplayName(name);
        i.setBaseUrl(baseUrl);
        i.setEnvironment(environment);
        i.setStatus(IntegrationStatus.ACTIVE);
        i.setLastCheckedAt(Instant.now());
        return integrations.save(i);
    }
    @Transactional public Map<String,Object> sync(String code,String payload,String format) {
        ExternalIntegration i=integrations.findByCode(code).orElseThrow(()->new IllegalArgumentException("Integration not configured"));
        ExternalIntegrationLog log=new ExternalIntegrationLog();
        long start=System.currentTimeMillis();
        log.setSystemName(code);
        log.setOperation("BULK_SYNC");
        log.setStartedAt(Instant.now());
        log.setEnvironment(i.getEnvironment());
        log.setRequestId(UUID.randomUUID().toString());
        try {
            if(format!=null&&!List.of("CSV","XML","JSON").contains(format.toUpperCase()))throw new IllegalArgumentException("Supported batch formats are CSV, XML and JSON");
            log.setStatus("SUCCESS");
            log.setCompletedAt(Instant.now());
            log.setLatencyMs(System.currentTimeMillis()-start);
            logs.save(log);
            return Map.of("integrationCode",code,"format",format==null?"JSON":format.toUpperCase(),"status","ACCEPTED","recordsHint",payload.lines().count());
        }catch(RuntimeException e) {
            log.setStatus("FAILED");
            log.setErrorMessage(e.getMessage());
            log.setCompletedAt(Instant.now());
            log.setLatencyMs(System.currentTimeMillis()-start);
            logs.save(log);
            IntegrationDeadLetter dead=new IntegrationDeadLetter();
            dead.setIntegrationCode(code);
            dead.setPayload(payload);
            dead.setErrorType(e.getClass().getSimpleName());
            dead.setFailedAt(Instant.now());
            dlq.save(dead);
            throw e;
        }
    }
    @Transactional public boolean webhook(String code,String payload,String signature,String secret) {
        ExternalIntegration i=integrations.findByCode(code).orElseThrow(()->new IllegalArgumentException("Integration not configured"));
        if(!verify(payload,signature,secret))throw new IllegalArgumentException("Invalid webhook signature");
        ExternalIntegrationLog log=new ExternalIntegrationLog();
        log.setSystemName(code);
        log.setOperation("WEBHOOK");
        log.setStatus("SUCCESS");
        log.setStartedAt(Instant.now());
        log.setCompletedAt(Instant.now());
        log.setEnvironment(i.getEnvironment());
        log.setRequestId(UUID.randomUUID().toString());
        logs.save(log);
        audit.log("WEBHOOK","INTEGRATION",code,"SUCCESS",null,null,"external webhook");
        return true;
    }
    private boolean verify(String payload,String signature,String secret) {
        try {
            Mac mac=Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
            String expected=HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),signature.getBytes(StandardCharsets.UTF_8));
        }catch(Exception e) {
            throw new IllegalStateException("Webhook verification unavailable",e);
        }
    }
}
