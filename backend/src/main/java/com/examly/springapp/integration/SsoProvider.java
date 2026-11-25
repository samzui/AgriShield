package com.examly.springapp.integration;
public interface SsoProvider {
    SsoResult exchange(String assertion);
    record SsoResult(boolean authenticated,String subject,String email,String role,String message) {
    }
}
