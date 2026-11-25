package com.examly.springapp.dto.request;
import jakarta.validation.constraints.*;
import java.time.Instant;
public final class ScheduleRequests {
    private ScheduleRequests() {
    }
    public record CreateRequest(@NotBlank String scheduleType,@NotNull Long resourceUserId,@NotNull Instant startTime,@NotNull Instant endTime,String recurrenceRule,String reminderRule,String participantUserIds,String timezone,String purpose,String location) {
    }
    public record RescheduleRequest(@NotNull Instant startTime,@NotNull Instant endTime,@NotBlank String reason) {
    }
    public record CancelRequest(@NotBlank String reason) {
    }
}
