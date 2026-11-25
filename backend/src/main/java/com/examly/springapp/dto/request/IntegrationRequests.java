package com.examly.springapp.dto.request;
import jakarta.validation.constraints.NotBlank;
public final class IntegrationRequests {
    private IntegrationRequests() {
    }
    public record WebhookRequest(@NotBlank String integrationCode,@NotBlank String payload,@NotBlank String signature) {
    }
    public record SyncRequest(@NotBlank String integrationCode,@NotBlank String payload,String format) {
    }
}
