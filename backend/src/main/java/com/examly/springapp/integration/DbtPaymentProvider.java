package com.examly.springapp.integration;
import java.math.BigDecimal;
public interface DbtPaymentProvider {
    PaymentResult transfer(String accountNumber,BigDecimal amount,String reference);
    record PaymentResult(boolean accepted,String utr,String providerReference,String message) {
    }
}
