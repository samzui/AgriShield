package com.examly.springapp.service;
import com.examly.springapp.entity.*;
import com.examly.springapp.enums.VerificationStatus;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.integration.AadhaarVerificationProvider;
import com.examly.springapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
@Service public class IdentityVerificationService {
    private final IdentityVerificationRepository repo;
    private final UserRepository users;
    private final AadhaarVerificationProvider provider;
    private final AuditService audit;
    public IdentityVerificationService(IdentityVerificationRepository repo,UserRepository users,AadhaarVerificationProvider provider,AuditService audit) {
        this.repo=repo;
        this.users=users;
        this.provider=provider;
        this.audit=audit;
    }
    @Transactional public IdentityVerification verify(Long userId,String type,String number) {
        User u=users.findById(userId).orElseThrow(()->new ResourceNotFoundException("User not found"));
        var result=provider.verify(number);
        IdentityVerification v=repo.findByUserId(userId).orElseGet(IdentityVerification::new);
        v.setUser(u);
        v.setDocumentType(type);
        v.setDocumentReferenceHash(hash(number));
        v.setStatus(result.verified()?VerificationStatus.VERIFIED:VerificationStatus.FAILED);
        v.setVerifiedAt(result.verified()?Instant.now():null);
        v.setProviderReference(result.providerReference());
        IdentityVerification saved=repo.save(v);
        audit.log("IDENTITY_VERIFY","USER",String.valueOf(userId),v.getStatus().name(),null,type,"identity verification");
        return saved;
    }
    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        }catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
