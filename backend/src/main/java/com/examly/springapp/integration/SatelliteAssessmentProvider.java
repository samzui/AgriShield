package com.examly.springapp.integration;
import java.math.BigDecimal;
public interface SatelliteAssessmentProvider {
    Assessment assess(BigDecimal latitude,BigDecimal longitude);
    record Assessment(BigDecimal ndvi,BigDecimal vegetationHealth,String source) {
    }
}
