package com.examly.springapp.aspect;
import com.examly.springapp.service.AuditService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
@Aspect
@Component
public class AuditAspect {
    private final AuditService auditService;
    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }
    @AfterReturning("within(com.examly.springapp.controller..*)")
    public void auditControllerAccess(JoinPoint joinPoint) {
        String controller = joinPoint.getTarget().getClass().getSimpleName();
        String method = joinPoint.getSignature().getName();
        if (!"profile".equals(method) && !"AuditController".equals(controller)) {
            auditService.log("ACCESS", controller, method, "SUCCESS", null, null, "API access");
        }
    }
}
