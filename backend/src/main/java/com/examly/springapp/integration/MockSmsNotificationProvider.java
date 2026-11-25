package com.examly.springapp.integration;
import org.springframework.stereotype.Component;
@Component public class MockSmsNotificationProvider implements SmsNotificationProvider {
    public void send(String phone,String message) {
        System.out.println("[DEV SMS] "+phone+" : "+message);
    }
}
