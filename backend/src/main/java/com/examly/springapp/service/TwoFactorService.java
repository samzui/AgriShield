package com.examly.springapp.service;
import com.examly.springapp.entity.TwoFactorVerification;
import com.examly.springapp.enums.VerificationStatus;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.repository.*;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.UUID;
@Service public class TwoFactorService {
    private final TwoFactorVerificationRepository repo;
    private final UserRepository users;
    private final OtpService otp;
    public TwoFactorService(TwoFactorVerificationRepository repo,UserRepository users,OtpService otp) {
        this.repo=repo;
        this.users=users;
        this.otp=otp;
    }
    public String createChallenge(Long userId,String operation) {
        var u=users.findById(userId).orElseThrow(()->new ResourceNotFoundException("User not found"));
        String id=UUID.randomUUID().toString();
        TwoFactorVerification c=new TwoFactorVerification();
        c.setUser(u);
        c.setOperation(operation);
        c.setChallengeId(id);
        c.setExpiresAt(Instant.now().plusSeconds(10L*60L));
        repo.save(c);
        otp.send(u.getEmail(),"2FA:"+id);
        return id;
    }
    public boolean verify(String id,String value) {
        var c=repo.findByChallengeId(id).orElseThrow(()->new IllegalArgumentException("2FA challenge not found"));
        if(c.getExpiresAt().isBefore(Instant.now()))throw new IllegalArgumentException("2FA challenge expired");
        otp.verify(c.getUser().getEmail(),value,"2FA:"+id);
        c.setStatus(VerificationStatus.VERIFIED);
        repo.save(c);
        return true;
    }
}
