package com.examly.springapp.dto.request;
import com.examly.springapp.enums.NotificationChannel;
import jakarta.validation.constraints.*;
public final class NotificationRequests {
    private NotificationRequests() {
    }
    public record TemplateRequest(@NotBlank String code,@NotBlank String subject,@NotBlank String body,boolean active) {
    }
    public record PreferenceRequest(@NotNull NotificationChannel channel,boolean enabled) {
    }
    public record BroadcastRequest(@NotBlank String eventType,@NotBlank String message,NotificationChannel channel,String role) {
    }
}
