package com.examly.springapp.service;
import com.examly.springapp.entity.OtpVerification;
import com.examly.springapp.integration.EmailNotificationProvider;
import com.examly.springapp.repository.OtpVerificationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
@Service
public class OtpService {
    private final OtpVerificationRepository repo;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final EmailNotificationProvider email;
    public OtpService(OtpVerificationRepository repo,PasswordEncoder encoder,AuditService audit,EmailNotificationProvider email) {
        this.repo=repo;
        this.encoder=encoder;
        this.audit=audit;
        this.email=email;
    }
    public void send(String target,String purpose) {
        String otp=String.format("%06d",ThreadLocalRandom.current().nextInt(0,1_000_000));
        OtpVerification o=new OtpVerification();
        o.setTarget(target);
        o.setPurpose(purpose);
        o.setOtpHash(encoder.encode(otp));
        o.setExpiresAt(Instant.now().plusSeconds(900));
        o.setUsed(false);
        o.setAttempts(0);
        repo.save(o);
        email.send(target,"Crop Insurance verification code","Your one-time verification code is "+otp+". It expires in 15 minutes.");
        audit.log("OTP_SENT","OTP",target,"SUCCESS",null,purpose,"one-time verification");
    }
    public void verify(String target,String value,String purpose) {
        OtpVerification o=repo.findAll().stream().filter(x->x.getTarget().equalsIgnoreCase(target)&&x.getPurpose().equals(purpose)&&!x.isUsed()).max((a,b)->a.getCreatedAt().compareTo(b.getCreatedAt())).orElseThrow(()->new IllegalArgumentException("OTP not found or expired"));
        if(o.getExpiresAt().isBefore(Instant.now())||o.getAttempts()>=5)throw new IllegalArgumentException("OTP expired or too many attempts");
        o.setAttempts(o.getAttempts()+1);
        if(!encoder.matches(value,o.getOtpHash())) {
            repo.save(o);
            throw new IllegalArgumentException("Invalid OTP");
        }o.setUsed(true);
        repo.save(o);
        audit.log("OTP_VERIFIED","OTP",target,"SUCCESS",null,purpose,"one-time verification");
    }
}
