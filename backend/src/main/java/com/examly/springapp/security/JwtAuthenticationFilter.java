package com.examly.springapp.security;
import com.examly.springapp.repository.BlacklistedTokenRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwt;
    private final BlacklistedTokenRepository blacklist;
    private final SessionActivityService sessions;
    public JwtAuthenticationFilter(JwtTokenProvider jwt,BlacklistedTokenRepository blacklist,SessionActivityService sessions) {
        this.jwt=jwt;
        this.blacklist=blacklist;
        this.sessions=sessions;
    }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException {
        String header=req.getHeader("Authorization");
        if(header!=null&&header.startsWith("Bearer ")) {
            String token=header.substring(7);
            if(!blacklist.existsByToken(token)&&jwt.valid(token)) {
                Claims claims=jwt.parse(token);
                String username=claims.getSubject();
                if(sessions.active(username)) {
                    String role=claims.get("role",String.class);
                    var auth=new UsernamePasswordAuthenticationToken(username,null,List.of(new SimpleGrantedAuthority("ROLE_"+role)));
                    auth.setDetails(claims);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    sessions.touch(username);
                }
            }
        }
        chain.doFilter(req,res);
    }
}
