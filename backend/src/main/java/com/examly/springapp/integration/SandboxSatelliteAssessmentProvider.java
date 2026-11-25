package com.examly.springapp.integration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.math.*;
@Component @Profile( {
    "default","dev","test"
})
public class SandboxSatelliteAssessmentProvider implements SatelliteAssessmentProvider {
    public Assessment assess(BigDecimal latitude,BigDecimal longitude) {
        double seed=Math.abs(latitude.doubleValue()*.13+longitude.doubleValue()*.07);
        double ndvi=Math.min(.95,.35+(seed%.55));
        return new Assessment(BigDecimal.valueOf(ndvi).setScale(4,RoundingMode.HALF_UP),BigDecimal.valueOf(ndvi*100).setScale(2,RoundingMode.HALF_UP),"SATELLITE-SANDBOX");
    }
}
