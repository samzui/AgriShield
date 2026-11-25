package com.examly.springapp.integration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
@Component @ConditionalOnProperty(name="app.mail.mode",havingValue="console",matchIfMissing=true) public class MockEmailNotificationProvider implements EmailNotificationProvider {
    public void send(String to,String subject,String body) {
        System.out.println("\n========================================");
        System.out.println("[DEV OTP] To: "+to);
        System.out.println("[DEV OTP] Subject: "+subject);
        System.out.println("[DEV OTP] "+body);
        System.out.println("========================================\n");
    }
}
