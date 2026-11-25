package com.examly.springapp.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private record Bucket(long count,Instant resetAt) {
    }
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Map<String,Bucket> local=new ConcurrentHashMap<>();
    public RateLimitFilter(StringRedisTemplate redis,ObjectMapper objectMapper) {
        this.redis=redis;
        this.objectMapper=objectMapper;
    }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException {
        String path=req.getRequestURI();
        String type=bucketType(path,req.getMethod());
        int limit=limit(path,req.getMethod());
        Duration window=window(path);
        String key="rl:"+req.getRemoteAddr()+":"+type;
        HttpServletRequest request=req;
        if(path.equals("/api/auth/login")) {
            byte[] body=req.getInputStream().readAllBytes();
            String identifier=loginIdentifier(body);
            if(identifier!=null)key+=":"+identifier;
            request=new ReplayableRequest(req,body);
        }
        if(limit>0&&!allow(key,limit,window)) {
            res.setStatus(429);
            res.setHeader("Retry-After",String.valueOf(window.toSeconds()));
            return;
        }chain.doFilter(request,res);
    }
    private String loginIdentifier(byte[] body) {
        if(body.length==0)return null;
        try {
            JsonNode identifier=objectMapper.readTree(new String(body,StandardCharsets.UTF_8)).get("identifier");
            return identifier==null||identifier.asText().isBlank()?null:identifier.asText().trim().toLowerCase();
        }catch(Exception ignored) {
            return null;
        }
    }
    private static final class ReplayableRequest extends HttpServletRequestWrapper {
        private final byte[] body;
        private ReplayableRequest(HttpServletRequest request,byte[] body) {
            super(request);
            this.body=body;
        }
        @Override public ServletInputStream getInputStream() {
            ByteArrayInputStream input=new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int read() { return input.read(); }
                @Override public boolean isFinished() { return input.available()==0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener listener) { }
            };
        }
    }
    private boolean allow(String key,int limit,Duration window) {
        try {
            Long count=redis.opsForValue().increment(key);
            if(count!=null&&count==1)redis.expire(key,window);
            return count==null||count<=limit;
        }catch(Exception ignored) {
            Instant now=Instant.now();
            Bucket b=local.get(key);
            if(b==null||b.resetAt().isBefore(now))b=new Bucket(0,now.plus(window));
            if(b.count()>=limit)return false;
            local.put(key,new Bucket(b.count()+1,b.resetAt()));
            return true;
        }
    }
    private String bucketType(String p,String m) {
        if(p.equals("/api/auth/login"))return "login";
        if(p.equals("/api/auth/register"))return "register";
        if(p.contains("password-reset"))return "reset";
        return m.equals("GET")?"read":"write";
    }
    private int limit(String p,String m) {
        if(p.equals("/api/auth/login"))return 5;
        // Registration abuse-prevention: was 3 requests per IP per DAY, which permanently locks out a shared/NAT'd
        // IP (or a developer testing the form) for 24h after only 3 attempts. 20/hour still meaningfully throttles
        // automated sign-up spam (480/day cap) without blocking legitimate retries for a full day.
        if(p.equals("/api/auth/register"))return 20;
        if(p.contains("password-reset"))return 3;
        return m.equals("GET")?1000:100;
    }
    private Duration window(String p) {
        if(p.equals("/api/auth/register"))return Duration.ofHours(1);
        if(p.contains("password-reset"))return Duration.ofHours(1);
        if(p.equals("/api/auth/login"))return Duration.ofMinutes(15);
        return Duration.ofHours(1);
    }
}
