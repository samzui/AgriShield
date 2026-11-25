package com.examly.springapp.util;
import com.examly.springapp.exception.UnauthorisedAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
public final class SecurityUtil {
    private SecurityUtil() {
    } public static String username() {
        var a=SecurityContextHolder.getContext().getAuthentication();
        if(a==null||!a.isAuthenticated())throw new UnauthorisedAccessException("Authentication required");
        return a.getName();
    } public static boolean has(String role) {
        var a=SecurityContextHolder.getContext().getAuthentication();
        return a!=null&&a.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_"+role));
    }
}
