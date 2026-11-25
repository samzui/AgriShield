package com.examly.springapp.service;
import com.examly.springapp.dto.request.AuthRequests.RegisterRequest;
import com.examly.springapp.enums.Role;
import com.examly.springapp.repository.*;
import com.examly.springapp.security.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertThrows;
class AuthServiceTest {
    @Test
    void passwordPolicyRejectsWeakPassword() {
        var service = new AuthService(
        Mockito.mock(UserRepository.class), new BCryptPasswordEncoder(), Mockito.mock(JwtTokenProvider.class),
        Mockito.mock(RefreshTokenRepository.class), Mockito.mock(BlacklistedTokenRepository.class), Mockito.mock(PasswordHistoryRepository.class),
        Mockito.mock(LoginAttemptRepository.class), Mockito.mock(OtpService.class), Mockito.mock(AuditService.class), Mockito.mock(SessionActivityService.class)
        );
        var request = new RegisterRequest("Valid Name", "validuser", "valid@example.com", "9876543210", "weak", Role.FARMER);
        assertThrows(IllegalArgumentException.class, () -> service.register(request));
    }
}
