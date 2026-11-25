package com.examly.springapp.service;
import org.springframework.stereotype.Service;
import java.util.*;
@Service public class PredictiveAnalyticsService {
    public Map<String,Object> riskScore(long claims,long losses,double avgLoss) {
        double score=Math.min(100,claims*2+losses*1.5+avgLoss*.5);
        return Map.of("riskScore",Math.round(score*100.0)/100.0,"model","baseline-rule-model","productionModelRequired",true);
    }public Map<String,Object> weeklyInsight() {
        return Map.of("summary","Weekly operational insight generated from current transactional indicators","recommendation","Prioritise high-loss and overdue workflow records","model","deterministic-baseline");
    }
}
