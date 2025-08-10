package com.practice.apiservice.rest;

import com.practice.apiservice.dto.processing.ProcessingStatusResponse;
import com.practice.apiservice.repository.irepository.ProcessingStatusFinder;
import com.practice.apiservice.utils.error.ResourceNotFoundException;
import com.practice.domain.jobexecution.JobExecution;
import com.practice.domain.processing.ProcessingRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/processings")
public class ProcessingQueryController {

    private final ProcessingStatusFinder finder;

    public ProcessingQueryController(ProcessingStatusFinder finder) { this.finder = finder; }

    @GetMapping("/{id}")
    public ResponseEntity<ProcessingStatusResponse> getById(@PathVariable("id") UUID id) {

        ProcessingRequest pr = finder.findRequest(id)
                .orElseThrow(() -> new ResourceNotFoundException("processing id not found"));

        var metrics = new ProcessingStatusResponse.Metrics(0,0,0);
        ProcessingStatusResponse.LastExecution lastDto = null;

        var lastOpt = finder.findLastExecution(id);
        if (lastOpt.isPresent()) {
            JobExecution last = lastOpt.get();
            metrics = new ProcessingStatusResponse.Metrics(
                    last.readCount(), last.writeCount(), last.skipCount());
            lastDto = new ProcessingStatusResponse.LastExecution(
                    last.startTime(), last.endTime(),
                    last.exitStatus() == null ? null : last.exitStatus().name(),
                    last.errorMessage());
        }

        var dto = new ProcessingStatusResponse(
                pr.id(),
                pr.title(),
                pr.status().name(),         // enum → String
                pr.createdAt(),
                pr.dataFile().id(),
                metrics,
                lastDto
        );
        return ResponseEntity.ok(dto);
    }
}
