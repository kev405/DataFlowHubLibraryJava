package com.practice.apiservice.dto.processing;

import java.time.Instant;
import java.util.UUID;

public record ProcessingStatusResponse(
        UUID id,
        String title,
        String status,
        Instant createdAt,
        UUID dataFileId,
        Metrics metrics,
        LastExecution lastExecution
) {
    public record Metrics(long readCount, long writeCount, long skipCount) {}
    public record LastExecution(Instant startTime, Instant endTime, String exitStatus, String errorMessage) {}
}
