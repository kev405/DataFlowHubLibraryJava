package com.practice.apiservice;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.explore.JobExplorer;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import com.practice.apiservice.batch.BatchScheduler;
import com.practice.apiservice.batch.PendingProcessingProvider;
import com.practice.apiservice.service.JobRunnerService;

class BatchSchedulerTest {

    @Test
    void launches_pending_and_skips_when_running_duplicate() throws Exception {
        PendingProcessingProvider provider = Mockito.mock(PendingProcessingProvider.class);
        JobRunnerService runner = Mockito.mock(JobRunnerService.class);
        JobExplorer explorer = Mockito.mock(JobExplorer.class);
        var meters = new SimpleMeterRegistry();

        // no presión
        when(explorer.getJobNames()).thenReturn(List.of("csv_to_ipa_v1"));
        when(explorer.findRunningJobExecutions(anyString())).thenReturn(Set.of());

        // 3 pendientes
        when(provider.findPending(50)).thenReturn(List.of(
                new PendingProcessingProvider.ProcessingItem("csv_to_ipa_v1", "p1"),
                new PendingProcessingProvider.ProcessingItem("csv_to_ipa_v1", "p2"),
                new PendingProcessingProvider.ProcessingItem("csv_to_ipa_v1", "p3")
        ));

        // uno se omite (null ⇒ había running con mismos params dentro de runIfNotRunning)
        when(runner.runIfNotRunning(eq("csv_to_ipa_v1"), anyString(), isNull()))
                .thenReturn(Mockito.mock(org.springframework.batch.core.JobExecution.class))
                .thenReturn(null)
                .thenReturn(Mockito.mock(org.springframework.batch.core.JobExecution.class));

        BatchScheduler scheduler = new BatchScheduler(provider, runner, explorer, meters);
        scheduler.triggerDaily();

        verify(runner, times(3)).runIfNotRunning(eq("csv_to_ipa_v1"), anyString(), isNull());
        // métrica agregada (no asertamos valores exactos aquí para mantenerlo simple)
        meters.find("dataflow.scheduler.trigger").counters();
    }
}
