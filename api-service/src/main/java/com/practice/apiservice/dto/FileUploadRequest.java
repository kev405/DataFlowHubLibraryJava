package com.practice.apiservice.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record FileUploadRequest(
        @NotBlank @Size(max = 255) String originalFilename,
        @Positive long sizeBytes,
        @Pattern(regexp = "^[a-fA-F0-9]{64}$") String checksumSha256, // opcional (null permitido)
        @NotBlank String storagePath,
        @NotNull UUID uploadedByUserId
) {}
