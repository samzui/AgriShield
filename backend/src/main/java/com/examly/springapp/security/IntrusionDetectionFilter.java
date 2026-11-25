package com.examly.springapp.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.*;
import java.util.concurrent.*;
@Component
public class IntrusionDetectionFilter extends OncePerRequestFilter {
    private final ConcurrentMap<String,Integer> failures=new ConcurrentHashMap<>();
    private final ConcurrentMap<String,Instant> blocked=new ConcurrentHashMap<>();
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException {
        String ip=req.getRemoteAddr();
        Instant until=blocked.get(ip);
        if(until!=null&&until.isAfter(Instant.now())) {
            res.setStatus(429);
            return;
        }if(until!=null)blocked.remove(ip);
        chain.doFilter(req,res);
        if(res.getStatus()==401) {
            int count=failures.merge(ip,1,Integer::sum);
            if(count>=10)blocked.put(ip,Instant.now().plusSeconds(30L*60L));
        }
    }
}
