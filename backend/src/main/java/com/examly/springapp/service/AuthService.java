package com.examly.springapp.service;
import com.examly.springapp.dto.request.AuthRequests.*;
import com.examly.springapp.dto.response.AuthResponse;
import com.examly.springapp.entity.*;
import com.examly.springapp.enums.Role;
import com.examly.springapp.exception.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.security.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.UUID;
@Service public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtTokenProvider jwt;
    private final RefreshTokenRepository refresh;
    private final BlacklistedTokenRepository blacklist;
    private final PasswordHistoryRepository history;
    private final LoginAttemptRepository attempts;
    private final OtpService otp;
    private final AuditService audit;
    private final SessionActivityService sessions;
    public AuthService(UserRepository users,PasswordEncoder encoder,JwtTokenProvider jwt,RefreshTokenRepository refresh,BlacklistedTokenRepository blacklist,PasswordHistoryRepository history,LoginAttemptRepository attempts,OtpService otp,AuditService audit,SessionActivityService sessions) {
        this.users=users;
        this.encoder=encoder;
        this.jwt=jwt;
        this.refresh=refresh;
        this.blacklist=blacklist;
        this.history=history;
        this.attempts=attempts;
        this.otp=otp;
        this.audit=audit;
        this.sessions=sessions;
    }
    @Transactional public String register(RegisterRequest r) {
        validateName(r.name());
        validatePassword(r.password());
        if(!r.phone().matches("\\d{10}"))throw new InvalidPhoneException("Phone Number must be exactly 10 digits long");
        if(users.existsByEmailIgnoreCase(r.email()))throw new IllegalArgumentException("This email is already registered");
        if(users.existsByUserId(r.userId()))throw new IllegalArgumentException("This user ID is already registered");
        if(users.existsByPhoneNumber(r.phone()))throw new IllegalArgumentException("This phone number is already registered");
        User u=new User();
        u.setName(r.name().trim());
        u.setUserId(r.userId().trim());
        u.setEmail(r.email().trim().toLowerCase());
        u.setPhoneNumber(r.phone());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRole(r.role());
        u.setActive(true);
        u.setEmailVerified(false);
        u.setSupervisorApproved(r.role()==Role.FARMER||r.role()==Role.GUEST);
        u.setPasswordChangedAt(Instant.now());
        u.setPasswordExpiresAt(Instant.now().plusSeconds((r.role()==Role.FARMER?180L:90L)*24L*60L*60L));
        u.setLastActivityAt(Instant.now());
        users.save(u);
        PasswordHistory ph=new PasswordHistory();
        ph.setUser(u);
        ph.setPasswordHash(u.getPasswordHash());
        ph.setChangedAt(Instant.now());
        history.save(ph);
        otp.send(u.getEmail(),"EMAIL_VERIFICATION");
        audit.log("REGISTER","USER",String.valueOf(u.getId()),"SUCCESS",null,u.getEmail(),"account creation");
        return u.getUserId();
    }
    public AuthResponse login(LoginRequest r) {
        User u=users.findByEmailIgnoreCase(r.identifier()).orElseGet(()->users.findByUserId(r.identifier()).orElseGet(()->users.findByPhoneNumber(r.identifier()).orElse(null)));
        if(u==null||!encoder.matches(r.password(),u.getPasswordHash())) {
            recordAttempt(r.identifier(),false);
            throw new IllegalArgumentException("Invalid credentials. Please check your email or user ID and password.");
        }if(u.getLockedUntil()!=null&&u.getLockedUntil().isAfter(Instant.now()))throw new IllegalArgumentException("Account temporarily locked");
        if(!u.isActive())throw new IllegalArgumentException("Account is inactive");
        if(!u.isEmailVerified())throw new IllegalArgumentException("Please verify your email before login");
        if(u.getRole()!=Role.FARMER&&u.getRole()!=Role.GUEST&&!u.isSupervisorApproved())throw new IllegalArgumentException("Account is awaiting supervisor approval");
        recordAttempt(r.identifier(),true);
        u.setLastLogin(Instant.now());
        u.setLastActivityAt(Instant.now());
        users.save(u);
        sessions.touch(u.getUserId());
        String access=jwt.createAccessToken(u);
        RefreshToken rt=new RefreshToken();
        rt.setUser(u);
        rt.setToken(UUID.randomUUID()+"."+UUID.randomUUID());
        rt.setExpiresAt(Instant.now().plusSeconds(30L*24L*60L*60L));
        refresh.save(rt);
        audit.log("LOGIN","USER",String.valueOf(u.getId()),"SUCCESS",null,null,"authentication");
        long seconds=Duration.between(Instant.now(),jwt.parse(access).getExpiration().toInstant()).getSeconds();
        return new AuthResponse(access,rt.getToken(),"Bearer",seconds,u.getId(),u.getUserId(),u.getEmail(),u.getRole().name());
    }
    @Transactional public void verifyEmail(OtpRequest r) {
        otp.verify(r.target(),r.otp(),"EMAIL_VERIFICATION");
        User u=users.findByEmailIgnoreCase(r.target()).orElseThrow(()->new ResourceNotFoundException("User not found"));
        u.setEmailVerified(true);
        users.save(u);
        audit.log("EMAIL_VERIFIED","USER",String.valueOf(u.getId()),"SUCCESS",null,null,"account activation");
    }
    @Transactional public void resetPassword(PasswordResetRequest r) {
        otp.verify(r.email(),r.otp(),"PASSWORD_RESET");
        User u=users.findByEmailIgnoreCase(r.email()).orElseThrow(()->new ResourceNotFoundException("User not found"));
        validatePassword(r.newPassword());
        var recent=history.findAll().stream().filter(x->x.getUser().getId().equals(u.getId())).sorted((a,b)->b.getChangedAt().compareTo(a.getChangedAt())).limit(5).toList();
        if(recent.stream().anyMatch(x->encoder.matches(r.newPassword(),x.getPasswordHash())))throw new IllegalArgumentException("Password cannot match the last 5 passwords");
        u.setPasswordHash(encoder.encode(r.newPassword()));
        u.setPasswordChangedAt(Instant.now());
        u.setPasswordExpiresAt(Instant.now().plusSeconds((u.getRole()==Role.FARMER?180L:90L)*24L*60L*60L));
        users.save(u);
        history.save(passwordHistory(u));
        refresh.findAll().stream().filter(t->t.getUser().getId().equals(u.getId())&&!t.isRevoked()).forEach(t-> {
            t.setRevoked(true);refresh.save(t);
        });
        audit.log("PASSWORD_RESET","USER",String.valueOf(u.getId()),"SUCCESS",null,null,"password recovery");
    }
    public void requestPasswordReset(String email) {
        users.findByEmailIgnoreCase(email).orElseThrow(()->new ResourceNotFoundException("User not found"));
        otp.send(email,"PASSWORD_RESET");
    }
    public void resendOtp(String target,String purpose) {
        if(target==null||target.isBlank())throw new IllegalArgumentException("OTP target is required");
        otp.send(target,purpose==null||purpose.isBlank()?"EMAIL_VERIFICATION":purpose);
    }
    @Transactional public void logout(String token) {
        if(token==null||token.isBlank())return;
        var claims=jwt.parse(token);
        BlacklistedToken b=new BlacklistedToken();
        b.setToken(token);
        b.setExpiresAt(claims.getExpiration().toInstant());
        b.setReason("LOGOUT");
        blacklist.save(b);
        sessions.invalidate(claims.getSubject());
        audit.log("LOGOUT","SESSION",claims.getSubject(),"SUCCESS",null,null,"authentication");
    }
    @Transactional public AuthResponse refresh(String token) {
        RefreshToken rt=refresh.findByToken(token).orElseThrow(()->new IllegalArgumentException("Invalid refresh token"));
        if(rt.isRevoked()||rt.getExpiresAt().isBefore(Instant.now()))throw new IllegalArgumentException("Refresh token expired");
        rt.setRevoked(true);
        refresh.save(rt);
        User u=rt.getUser();
        String access=jwt.createAccessToken(u);
        RefreshToken next=new RefreshToken();
        next.setUser(u);
        next.setToken(UUID.randomUUID()+"."+UUID.randomUUID());
        next.setExpiresAt(Instant.now().plusSeconds(30L*24L*60L*60L));
        refresh.save(next);
        long seconds=Duration.between(Instant.now(),jwt.parse(access).getExpiration().toInstant()).getSeconds();
        return new AuthResponse(access,next.getToken(),"Bearer",seconds,u.getId(),u.getUserId(),u.getEmail(),u.getRole().name());
    }
    private PasswordHistory passwordHistory(User u) {
        PasswordHistory ph=new PasswordHistory();
        ph.setUser(u);
        ph.setPasswordHash(u.getPasswordHash());
        ph.setChangedAt(Instant.now());
        return ph;
    }
    private void validateName(String name) {
        if(name==null||!name.matches("[A-Za-z ]{2,100}"))throw new InvalidNameException("Name must not contain numbers or special characters");
    }
    private void validatePassword(String p) {
        if(p==null||!p.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$"))throw new IllegalArgumentException("Password must meet security requirements");
    }
    private void recordAttempt(String identifier,boolean success) {
        LoginAttempt a=new LoginAttempt();
        a.setUsernameOrEmail(identifier);
        a.setAttemptedAt(Instant.now());
        a.setSuccessful(success);
        attempts.save(a);
        if(!success) {
            long failures=attempts.findAll().stream().filter(x->x.getUsernameOrEmail().equalsIgnoreCase(identifier)&&!x.isSuccessful()&&x.getAttemptedAt().isAfter(Instant.now().minusSeconds(30L*60L))).count();
            users.findByEmailIgnoreCase(identifier).or(()->users.findByUserId(identifier)).or(()->users.findByPhoneNumber(identifier)).ifPresent(u-> {
                if(failures>=15)u.setLockedUntil(Instant.now().plusSeconds(1440L*60L));else if(failures>=10)u.setLockedUntil(Instant.now().plusSeconds(30L*60L));else if(failures>=5)u.setLockedUntil(Instant.now().plusSeconds(15L*60L));users.save(u);
            });
        }
    }
}
