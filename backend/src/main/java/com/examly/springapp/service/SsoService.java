package com.examly.springapp.service;
import com.examly.springapp.integration.SsoProvider;
import org.springframework.stereotype.Service;
@Service public class SsoService {
    private final SsoProvider provider;
    public SsoService(SsoProvider provider) {
        this.provider=provider;
    }public SsoProvider.SsoResult exchange(String assertion) {
        return provider.exchange(assertion);
    }
}
