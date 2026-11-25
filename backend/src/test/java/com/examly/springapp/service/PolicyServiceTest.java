package com.examly.springapp.service;
import com.examly.springapp.repository.PolicyRepository;
import com.examly.springapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertNotNull;
class PolicyServiceTest {
    @Test
    void serviceCanBeConstructedForUnitTesting() {
        var service = new PolicyService(Mockito.mock(PolicyRepository.class), Mockito.mock(UserRepository.class), Mockito.mock(com.examly.springapp.repository.CropRepository.class), Mockito.mock(AuditService.class), Mockito.mock(OperationalHoursService.class));
        assertNotNull(service);
    }
}
