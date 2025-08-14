package com.practice.apiservice.batch.listener;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LoggingJobExecutionListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingJobExecutionListener.class);
    private static final String START_NANO = "jobStartNano";

    private final MeterRegistry registry;

    public LoggingJobExecutionListener(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        jobExecution.getExecutionContext().putLong(START_NANO, System.nanoTime());

        Map<String, Object> params = jobExecution.getJobParameters()
                .getParameters()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getValue()
                ));

        Map<String, Object> evt = Map.of(
                "event", "JOB_START",
                "job", jobExecution.getJobInstance().getJobName(),
                "executionId", jobExecution.getId(),
                "params", params
        );
        log.info("{}", evt);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long start = jobExecution.getExecutionContext().getLong(START_NANO, System.nanoTime());
        long durationNs = System.nanoTime() - start;

        // Métrica: total de ejecuciones por job y status
        registry.counter("dataflow.job.executions.total",
                "job", jobExecution.getJobInstance().getJobName(),
                "status", jobExecution.getStatus().name()
        ).increment();

        // Métrica: duración del job (timer)
        Timer.builder("dataflow.job.duration")
                .description("Job duration in seconds")
                .tag("job", jobExecution.getJobInstance().getJobName())
                .register(registry)
                .record(Duration.ofNanos(durationNs));

        Map<String, Object> evt = Map.of(
                "event", "JOB_END",
                "job", jobExecution.getJobInstance().getJobName(),
                "executionId", jobExecution.getId(),
                "status", jobExecution.getStatus().name(),
                "exitCode", jobExecution.getExitStatus().getExitCode(),
                "durationMs", Duration.ofNanos(durationNs).toMillis()
        );
        log.info("{}", evt);
    }
}
