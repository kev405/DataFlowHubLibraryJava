package com.practice.apiservice.batch.retry;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.MDC;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

@Component
public class RetryMetricsListener implements RetryListener {

    private final MeterRegistry registry;

    public RetryMetricsListener(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public <T, E extends Throwable> void onError(
            RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {

        String step = (String) context.getAttribute(RetryContext.NAME);
        if (step == null) step = MDC.get("step"); // opcional, si lo colocas en MDC

        registry.counter("dataflow.step.retry.attempts",
                "step", step == null ? "unknown" : step,
                "exception", throwable.getClass().getSimpleName()
        ).increment();
    }

    // opcional: puedes usar open/close si quieres inicializar o limpiar algo
    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        return true; // permitir el retry
    }

    @Override
    public <T, E extends Throwable> void close(
            RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        // no-op
    }
}
