package com.practice.apiservice.batch;

import com.practice.apiservice.batch.listener.LoggingJobExecutionListener;
import com.practice.apiservice.batch.listener.MetricsStepListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class DemoJobConfig {

    @Bean
    public Job demoJob(JobRepository jobRepository,
                       Step demoStep,
                       LoggingJobExecutionListener jobListener) {
        return new JobBuilder("demoJob", jobRepository)
                .listener(jobListener)
                .start(demoStep)
                .build();
    }

    @Bean
    public Step demoStep(JobRepository jobRepository,
                         PlatformTransactionManager tx,
                         MetricsStepListener stepListener) {
        return new StepBuilder("demoStep", jobRepository)
                .tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, tx)
                .listener(stepListener)
                .build();
    }
}
