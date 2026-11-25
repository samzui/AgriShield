package com.examly.springapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public final class DocumentRequests {
    private DocumentRequests() {}

    public record RollbackRequest(@Positive int versionNumber, @NotBlank String reason) {}

    public record SignatureRequest(@NotBlank String signatureReference,
                                   @NotBlank String algorithm) {}
}
