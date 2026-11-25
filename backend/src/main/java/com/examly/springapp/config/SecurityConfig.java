package com.examly.springapp.config;
import com.examly.springapp.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import java.util.*;
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
    @Bean public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origins}")String origins) {
        CorsConfiguration c=new CorsConfiguration();
        c.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setExposedHeaders(List.of("Content-Disposition","X-Request-Id"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**",c);
        return source;
    }
    @Bean public SecurityFilterChain filterChain(HttpSecurity http,JwtAuthenticationFilter jwt,PermissionFilter permission,RateLimitFilter rateLimit,IntrusionDetectionFilter intrusion)throws Exception {
        http.csrf(csrf->csrf.disable()).cors(cors-> {
        }).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(h->h.frameOptions(f->f.deny()).contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; frame-ancestors 'none'; object-src 'none'; base-uri 'self'" )).httpStrictTransportSecurity(t->t.includeSubDomains(true).maxAgeInSeconds(31536000)))
        .authorizeHttpRequests(a->a.requestMatchers(HttpMethod.OPTIONS,"/**").permitAll().requestMatchers("/api/auth/**","/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**","/actuator/health","/error").permitAll().anyRequest().authenticated())
        .addFilterBefore(intrusion,UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(rateLimit,UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwt,UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(permission,JwtAuthenticationFilter.class);
        return http.build();
    }
}
