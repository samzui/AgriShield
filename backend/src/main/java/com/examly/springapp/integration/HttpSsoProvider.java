package com.examly.springapp.integration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Map;
@Component @Profile("prod")
public class HttpSsoProvider implements SsoProvider {
    private final RestClient client;
    private final String url;
    private final String clientId;
    public HttpSsoProvider(@Value("${SSO_TOKEN_URL:}")String url,@Value("${SSO_CLIENT_ID:}")String clientId) {
        this.client=RestClient.create();
        this.url=url;
        this.clientId=clientId;
    }
    public SsoResult exchange(String assertion) {
        if(url.isBlank()||clientId.isBlank())return new SsoResult(false,null,null,null,"SSO provider is not configured");
        Map<?,?> r=client.post().uri(url).body(Map.of("clientId",clientId,"assertion",assertion)).retrieve().body(Map.class);
        return new SsoResult(Boolean.TRUE.equals(r.get("authenticated")),String.valueOf(r.get("subject")),String.valueOf(r.get("email")),String.valueOf(r.get("role")),String.valueOf(r.get("message")));
    }
}
