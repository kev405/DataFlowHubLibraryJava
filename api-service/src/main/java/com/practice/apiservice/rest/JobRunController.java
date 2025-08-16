package com.practice.apiservice.rest;

import java.time.Instant;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jobs2")
public class JobRunController {

    private final JobLauncher jobLauncher;
    private final Job csvToJpaJob;

    public JobRunController(JobLauncher jobLauncher, Job csvToJpaJob) {
        this.jobLauncher = jobLauncher;
        this.csvToJpaJob = csvToJpaJob;
    }

    @PostMapping("/{configId}/run")
    public ResponseEntity<Map<String, Object>> runCsvToJpaJob(@PathVariable String configId,
                                            @Validated @RequestBody RunCsvRequest req) throws Exception {

        var params = new JobParametersBuilder()
                .addString("configId", configId)
                .addString("processingRequestId", req.processingRequestId())
                .addString("storagePath", req.storagePath())
                .addString("delimiter", req.delimiter() == null ? "," : req.delimiter())
                .addLong("chunkSize", req.chunkSize() == null ? 500L : req.chunkSize().longValue())
                .addString("requestTime", (req.requestTime() == null ? Instant.now() : req.requestTime()).toString())
                .toJobParameters();

        JobExecution exec = jobLauncher.run(csvToJpaJob, params);

        return ResponseEntity.ok(Map.of(
                "jobExecutionId", exec.getId(),
                "status", exec.getStatus().toString()
        ));
    }

    public record RunCsvRequest(
            String processingRequestId,
            String storagePath,
            String delimiter,
            Integer chunkSize,
            Instant requestTime
    ) {}
}
