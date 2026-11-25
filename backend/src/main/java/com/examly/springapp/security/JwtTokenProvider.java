package com.examly.springapp.security;
import com.examly.springapp.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
@Component public class JwtTokenProvider {
    private final SecretKey key;
    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret) {
        if(secret.getBytes(StandardCharsets.UTF_8).length<32) throw new IllegalArgumentException("JWT secret must be at least 256 bits");
        key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public String createAccessToken(User u) {
        long hours=switch(u.getRole()) {
            case ADMIN->24;
            case BANK_OFFICER,SURVEYOR,INSURANCE_OFFICER,STATE_OFFICER->12;
            default->8;
        };
        Instant now=Instant.now();
        return Jwts.builder().subject(u.getUserId()).claim("userId",u.getId()).claim("role",u.getRole().name()).claim("email",u.getEmail()).issuedAt(Date.from(now)).expiration(Date.from(now.plus(hours, java.time.temporal.ChronoUnit.HOURS))).signWith(key,Jwts.SIG.HS256).compact();
    }
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
    public boolean valid(String token) {
        try {
            parse(token);
            return true;
        }catch(JwtException|IllegalArgumentException e) {
            return false;
        }
    }
}
