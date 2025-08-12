package com.practice.apiservice.dto.batch;

public record JobRunAcceptedResponse(
        Long jobInstanceId,
        Long jobExecutionId
) {}
