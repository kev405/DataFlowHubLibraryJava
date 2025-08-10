package com.practice.apiservice.dto.processing;

public record CreateProcessingRequest(
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(max = 400) String title, // trimmed to a 1..140
        @jakarta.validation.constraints.NotNull java.util.UUID dataFileId,
        @jakarta.validation.constraints.NotNull java.util.UUID requestedByUserId,
        java.util.UUID batchJobConfigId,
        java.util.Map<@jakarta.validation.constraints.NotBlank String,
                @jakarta.validation.constraints.NotBlank String> parameters
) {}
