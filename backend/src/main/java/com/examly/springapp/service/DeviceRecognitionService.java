package com.examly.springapp.service;
import com.examly.springapp.entity.*;
import com.examly.springapp.repository.DeviceRegistrationRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
@Service public class DeviceRecognitionService {
    private final DeviceRegistrationRepository repo;
    public DeviceRecognitionService(DeviceRegistrationRepository repo) {
        this.repo=repo;
    }
    public boolean isTrusted(User u,String fingerprint) {
        return repo.findByUserIdAndDeviceFingerprint(u.getId(),fingerprint).map(DeviceRegistration::isTrusted).orElse(false);
    }
    public DeviceRegistration register(User u,String fingerprint,String name,String ip,boolean trusted) {
        DeviceRegistration d=repo.findByUserIdAndDeviceFingerprint(u.getId(),fingerprint).orElseGet(DeviceRegistration::new);
        d.setUser(u);
        d.setDeviceFingerprint(fingerprint);
        d.setDeviceName(name);
        d.setIpAddress(ip);
        d.setTrusted(trusted);
        d.setLastSeenAt(Instant.now());
        return repo.save(d);
    }
}
