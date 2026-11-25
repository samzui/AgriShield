package com.examly.springapp.service;
import com.examly.springapp.dto.request.ClaimRequests.*;
import com.examly.springapp.entity.*;
import com.examly.springapp.enums.*;
import com.examly.springapp.exception.*;
import com.examly.springapp.integration.DbtPaymentProvider;
import com.examly.springapp.repository.*;
import com.examly.springapp.util.SecurityUtil;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;
@Service
public class ClaimService {
    private final ClaimRepository repo;
    private final PolicyRepository policies;
    private final SurveyAssignmentRepository surveys;
    private final StatusHistoryRepository history;
    private final AuditService audit;
    private final DbtPaymentProvider dbt;
    private final NotificationService notifier;
    private final OperationalHoursService hours;
    private final UserRepository users;
    public ClaimService(ClaimRepository repo,PolicyRepository policies,SurveyAssignmentRepository surveys,StatusHistoryRepository history,AuditService audit,DbtPaymentProvider dbt,NotificationService notifier,OperationalHoursService hours,UserRepository users) {
        this.repo=repo;
        this.policies=policies;
        this.surveys=surveys;
        this.history=history;
        this.audit=audit;
        this.dbt=dbt;
        this.notifier=notifier;
        this.hours=hours;
        this.users=users;
    }
    @Transactional
    public Claim initiate(InitiateRequest r) {
        Policy policy=policies.findById(r.policyId()).orElseThrow(()->new ResourceNotFoundException("Policy not found"));
        if(SecurityUtil.has("FARMER") && !policy.getFarmer().getUserId().equals(SecurityUtil.username())) throw new com.examly.springapp.exception.UnauthorisedAccessException("Farmers can initiate only their own claim");
        SurveyAssignment survey=surveys.findById(r.surveyId()).orElseThrow(()->new ResourceNotFoundException("Survey not found"));
        if(repo.findBySurveyId(r.surveyId()).isPresent())throw new DuplicateClaimException("A claim already exists for this survey");
        if(!survey.getNotification().getPolicy().getId().equals(policy.getId()))throw new IllegalArgumentException("Survey does not belong to policy");
        if(survey.getStatus()!=SurveyStatus.SUBMITTED&&survey.getStatus()!=SurveyStatus.VERIFIED)throw new IllegalArgumentException("Claim requires a submitted or verified survey");
        Claim claim=new Claim();
        claim.setPolicy(policy);
        claim.setSurvey(survey);
        claim.setClaimedAmount(r.claimedAmount());
        claim.setStatus(ClaimStatus.INITIATED);
        Claim saved=repo.save(claim);
        policy.setStatus(PolicyStatus.CLAIM_FILED);
        policies.save(policy);
        transitionHistory(saved,null,ClaimStatus.INITIATED.name(),"claim initiation");
        audit.log("CREATE","CLAIM",String.valueOf(saved.getId()),"SUCCESS",null,saved.getClaimedAmount().toString(),"claim initiation");
        return saved;
    }
    @Transactional public Claim review(Long id) {
        Claim c=get(id);
        require(c.getStatus()==ClaimStatus.INITIATED,"Claim is not ready for review");
        setStatus(c,ClaimStatus.UNDER_REVIEW,"review started");
        return repo.save(c);
    }
    @Transactional public Claim approve(Long id,ApproveRequest r) {
        hours.assertOpen();
        Claim c=get(id);
        require(c.getStatus()==ClaimStatus.INITIATED||c.getStatus()==ClaimStatus.UNDER_REVIEW,"Claim is not reviewable");
        if(r.approvedAmount().compareTo(c.getClaimedAmount())>0)throw new IllegalArgumentException("Approved amount cannot exceed claimed amount");
        c.setApprovedAmount(r.approvedAmount());
        setStatus(c,ClaimStatus.APPROVED,r.reason()==null?"claim approval":r.reason());
        return repo.save(c);
    }
    @Transactional public Claim reject(Long id,String reason) {
        Claim c=get(id);
        require(c.getStatus()==ClaimStatus.INITIATED||c.getStatus()==ClaimStatus.UNDER_REVIEW,"Claim is not reviewable");
        setStatus(c,ClaimStatus.REJECTED,reason);
        return repo.save(c);
    }
    @Transactional public Claim disburse(Long id,DisburseRequest r) {
        hours.assertOpen();
        Claim c=get(id);
        require(c.getStatus()==ClaimStatus.APPROVED,"Only approved claims can be disbursed");
        var result=dbt.transfer(c.getPolicy().getBankAccountNo(),c.getApprovedAmount(),r.dbtUtr());
        if(!result.accepted())throw new IllegalArgumentException("DBT transfer was not accepted: "+result.message());
        c.setDbtUtr(result.utr());
        c.setDisbursedDate(LocalDate.now());
        setStatus(c,ClaimStatus.PAID,"DBT disbursement");
        c.getPolicy().setStatus(PolicyStatus.SETTLED);
        policies.save(c.getPolicy());
        return repo.save(c);
    }
    public Claim get(Long id) {
        Claim claim=repo.findById(id).orElseThrow(()->new ResourceNotFoundException("Claim not found"));
        if (!SecurityUtil.has("ADMIN") && !SecurityUtil.has("INSURANCE_OFFICER") && !SecurityUtil.has("STATE_OFFICER") && !SecurityUtil.has("BANK_OFFICER")) {
            String current=SecurityUtil.username();
            if (!claim.getPolicy().getFarmer().getUserId().equals(current)) throw new com.examly.springapp.exception.UnauthorisedAccessException("You can access only your own claim");
        }
        return claim;
    }
    @Transactional
    public Page<Claim> list(int page,int size) {
        Page<Claim> pageData=repo.findAll(PageRequest.of(Math.max(0,page),Math.min(Math.max(1,size),100),Sort.by("id").descending()));
        if(SecurityUtil.has("ADMIN")||SecurityUtil.has("INSURANCE_OFFICER")||SecurityUtil.has("STATE_OFFICER")||SecurityUtil.has("BANK_OFFICER"))return pageData;
        String current=SecurityUtil.username();
        List<Claim> own=repo.findAll().stream().filter(c->c.getPolicy().getFarmer().getUserId().equals(current)).toList();
        int safeSize=Math.min(Math.max(1,size),100);
        int from=Math.min(Math.max(0,page)*safeSize,own.size());
        int to=Math.min(from+safeSize,own.size());
        return new PageImpl<>(own.subList(from,to),PageRequest.of(Math.max(0,page),safeSize),own.size());
    }
    public List<StatusHistory> history(Long id) {
        get(id);
        return history.findByEntityTypeAndEntityIdOrderByChangedAtAsc("CLAIM",id);
    }
    private void setStatus(Claim c,ClaimStatus next,String reason) {
        String previous=c.getStatus()==null?null:c.getStatus().name();
        c.setStatus(next);
        transitionHistory(c,previous,next.name(),reason);
        audit.log("STATUS_CHANGE","CLAIM",String.valueOf(c.getId()),"SUCCESS",previous,next.name(),reason);
        notifier.notify(c.getPolicy().getFarmer(),"CLAIM_STATUS","Claim "+c.getId()+" status changed to "+next.name(),com.examly.springapp.enums.NotificationChannel.IN_APP);
    }
    private void transitionHistory(Claim c,String previous,String next,String reason) {
        StatusHistory h=new StatusHistory();
        h.setEntityType("CLAIM");
        h.setEntityId(c.getId());
        h.setPreviousStatus(previous);
        h.setNewStatus(next);
        h.setChangedAt(Instant.now());
        h.setReason(reason);
        history.save(h);
    }
    private void require(boolean condition,String message) {
        if(!condition)throw new IllegalArgumentException(message);
    }
}
