package com.examly.springapp.dto.request;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
public final class MobileRequests {
    private MobileRequests() {
    }
    public record SyncRequest(@NotBlank String deviceId,@NotNull Instant lastSyncAt,@NotNull List<SyncItem> changes) {
    }
    public record SyncItem(@NotBlank String entityType,@NotBlank String operation,@NotBlank String clientId,String payload) {
    }
}
