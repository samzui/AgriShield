package com.examly.springapp.integration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
@Component @Profile( {
    "default","dev","test"
})
public class SandboxSsoProvider implements SsoProvider {
    public SsoResult exchange(String assertion) {
        return new SsoResult(false,null,null,null,"Corporate SSO provider must be configured for production");
    }
}
