package com.examly.springapp.integration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Map;
@Component @Profile("prod")
public class HttpAadhaarVerificationProvider implements AadhaarVerificationProvider {
    private final RestClient client;
    private final String url;
    private final String apiKey;
    public HttpAadhaarVerificationProvider(@Value("${AADHAAR_VERIFY_URL:}")String url,@Value("${AADHAAR_API_KEY:}")String apiKey) {
        this.client=RestClient.create();
        this.url=url;
        this.apiKey=apiKey;
    }
    public VerificationResult verify(String documentNumber) {
        if(url.isBlank()||apiKey.isBlank())throw new IllegalStateException("Aadhaar verification provider is not configured");
        Map<String,Object> response=client.post().uri(url).header("X-API-Key",apiKey).body(Map.of("documentNumber",documentNumber)).retrieve().body(Map.class);
        boolean verified=Boolean.TRUE.equals(response.get("verified"));
        return new VerificationResult(verified,String.valueOf(response.getOrDefault("reference","EXTERNAL")),String.valueOf(response.getOrDefault("message","External verification completed")));
    }
}
