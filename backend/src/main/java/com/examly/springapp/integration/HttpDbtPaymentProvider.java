package com.examly.springapp.integration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.util.Map;
@Component @Profile("prod")
public class HttpDbtPaymentProvider implements DbtPaymentProvider {
    private final RestClient client;
    private final String url;
    private final String apiKey;
    public HttpDbtPaymentProvider(@Value("${DBT_API_URL:}")String url,@Value("${DBT_API_KEY:}")String apiKey) {
        this.client=RestClient.create();
        this.url=url;
        this.apiKey=apiKey;
    }
    public PaymentResult transfer(String accountNumber,BigDecimal amount,String reference) {
        if(url.isBlank()||apiKey.isBlank())throw new IllegalStateException("DBT provider is not configured");
        Map<?,?> r=client.post().uri(url).header("X-API-Key",apiKey).body(Map.of("accountNumber",accountNumber,"amount",amount,"reference",reference)).retrieve().body(Map.class);
        Object message=r.get("message");
        return new PaymentResult(Boolean.TRUE.equals(r.get("accepted")),String.valueOf(r.get("utr")),String.valueOf(r.get("reference")),String.valueOf(message==null?"External DBT response":message));
    }
}
