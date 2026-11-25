package com.examly.springapp.integration;
public interface SmsNotificationProvider {
    void send(String phone,String message);
}
