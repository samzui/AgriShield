package com.examly.springapp.integration;
import org.springframework.stereotype.Component;
import java.util.*;
@Component public class MockExternalIntegrationClient implements ExternalIntegrationClient {
    public Map<String,Object> call(String operation,Map<String,Object> payload) {
        return Map.of("operation",operation,"status","SANDBOX","payload",payload);
    }
}
