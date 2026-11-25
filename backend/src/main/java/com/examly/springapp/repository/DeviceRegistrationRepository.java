package com.examly.springapp.repository;
import com.examly.springapp.entity.DeviceRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface DeviceRegistrationRepository extends JpaRepository<DeviceRegistration,Long> {
    Optional<DeviceRegistration> findByUserIdAndDeviceFingerprint(Long userId,String fingerprint);
}
