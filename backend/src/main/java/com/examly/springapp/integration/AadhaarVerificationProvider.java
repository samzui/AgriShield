package com.examly.springapp.integration;
public interface AadhaarVerificationProvider {
    VerificationResult verify(String documentNumber);
    record VerificationResult(boolean verified,String providerReference,String message) {
    }
}
