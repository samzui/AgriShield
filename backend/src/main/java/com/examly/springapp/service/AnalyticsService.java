package com.examly.springapp.service;
import com.examly.springapp.dto.response.AnalyticsDtos.*;
import com.examly.springapp.entity.*;
import com.examly.springapp.enums.*;
import com.examly.springapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class AnalyticsService {
    private final ClaimRepository claims;
    private final PolicyRepository policies;
    private final LossNotificationRepository losses;
    private final AuditLogRepository audits;
    public AnalyticsService(ClaimRepository claims,PolicyRepository policies,LossNotificationRepository losses,AuditLogRepository audits) {
        this.claims=claims;
        this.policies=policies;
        this.losses=losses;
        this.audits=audits;
    }
    @Transactional(readOnly=true)
    public Dashboard dashboard() {
        List<Policy> ps=policies.findAll();
        List<Claim> cs=claims.findAll();
        BigDecimal exposure=ps.stream().map(Policy::getSumInsured).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal claimed=cs.stream().map(Claim::getClaimedAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal approved=cs.stream().map(Claim::getApprovedAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal ratio=claimed.signum()==0?BigDecimal.ZERO:approved.multiply(BigDecimal.valueOf(100)).divide(claimed,2,RoundingMode.HALF_UP);
        return new Dashboard(ps.size(),ps.stream().filter(p->p.getStatus()==PolicyStatus.ACTIVE).count(),losses.count(),cs.size(),cs.stream().filter(c->c.getStatus()==ClaimStatus.PAID).count(),exposure,ratio,audits.count());
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> lossPatterns() {
        return losses.findAll().stream().collect(Collectors.groupingBy(x->x.getLossType().name())).entrySet().stream().map(e-> {
            BigDecimal total=e.getValue().stream().map(LossNotification::getLossPercentageEstimate).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
            long count=e.getValue().size();
            return Map.<String,Object>of("lossType",e.getKey(),"count",count,"averageLossPercentage",count==0?BigDecimal.ZERO:total.divide(BigDecimal.valueOf(count),2,RoundingMode.HALF_UP));
        }).toList();
    }
    @Transactional(readOnly=true)
    public List<TrendPoint> trends(LocalDate from,LocalDate to) {
        LocalDate start=from==null?LocalDate.now().minusDays(30):from;
        LocalDate end=to==null?LocalDate.now():to;
        if(end.isBefore(start)) throw new IllegalArgumentException("Report end date cannot be before start date");
        List<Policy> ps=policies.findAll();
        List<LossNotification> ls=losses.findAll();
        List<Claim> cs=claims.findAll();
        List<TrendPoint> out=new ArrayList<>();
        for(LocalDate d=start;!d.isAfter(end);d=d.plusDays(1)) {
            final LocalDate day=d;
            long p=ps.stream().filter(x->day.equals(x.getEnrollmentDate())).count();
            long l=ls.stream().filter(x->x.getNotificationDate()!=null&&day.equals(x.getNotificationDate().atZone(ZoneOffset.UTC).toLocalDate())).count();
            long c=cs.stream().filter(x->x.getCreatedAt()!=null&&day.equals(x.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate())).count();
            BigDecimal exposure=ps.stream().filter(x->x.getEnrollmentDate()!=null&&!x.getEnrollmentDate().isAfter(day)).map(Policy::getSumInsured).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
            out.add(new TrendPoint(day,p,l,c,exposure));
        }
        return out;
    }
    public List<Benchmark> benchmarks() {
        Dashboard d=dashboard();
        BigDecimal target=BigDecimal.valueOf(80);
        BigDecimal variance=d.claimRatio().subtract(target);
        return List.of(new Benchmark("claimRatio",d.claimRatio(),target,variance,variance.signum()>=0?"ABOVE":"BELOW"));
    }
    public List<Anomaly> anomalies() {
        List<Double> values=losses.findAll().stream().map(LossNotification::getLossPercentageEstimate).filter(Objects::nonNull).map(BigDecimal::doubleValue).toList();
        if(values.size()<2)return List.of();
        double mean=values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance=values.stream().mapToDouble(v->Math.pow(v-mean,2)).average().orElse(0);
        double std=Math.sqrt(variance);
        double latest=values.get(values.size()-1);
        double z=std==0?0:(latest-mean)/std;
        String severity=Math.abs(z)>=3?"HIGH":Math.abs(z)>=2?"MEDIUM":"LOW";
        return List.of(new Anomaly("lossPercentage",z,severity,"Z-score anomaly detection over loss assessments"));
    }
    public Forecast forecastDemand(int horizon) {
        int n=Math.max(1,Math.min(horizon,30));
        double baseline=policies.count();
        List<BigDecimal> values=new ArrayList<>();
        double slope=Math.max(1.0,baseline/20.0);
        for(int i=1;i<=n;i++)values.add(BigDecimal.valueOf(baseline+slope*i).setScale(2,RoundingMode.HALF_UP));
        return new Forecast("policyDemand",values,"linear-trend-baseline");
    }
    public Funnel funnel() {
        Map<String,Long> stages=new LinkedHashMap<>();
        stages.put("ENROLLED_POLICIES",policies.count());
        stages.put("LOSS_NOTIFICATIONS",losses.count());
        stages.put("CLAIMS",claims.count());
        stages.put("PAID_CLAIMS",claims.findAll().stream().filter(c->c.getStatus()==ClaimStatus.PAID).count());
        return new Funnel(stages);
    }
    public String weeklyInsight() {
        Dashboard d=dashboard();
        return "Weekly insight: "+d.losses()+" loss notifications, "+d.claims()+" claims and exposure of "+d.exposure()+" are currently recorded. Prioritise high-loss, unresolved and overdue operational records.";
    }
}
