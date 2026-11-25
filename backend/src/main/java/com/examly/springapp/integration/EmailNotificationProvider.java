package com.examly.springapp.integration;
public interface EmailNotificationProvider {
    void send(String to,String subject,String body);
}
