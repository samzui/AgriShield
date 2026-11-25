package com.examly.springapp.service;
import com.examly.springapp.dto.request.LossRequests.CreateRequest;
import com.examly.springapp.entity.*;
import com.examly.springapp.enums.*;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.examly.springapp.util.SecurityUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
@Service
public class LossNotificationService {
    private final LossNotificationRepository repo;
    private final PolicyRepository policies;
    private final AuditService audit;
    private final NotificationService notifications;
    private final OperationalHoursService hours;
    public LossNotificationService(LossNotificationRepository repo,PolicyRepository policies,AuditService audit,NotificationService notifications,OperationalHoursService hours) {
        this.repo=repo;
        this.policies=policies;
        this.audit=audit;
        this.notifications=notifications;
        this.hours=hours;
    }
    @Transactional public LossNotification create(CreateRequest r) {
        hours.assertOpen();
        Policy p=policies.findById(r.policyId()).orElseThrow(()->new ResourceNotFoundException("Policy not found"));
        if(SecurityUtil.has("FARMER") && !p.getFarmer().getUserId().equals(SecurityUtil.username())) throw new com.examly.springapp.exception.UnauthorisedAccessException("Farmers can report loss only for their own policy");
        if(r.affectedAreaHa().compareTo(p.getSownAreaHa())>0)throw new IllegalArgumentException("Affected area cannot exceed insured area");
        LossNotification n=new LossNotification();
        n.setPolicy(p);
        n.setLossType(r.lossType());
        n.setAffectedAreaHa(r.affectedAreaHa());
        n.setLossPercentageEstimate(r.lossPercentageEstimate());
        n.setGeoLat(r.geoLat());
        n.setGeoLng(r.geoLng());
        n.setPhotoUrls(r.photoUrls()==null?new ArrayList<>():r.photoUrls());
        n.setNotificationDate(Instant.now());
        n.setStatus(LossNotificationStatus.SUBMITTED);
        LossNotification saved=repo.save(n);
        audit.log("CREATE","LOSS_NOTIFICATION",String.valueOf(saved.getId()),"SUCCESS",null,null,"loss notification");
        notifications.notify(p.getFarmer(),"LOSS_SUBMITTED","Loss notification "+saved.getId()+" submitted",NotificationChannel.IN_APP);
        return saved;
    }
    @Transactional
    public List<LossNotification> listFor(User user) {
        boolean canViewAll = user.getRole() == Role.ADMIN
                || user.getRole() == Role.INSURANCE_OFFICER
                || user.getRole() == Role.STATE_OFFICER
                || user.getRole() == Role.BANK_OFFICER;
        List<LossNotification> records = repo.findAll();
        if (canViewAll) return records;
        return records.stream().filter(loss -> loss.getPolicy().getFarmer().getId().equals(user.getId())).toList();
    }
    @Transactional
    public List<Map<String, Object>> listViewsFor(User user) {
        boolean canViewAll = user.getRole() == Role.ADMIN
                || user.getRole() == Role.INSURANCE_OFFICER
                || user.getRole() == Role.STATE_OFFICER
                || user.getRole() == Role.BANK_OFFICER;
        List<Object[]> rows = canViewAll ? repo.findAllListViews() : repo.findListViewByFarmerId(user.getId());
        return rows.stream().map(this::projectionView).toList();
    }
    private Map<String, Object> projectionView(Object[] row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", row[0]);
        result.put("lossType", row[1]);
        result.put("affectedAreaHa", row[2]);
        result.put("lossPercentageEstimate", row[3]);
        result.put("notificationDate", row[4]);
        result.put("geoLat", row[5]);
        result.put("geoLng", row[6]);
        result.put("status", row[7]);
        result.put("policyId", row[8]);
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("id", row[8]);
        policy.put("policyNumber", row[9]);
        policy.put("cropName", row[10]);
        result.put("policy", policy);
        return result;
    }
    private Map<String, Object> view(LossNotification loss) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", loss.getId());
        result.put("lossType", loss.getLossType());
        result.put("affectedAreaHa", loss.getAffectedAreaHa());
        result.put("lossPercentageEstimate", loss.getLossPercentageEstimate());
        result.put("notificationDate", loss.getNotificationDate());
        result.put("geoLat", loss.getGeoLat());
        result.put("geoLng", loss.getGeoLng());
        result.put("status", loss.getStatus());
        if (loss.getPolicy() != null) {
            Map<String, Object> policy = new LinkedHashMap<>();
            policy.put("id", loss.getPolicy().getId());
            policy.put("policyNumber", loss.getPolicy().getPolicyNumber());
            policy.put("cropName", loss.getPolicy().getCropName());
            result.put("policy", policy);
            result.put("policyId", loss.getPolicy().getId());
        }
        return result;
    }
    public LossNotification get(Long id) {
        LossNotification n=repo.findById(id).orElseThrow(()->new ResourceNotFoundException("Loss notification not found"));
        if(SecurityUtil.has("FARMER") && !n.getPolicy().getFarmer().getUserId().equals(SecurityUtil.username())) throw new com.examly.springapp.exception.UnauthorisedAccessException("You can access only your own loss reports");
        return n;
    }
}
