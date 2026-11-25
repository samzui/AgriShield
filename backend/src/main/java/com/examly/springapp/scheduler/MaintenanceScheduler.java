package com.examly.springapp.scheduler;
import com.examly.springapp.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
@Component public class MaintenanceScheduler {
    private final BlacklistedTokenRepository blacklist;
    private final OtpVerificationRepository otp;
    public MaintenanceScheduler(BlacklistedTokenRepository b,OtpVerificationRepository o) {
        blacklist=b;
        otp=o;
    }@Scheduled(fixedDelay=3600000)public void cleanup() {
        var now=Instant.now();
        blacklist.deleteAll(blacklist.findAll().stream().filter(x->x.getExpiresAt().isBefore(now)).toList());
        otp.deleteAll(otp.findAll().stream().filter(x->x.getExpiresAt().isBefore(now)||x.isUsed()).toList());
    }
}
