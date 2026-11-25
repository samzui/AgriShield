package com.examly.springapp.service;
import org.springframework.stereotype.Service;
import java.util.*;
@Service public class HealthIntegrationService {
    public Map<String,String> status() {
        return Map.of("database","managed-by-spring-actuator","email","sandbox","sms","sandbox","documentStorage","local","dbt","adapter-ready","externalVerification","adapter-ready","ml","baseline-ready");
    }
}
