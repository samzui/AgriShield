package com.examly.springapp.service;
import com.examly.springapp.dto.request.ScheduleRequests.*;
import com.examly.springapp.entity.Schedule;
import com.examly.springapp.entity.User;
import com.examly.springapp.enums.NotificationChannel;
import com.examly.springapp.enums.ScheduleStatus;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.repository.ScheduleRepository;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.util.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
@Service
public class SchedulingService {
    private static final DateTimeFormatter DISPLAY=DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneOffset.UTC);
    private final ScheduleRepository repo;
    private final AuditService audit;
    private final OperationalHoursService hours;
    private final UserRepository users;
    private final NotificationService notifier;
    public SchedulingService(ScheduleRepository repo,AuditService audit,OperationalHoursService hours,UserRepository users,NotificationService notifier) {
        this.repo=repo;
        this.audit=audit;
        this.hours=hours;
        this.users=users;
        this.notifier=notifier;
    }
    @Transactional public Schedule create(CreateRequest r) {
        hours.assertOpen();
        validate(r.startTime(),r.endTime());
        if(!available(r.resourceUserId(),r.startTime(),r.endTime(),null))throw new IllegalArgumentException("Resource is already assigned in this time slot");
        Schedule s=new Schedule();
        s.setScheduleType(r.scheduleType());
        s.setResourceUserId(r.resourceUserId());
        s.setStartTime(r.startTime());
        s.setEndTime(r.endTime());
        s.setRecurrenceRule(r.recurrenceRule());
        s.setReminderRule(r.reminderRule());
        s.setParticipantUserIds(r.participantUserIds());
        s.setTimezone(r.timezone());
        s.setPurpose(r.purpose());
        s.setLocation(r.location());
        s.setStatus(ScheduleStatus.CONFIRMED.name());
        Schedule saved=repo.save(s);
        audit.log("CREATE","SCHEDULE",String.valueOf(saved.getId()),"SUCCESS",null,null,"schedule");
        notifyResource(saved);
        return saved;
    }
    // Real, backend-persisted notification to the assigned resource - uses the existing NotificationService/
    // Notification entity so it appears in the recipient's own Notifications view and survives refresh/login,
    // exactly like the survey-assignment notification already does.
    private void notifyResource(Schedule s) {
        User resource=users.findById(s.getResourceUserId()).orElse(null);
        if(resource==null)return;
        String scheduledBy=users.findByUserId(SecurityUtil.username()).map(User::getName).orElse("Administration");
        StringBuilder message=new StringBuilder();
        message.append(s.getScheduleType()).append(" scheduled: ").append(DISPLAY.format(s.getStartTime())).append(" to ").append(DISPLAY.format(s.getEndTime())).append(" UTC");
        if(s.getPurpose()!=null&&!s.getPurpose().isBlank())message.append(". Purpose: ").append(s.getPurpose());
        if(s.getLocation()!=null&&!s.getLocation().isBlank())message.append(". Location: ").append(s.getLocation());
        message.append(". Scheduled by ").append(scheduledBy).append('.');
        notifier.notify(resource,"SCHEDULE_CREATED",message.toString(),NotificationChannel.IN_APP);
    }
    public boolean available(Long resource,Instant start,Instant end,Long excluded) {
        validate(start,end);
        return repo.findAll().stream().filter(s->Objects.equals(s.getResourceUserId(),resource)).filter(s->!Objects.equals(s.getId(),excluded)).filter(s->!ScheduleStatus.CANCELLED.name().equals(s.getStatus())).noneMatch(s->start.isBefore(s.getEndTime())&&end.isAfter(s.getStartTime()));
    }
    @Transactional public Schedule reschedule(Long id,RescheduleRequest r) {
        Schedule s=get(id);
        if(!available(s.getResourceUserId(),r.startTime(),r.endTime(),id))throw new IllegalArgumentException("Resource is already assigned in this time slot");
        s.setStartTime(r.startTime());
        s.setEndTime(r.endTime());
        s.setRescheduleReason(r.reason());
        s.setStatus(ScheduleStatus.RESCHEDULED.name());
        Schedule saved=repo.save(s);
        audit.log("RESCHEDULE","SCHEDULE",String.valueOf(id),"SUCCESS",null,r.reason(),"schedule");
        return saved;
    }
    @Transactional public Schedule cancel(Long id,String reason) {
        Schedule s=get(id);
        s.setCancellationReason(reason);
        s.setStatus(ScheduleStatus.CANCELLED.name());
        Schedule saved=repo.save(s);
        audit.log("CANCEL","SCHEDULE",String.valueOf(id),"SUCCESS",null,reason,"schedule");
        return saved;
    }
    public Schedule get(Long id) {
        return repo.findById(id).orElseThrow(()->new ResourceNotFoundException("Schedule not found"));
    }
    public List<Schedule> list() {
        return repo.findAll();
    }
    private void validate(Instant start,Instant end) {
        if(start==null||end==null||!start.isBefore(end))throw new IllegalArgumentException("Start time must be before end time");
    }
}
