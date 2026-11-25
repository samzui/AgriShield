package com.examly.springapp.dto.response;
import java.math.BigDecimal;
public final class DashboardDtos {
    private DashboardDtos() {
    } public record ClaimRatio(BigDecimal claimed,BigDecimal approved,BigDecimal ratio,long totalClaims) {
    } public record LossPattern(String lossType,long count,BigDecimal averageLossPercentage) {
    } public record Kpi(long policies,long activePolicies,long lossNotifications,long claims,long paidClaims,BigDecimal totalExposure) {
    }
}
