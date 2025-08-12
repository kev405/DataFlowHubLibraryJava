package com.practice.apiservice.batch;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import com.practice.apiservice.service.JobRunnerService;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "scheduling.enabled", havingValue = "true")
public class BatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(BatchScheduler.class);

    private final PendingProcessingProvider pendingProvider;
    private final JobRunnerService jobRunner;
    private final JobExplorer jobExplorer;
    private final MeterRegistry meterRegistry;

    // simple back‑pressure: si hay > N running, saltamos
    private final int runningThreshold = 10;
    private final int queryLimit = 50;

    public BatchScheduler(PendingProcessingProvider pendingProvider,
                          JobRunnerService jobRunner,
                          JobExplorer jobExplorer,
                          MeterRegistry meterRegistry) {
        this.pendingProvider = pendingProvider;
        this.jobRunner = jobRunner;
        this.jobExplorer = jobExplorer;
        this.meterRegistry = meterRegistry;
    }

    /** Todos los días a las 02:00. En otras zonas, ajusta el cron o el TZ del contenedor. */
    @Scheduled(cron = "0 0 2 * * *")
    public void triggerDaily() {
        int runningNow = jobExplorer.getJobNames().stream()
                .mapToInt(n -> jobExplorer.findRunningJobExecutions(n).size())
                .sum();

        if (runningNow > runningThreshold) {
            log.info("Scheduler skipped due to back-pressure (runningNow={})", runningNow);
            meterRegistry.counter("dataflow.scheduler.trigger", "result", "skipped").increment();
            return;
        }

        List<PendingProcessingProvider.ProcessingItem> pending = pendingProvider.findPending(queryLimit);
        int launched = 0, skipped = 0;

        for (var item : pending) {
            try {
                var exec = jobRunner.runIfNotRunning(item.configId(), item.processingRequestId(), null);
                if (exec != null) launched++; else skipped++;
            } catch (Exception e) {
                log.error("Error launching job '{}' for processingRequestId={}", item.configId(), item.processingRequestId(), e);
            }
        }

        meterRegistry.counter("dataflow.scheduler.trigger", "result", "launched").increment(launched);
        meterRegistry.counter("dataflow.scheduler.trigger", "result", "skipped-running").increment(skipped);

        log.info("Scheduler launched {} jobs (pending={}, skippedRunning={})", launched, pending.size(), skipped);
    }
}
