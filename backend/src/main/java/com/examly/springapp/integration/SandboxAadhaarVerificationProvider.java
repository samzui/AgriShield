package com.examly.springapp.integration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
@Component @Profile( {
    "default","dev","test"
})
public class SandboxAadhaarVerificationProvider implements AadhaarVerificationProvider {
    public VerificationResult verify(String documentNumber) {
        boolean valid=documentNumber!=null&&documentNumber.matches("\\d{12}");
        return new VerificationResult(valid,"SANDBOX-"+documentNumber,valid?"Verified in sandbox":"Invalid document format");
    }
}
