package com.examly.springapp.dto.response;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
public final class AnalyticsDtos {
    private AnalyticsDtos() {
    }
    public record Dashboard(long policies,long activePolicies,long losses,long claims,long paidClaims,BigDecimal exposure,BigDecimal claimRatio,long auditEvents) {
    }
    public record TrendPoint(LocalDate date,long policies,long losses,long claims,BigDecimal exposure) {
    }
    public record Benchmark(String metric,BigDecimal currentValue,BigDecimal targetValue,BigDecimal variance,String direction) {
    }
    public record Forecast(String metric,List<BigDecimal> values,String method) {
    }
    public record Anomaly(String metric,double zScore,String severity,String explanation) {
    }
    public record Funnel(Map<String,Long> stages) {
    }
}
