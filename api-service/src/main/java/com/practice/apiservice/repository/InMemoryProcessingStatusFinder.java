package com.practice.apiservice.repository;

import com.practice.apiservice.repository.irepository.ProcessingStatusFinder;
import com.practice.domain.jobexecution.JobExecution;
import com.practice.domain.processing.ProcessingRequest;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryProcessingStatusFinder implements ProcessingStatusFinder {

    private final Map<UUID, ProcessingRequest> requests = new ConcurrentHashMap<>();
    private final Map<UUID, JobExecution> lastExecByRequest = new ConcurrentHashMap<>();

    @Override public Optional<ProcessingRequest> findRequest(UUID id) {
        return Optional.ofNullable(requests.get(id));
    }
    @Override public Optional<JobExecution> findLastExecution(UUID processingId) {
        return Optional.ofNullable(lastExecByRequest.get(processingId));
    }

    // utilidades para seed/demos (opcionales)
    public void upsert(ProcessingRequest pr) { requests.put(pr.id(), pr); }
    public void putLastExecution(JobExecution je) { lastExecByRequest.put(je.processingRequest().id(), je); }
}
