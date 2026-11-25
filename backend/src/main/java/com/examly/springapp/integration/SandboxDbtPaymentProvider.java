package com.examly.springapp.integration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.UUID;
@Component @Profile( {
    "default","dev","test"
})
public class SandboxDbtPaymentProvider implements DbtPaymentProvider {
    public PaymentResult transfer(String accountNumber,BigDecimal amount,String reference) {
        if(accountNumber==null||accountNumber.isBlank()||amount==null||amount.signum()<=0)return new PaymentResult(false,null,null,"Invalid payment request");
        return new PaymentResult(true,"SANDBOX-"+UUID.randomUUID(),"SANDBOX-DBT","Accepted by sandbox provider");
    }
}
