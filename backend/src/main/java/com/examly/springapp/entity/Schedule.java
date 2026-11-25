package com.examly.springapp.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Getter @Setter @NoArgsConstructor @Table(name="schedules") public class Schedule extends BaseEntity {
    private String scheduleType;
    private Long resourceUserId;
    private Instant startTime;
    private Instant endTime;
    private String status;
    private String recurrenceRule;
    private String cancellationReason;
    private String rescheduleReason;
    private String reminderRule;
    @Lob private String participantUserIds;
    private String timezone;
    private String purpose;
    private String location;
}
