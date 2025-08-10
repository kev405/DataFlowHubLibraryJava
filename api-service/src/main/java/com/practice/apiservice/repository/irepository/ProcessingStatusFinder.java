package com.practice.apiservice.repository.irepository;

import com.practice.domain.jobexecution.JobExecution;
import com.practice.domain.processing.ProcessingRequest;

import java.util.Optional;
import java.util.UUID;

public interface ProcessingStatusFinder {
    Optional<ProcessingRequest> findRequest(UUID id);
    Optional<JobExecution> findLastExecution(UUID processingId);
}
