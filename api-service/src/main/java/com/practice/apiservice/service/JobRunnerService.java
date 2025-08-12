package com.practice.apiservice.service;

import com.practice.apiservice.metrics.JobMetrics;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
public class JobRunnerService {

    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;
    private final JobExplorer jobExplorer;
    private final JobMetrics jobMetrics;

    public JobRunnerService(JobLauncher jobLauncher, JobRegistry jobRegistry,
                            JobExplorer jobExplorer, JobMetrics jobMetrics) {
        this.jobLauncher = jobLauncher;
        this.jobRegistry = jobRegistry;
        this.jobExplorer = jobExplorer;
        this.jobMetrics = jobMetrics;
    }

    /** Lanza el job si no hay ejecución RUNNING con los mismos parámetros clave. */
    public JobExecution runIfNotRunning(String jobName, String processingRequestId, Map<String,String> extraParams) throws Exception {
        Job job = jobRegistry.getJob(jobName);

        JobParametersBuilder b = new JobParametersBuilder()
                .addString("processingRequestId", processingRequestId)
                .addString("configId", jobName)
                .addString("requestTime", Instant.now().toString());

        if (extraParams != null) extraParams.forEach(b::addString);
        JobParameters params = b.toJobParameters();

        // evitar duplicados: ¿hay RUNNING con mismos parámetros?
        boolean running = jobExplorer.findRunningJobExecutions(jobName).stream()
                .anyMatch(exec -> params.equals(exec.getJobParameters()));
        if (running) {
            jobMetrics.incrementJobExecutions("skipped-running");
            return null; // señal de omitido
        }

        JobExecution exec = jobLauncher.run(job, params);
        jobMetrics.incrementJobExecutions("launched");
        return exec;
    }
}
