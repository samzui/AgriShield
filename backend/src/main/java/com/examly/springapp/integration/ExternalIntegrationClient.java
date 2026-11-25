package com.examly.springapp.integration;
import java.util.Map;
public interface ExternalIntegrationClient {
    Map<String,Object> call(String operation,Map<String,Object> payload);
}
