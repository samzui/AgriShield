package com.examly.springapp.integration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.util.Map;
@Component @Profile("prod")
public class HttpSatelliteAssessmentProvider implements SatelliteAssessmentProvider {
    private final RestClient client;
    private final String url;
    private final String apiKey;
    public HttpSatelliteAssessmentProvider(@Value("${SATELLITE_API_URL:}")String url,@Value("${SATELLITE_API_KEY:}")String apiKey) {
        this.client=RestClient.create();
        this.url=url;
        this.apiKey=apiKey;
    }
    public Assessment assess(BigDecimal lat,BigDecimal lng) {
        if(url.isBlank()||apiKey.isBlank())throw new IllegalStateException("Satellite provider is not configured");
        Map<?,?> r=client.get().uri(url+"?lat="+lat+"&lng="+lng).header("X-API-Key",apiKey).retrieve().body(Map.class);
        Object ndvi=r.get("ndvi");
        Object health=r.get("vegetationHealth");
        return new Assessment(new BigDecimal(String.valueOf(ndvi)),new BigDecimal(String.valueOf(health)),"EXTERNAL-SATELLITE");
    }
}
