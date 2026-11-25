package com.examly.springapp.security;
import com.examly.springapp.enums.Role;
import com.examly.springapp.service.PermissionService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
@Component
public class PermissionFilter extends OncePerRequestFilter {
    private final PermissionService permissions;
    public PermissionFilter(PermissionService permissions) {
        this.permissions=permissions;
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null&&auth.isAuthenticated()&&!publicPath(request.getRequestURI())) {
            String code=permissionFor(request.getRequestURI(),request.getMethod());
            if(code!=null) {
                String roleText=auth.getAuthorities().stream().map(Object::toString).filter(x->x.startsWith("ROLE_")).findFirst().orElse("ROLE_GUEST");
                try {
                    Role role=Role.valueOf(roleText.substring(5));
                    if(!permissions.has(role,code)) {
                        response.setStatus(403);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"success\":false,\"message\":\"Permission denied\"}");
                        return;
                    }
                }catch(IllegalArgumentException e) {
                    response.setStatus(403);
                    return;
                }
            }
        }
        chain.doFilter(request,response);
    }
    private boolean publicPath(String p) {
        return p.startsWith("/api/auth/")||p.startsWith("/swagger")||p.startsWith("/v3/api-docs")||p.equals("/actuator/health")||p.equals("/error");
    }
    private String permissionFor(String p,String m) {
        if(p.startsWith("/api/admin")||p.startsWith("/api/permissions")||p.startsWith("/api/compliance"))return "ADMIN_WRITE";
        if(p.startsWith("/api/audit-logs"))return "AUDIT_READ";
        if(p.startsWith("/api/reports")||p.startsWith("/api/analytics"))return "REPORT_READ";
        if(p.startsWith("/api/documents"))return m.equals("GET")?"DOCUMENT_READ":"DOCUMENT_WRITE";
        if(p.startsWith("/api/schedules"))return "SCHEDULE_WRITE";
        if(p.startsWith("/api/claims"))return m.equals("GET")?"CLAIM_READ":"CLAIM_WRITE";
        if(p.startsWith("/api/loss-notifications"))return m.equals("GET")?"LOSS_READ":"LOSS_WRITE";
        if(p.startsWith("/api/policies"))return m.equals("GET")?"POLICY_READ":"POLICY_WRITE";
        return null;
    }
}
