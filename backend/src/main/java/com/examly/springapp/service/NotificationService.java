package com.examly.springapp.service;
import com.examly.springapp.entity.*;
import com.examly.springapp.enums.*;
import com.examly.springapp.integration.*;
import com.examly.springapp.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service
public class NotificationService {
    private final NotificationRepository repo;
    private final NotificationPreferenceRepository preferences;
    private final NotificationTemplateRepository templates;
    private final UserRepository users;
    private final EmailNotificationProvider email;
    private final SmsNotificationProvider sms;
    private final SimpMessagingTemplate ws;
    private final AuditService audit;
    public NotificationService(NotificationRepository repo,NotificationPreferenceRepository preferences,NotificationTemplateRepository templates,UserRepository users,EmailNotificationProvider email,SmsNotificationProvider sms,SimpMessagingTemplate ws,AuditService audit) {
        this.repo=repo;
        this.preferences=preferences;
        this.templates=templates;
        this.users=users;
        this.email=email;
        this.sms=sms;
        this.ws=ws;
        this.audit=audit;
    }
    @Transactional public Notification notify(User user,String event,String message,NotificationChannel channel) {
        boolean enabled=preferences.findAll().stream().filter(p->p.getUser().getId().equals(user.getId())&&p.getChannel()==channel).findFirst().map(NotificationPreference::isEnabled).orElse(true);
        Notification n=new Notification();
        n.setUser(user);
        n.setEventType(event);
        n.setMessage(message);
        n.setChannel(channel);
        n.setTemplateCode(event);
        n.setStatus(enabled?NotificationStatus.PENDING:NotificationStatus.FAILED);
        repo.save(n);
        if(enabled)deliver(n);
        else audit.log("NOTIFICATION_SUPPRESSED","NOTIFICATION",String.valueOf(n.getId()),"SUCCESS",null,null,"preference");
        return repo.save(n);
    }
    @Transactional public Notification deliver(Notification n) {
        try {
            switch(n.getChannel()) {
                case EMAIL->email.send(n.getUser().getEmail(),n.getEventType(),n.getMessage());
                case SMS->sms.send(n.getUser().getPhoneNumber(),n.getMessage());
                case IN_APP,PUSH->ws.convertAndSendToUser(n.getUser().getUserId(),"/queue/notifications",n.getMessage());
            }n.setStatus(NotificationStatus.SENT);
            n.setSentAt(Instant.now());
        }
        catch(Exception e) {
            n.setRetryCount(n.getRetryCount()+1);
            n.setStatus(n.getRetryCount()>=3?NotificationStatus.FAILED:NotificationStatus.RETRYING);
            n.setNextAttemptAt(Instant.now().plusSeconds(60L*n.getRetryCount()));
        }
        audit.log("NOTIFICATION_DELIVERY","NOTIFICATION",String.valueOf(n.getId()),n.getStatus().name(),null,null,"communication");
        return n;
    }
    @Transactional public void retryFailed() {
        repo.findByStatusIn(List.of(NotificationStatus.RETRYING,NotificationStatus.FAILED)).stream().filter(n->n.getRetryCount()<3).filter(n->n.getNextAttemptAt()==null||!n.getNextAttemptAt().isAfter(Instant.now())).forEach(this::deliver);
    }
    @Transactional public NotificationTemplate saveTemplate(String code,String subject,String body,boolean active) {
        NotificationTemplate t=templates.findByCode(code).orElseGet(NotificationTemplate::new);
        t.setCode(code);
        t.setSubject(subject);
        t.setBody(body);
        t.setActive(active);
        return templates.save(t);
    }
    @Transactional public void setPreference(User user,NotificationChannel channel,boolean enabled) {
        NotificationPreference p=preferences.findAll().stream().filter(x->x.getUser().getId().equals(user.getId())&&x.getChannel()==channel).findFirst().orElseGet(NotificationPreference::new);
        p.setUser(user);
        p.setChannel(channel);
        p.setEnabled(enabled);
        preferences.save(p);
    }
    @Transactional public int broadcast(String event,String message,NotificationChannel channel,Role role) {
        NotificationChannel actual=channel==null?NotificationChannel.IN_APP:channel;
        List<User> recipients=users.findAll().stream().filter(User::isActive).filter(u->role==null||u.getRole()==role).toList();
        recipients.forEach(u->notify(u,event,message,actual));
        return recipients.size();
    }
    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> userNotifications(Long id) {
        return repo.findByUserIdOrderByCreatedAtDesc(id).stream().map(n -> {
            java.util.Map<String, Object> dto = new java.util.LinkedHashMap<>();
            dto.put("id", n.getId());
            dto.put("eventType", n.getEventType());
            dto.put("message", n.getMessage());
            dto.put("channel", n.getChannel());
            dto.put("status", n.getStatus());
            dto.put("createdAt", n.getCreatedAt());
            dto.put("sentAt", n.getSentAt());
            dto.put("templateCode", n.getTemplateCode());
            return dto;
        }).toList();
    }
}
