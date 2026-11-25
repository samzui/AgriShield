package com.examly.springapp.integration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Map;
@Component @Profile("prod")
public class TwilioSmsNotificationProvider implements SmsNotificationProvider {
    private final RestClient client=RestClient.create();
    private final String accountSid;
    private final String authToken;
    private final String from;
    public TwilioSmsNotificationProvider(@Value("${TWILIO_ACCOUNT_SID:}")String accountSid,@Value("${TWILIO_AUTH_TOKEN:}")String authToken,@Value("${TWILIO_FROM:}")String from) {
        this.accountSid=accountSid;
        this.authToken=authToken;
        this.from=from;
    }
    public void send(String phone,String message) {
        if(accountSid.isBlank()||authToken.isBlank()||from.isBlank())throw new IllegalStateException("Twilio SMS credentials are not configured");
        client.post().uri("https://api.twilio.com/2010-04-01/Accounts/{sid}/Messages.json",accountSid).headers(h->h.setBasicAuth(accountSid,authToken)).contentType(MediaType.APPLICATION_FORM_URLENCODED).body("To="+phone+"&From="+from+"&Body="+message).retrieve().toBodilessEntity();
    }
}
