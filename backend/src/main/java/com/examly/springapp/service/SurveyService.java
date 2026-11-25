package com.examly.springapp.service;
import com.examly.springapp.dto.request.SurveyRequests.*;
import com.examly.springapp.entity.*;
import com.examly.springapp.enums.*;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import com.examly.springapp.util.SecurityUtil;
@Service public class SurveyService {
    private final SurveyAssignmentRepository repo;
    private final LossNotificationRepository notifications;
    private final UserRepository users;
    private final AuditService audit;
    private final NotificationService notifier;
    public SurveyService(SurveyAssignmentRepository repo,LossNotificationRepository notifications,UserRepository users,AuditService audit,NotificationService notifier) {
        this.repo=repo;
        this.notifications=notifications;
        this.users=users;
        this.audit=audit;
        this.notifier=notifier;
    }
    @Transactional public SurveyAssignment assign(AssignRequest r) {
        LossNotification n=notifications.findById(r.notificationId()).orElseThrow(()->new ResourceNotFoundException("Loss notification not found"));
        User s=users.findById(r.surveyorId()).orElseThrow(()->new ResourceNotFoundException("Surveyor not found"));
        if(s.getRole()!=Role.SURVEYOR)throw new IllegalArgumentException("Selected user is not a surveyor");
        if(repo.findByNotificationId(r.notificationId()).isPresent())throw new IllegalArgumentException("Survey already assigned");
        SurveyAssignment x=new SurveyAssignment();
        x.setNotification(n);
        x.setSurveyor(s);
        x.setAssignedDate(LocalDate.now());
        x.setSurveyDate(r.surveyDate());
        x.setStatus(SurveyStatus.ASSIGNED);
        SurveyAssignment saved=repo.save(x);
        n.setStatus(LossNotificationStatus.SURVEYOR_ASSIGNED);
        notifications.save(n);
        audit.log("ASSIGN","SURVEY",String.valueOf(saved.getId()),"SUCCESS",null,null,"survey assignment");
        notifier.notify(s,"SURVEY_ASSIGNED","Survey assignment "+saved.getId()+" is ready",NotificationChannel.IN_APP);
        return saved;
    }
    @Transactional public SurveyAssignment submit(Long id,SubmitRequest r) {
        SurveyAssignment x=repo.findById(id).orElseThrow(()->new ResourceNotFoundException("Survey assignment not found"));
        if(SecurityUtil.has("SURVEYOR") && !x.getSurveyor().getUserId().equals(SecurityUtil.username())) throw new com.examly.springapp.exception.UnauthorisedAccessException("Surveyors can submit only their own assignments");
        if(x.getStatus()!=SurveyStatus.ASSIGNED&&x.getStatus()!=SurveyStatus.IN_PROGRESS)throw new IllegalArgumentException("Survey cannot be submitted from current status");
        x.setYieldAssessedKgHa(r.yieldAssessedKgHa());
        x.setLossAssessedPercentage(r.lossAssessedPercentage());
        x.setSurveyPhotos(r.surveyPhotos()==null?new ArrayList<>():r.surveyPhotos());
        x.setStatus(SurveyStatus.SUBMITTED);
        SurveyAssignment saved=repo.save(x);
        LossNotification n=x.getNotification();
        n.setStatus(LossNotificationStatus.SURVEYED);
        notifications.save(n);
        audit.log("SUBMIT","SURVEY",String.valueOf(id),"SUCCESS",null,null,"field survey submission");
        notifier.notify(n.getPolicy().getFarmer(),"SURVEY_SUBMITTED","Field assessment for your loss report #"+n.getId()+" has been submitted. You may now initiate a claim.",NotificationChannel.IN_APP);
        return saved;
    }
    @Transactional public SurveyAssignment start(Long id) {
        SurveyAssignment x=repo.findByIdWithDetails(id).orElseThrow(()->new ResourceNotFoundException("Survey assignment not found"));
        if(SecurityUtil.has("SURVEYOR") && !x.getSurveyor().getUserId().equals(SecurityUtil.username())) throw new com.examly.springapp.exception.UnauthorisedAccessException("Surveyors can start only their own assignments");
        if(x.getStatus()!=SurveyStatus.ASSIGNED)throw new IllegalArgumentException("Only assigned surveys can be started");
        x.setStatus(SurveyStatus.IN_PROGRESS);
        return repo.save(x);
    }
}
