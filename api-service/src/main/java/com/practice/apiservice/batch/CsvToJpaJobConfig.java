package com.practice.apiservice.batch;

import com.practice.apiservice.batch.listener.LoggingJobExecutionListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CsvToJpaJobConfig {

    @Bean
    public Job csvToJpaJob(JobRepository jobRepository,
                           Step csvImportStep,
                           LoggingJobExecutionListener jobListener,        // C0-F3-03
                           JobParametersValidator csvToJpaJobParametersValidator, // F3-05
                           JobParametersIncrementer jobParametersIncrementer) {   // F3-06
        return new JobBuilder("csvToJpaJob", jobRepository)
                .listener(jobListener)
                .validator(csvToJpaJobParametersValidator)
                .incrementer(jobParametersIncrementer)
                // .preventRestart() NO aquí: dejamos que sea reiniciable.
                .start(csvImportStep)
                .build();
    }
}
