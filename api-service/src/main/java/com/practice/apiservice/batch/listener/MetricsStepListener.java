package com.practice.apiservice.batch.listener;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class MetricsStepListener implements StepExecutionListener {

    private static final String START_NANO = "stepStartNano";
    private final MeterRegistry registry;

    public MetricsStepListener(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        stepExecution.getExecutionContext().putLong(START_NANO, System.nanoTime());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        final String job = stepExecution.getJobExecution().getJobInstance().getJobName();
        final String step = stepExecution.getStepName();

        long start = stepExecution.getExecutionContext().getLong(START_NANO, System.nanoTime());
        long durationNs = System.nanoTime() - start;

        // Step duration
        Timer.builder("dataflow.step.duration")
                .tag("job", job)
                .tag("step", step)
                .register(registry)
                .record(Duration.ofNanos(durationNs));

        // Counters for reads/writes/skips
        registry.counter("dataflow.step.reads", "job", job, "step", step)
                .increment(stepExecution.getReadCount());
        registry.counter("dataflow.step.writes", "job", job, "step", step)
                .increment(stepExecution.getWriteCount());
        registry.counter("dataflow.step.skips", "job", job, "step", step)
                .increment(stepExecution.getReadSkipCount()
                        + stepExecution.getWriteSkipCount()
                        + stepExecution.getProcessSkipCount());

        return stepExecution.getExitStatus();
    }
}
