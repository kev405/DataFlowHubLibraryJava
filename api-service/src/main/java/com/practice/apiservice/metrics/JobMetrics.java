package com.practice.apiservice.metrics;


import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class JobMetrics {
    public static final String JOB_EXEC_COUNTER = "dataflow.job.executions.total";

    private final MeterRegistry meterRegistry;

    public JobMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /** result: "success" | "fail" (o cualquier etiqueta que uses) */
    public void incrementJobExecutions(String result) {
        meterRegistry.counter(JOB_EXEC_COUNTER, "result", result).increment();
    }
}
