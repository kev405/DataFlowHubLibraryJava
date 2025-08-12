package com.practice.apiservice.rest;

import com.practice.apiservice.dto.batch.JobRunAcceptedResponse;
import com.practice.apiservice.dto.batch.RunJobRequest;
import com.practice.apiservice.metrics.JobMetrics;
import com.practice.apiservice.utils.error.BatchConfigNotFoundException;
import jakarta.validation.Valid;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;
    private final JobMetrics jobMetrics;

    public JobController(JobLauncher jobLauncher, JobRegistry jobRegistry, JobMetrics jobMetrics) {
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
        this.jobMetrics = jobMetrics;
    }

    @PostMapping("/{configId}/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> runJob(@PathVariable String configId,
                                    @Valid @RequestBody RunJobRequest req) throws Exception {

        Job job = jobRegistry.getJob(configId); // el bean del Job debe llamarse como configId
        if (job == null) throw new BatchConfigNotFoundException(configId);

        JobParametersBuilder params = new JobParametersBuilder()
                .addString("processingRequestId", req.processingRequestId())
                .addString("configId", configId)
                .addString("requestTime", Instant.now().toString());

        if (req.parameters() != null) {
            for (Map.Entry<String,String> e : req.parameters().entrySet()) {
                params.addString(e.getKey(), e.getValue());
            }
        }

        JobExecution exec = jobLauncher.run(job, params.toJobParameters());

        // métrica básica: incrementamos con result=PENDING (se puede actualizar al finalizar en un listener)
        jobMetrics.incrementJobExecutions("accepted");

        URI location = URI.create("/processings/" + req.processingRequestId());
        JobRunAcceptedResponse body = new JobRunAcceptedResponse(
                exec.getJobInstance() != null ? exec.getJobInstance().getId() : null,
                exec.getId()
        );

        return ResponseEntity.accepted().location(location).body(body);
    }
}
